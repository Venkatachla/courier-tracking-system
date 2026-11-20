package com.courier.gateway.security;

import com.courier.gateway.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatewaySecurityFilter implements WebFilter {
    public static final String REQUEST_ID_ATTRIBUTE = GatewaySecurityFilter.class.getName() + ".requestId";
    public static final String USER_ID_ATTRIBUTE = GatewaySecurityFilter.class.getName() + ".userId";
    private static final String[] IDENTITY_HEADERS = {"X-User-Id", "X-User-Role", "X-User-Email"};
    private final JwtTokenValidator tokens;
    private final AuthorizationRules rules;
    private final InMemoryRateLimiter rateLimiter;
    private final ObjectMapper json;
    private final Set<String> allowedOrigins;

    public GatewaySecurityFilter(JwtTokenValidator tokens, AuthorizationRules rules,
                                 InMemoryRateLimiter rateLimiter, ObjectMapper json,
                                 @Value("${gateway.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String origins) {
        this.tokens = tokens; this.rules = rules; this.rateLimiter = rateLimiter; this.json = json;
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(StringUtils::hasText).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange original, WebFilterChain chain) {
        String requestId = requestId(original.getRequest().getHeaders().getFirst("X-Request-Id"));
        original.getAttributes().put(REQUEST_ID_ATTRIBUTE, requestId);
        original.getResponse().getHeaders().set("X-Request-Id", requestId);
        original.getResponse().beforeCommit(() -> {
            original.getResponse().getHeaders().set("X-Request-Id", requestId);
            return Mono.empty();
        });
        applyCors(original);

        String path = original.getRequest().getPath().value();
        HttpMethod method = original.getRequest().getMethod();
        if (isBlocked(path)) return writeError(original, HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested path is not available", 0);
        if (HttpMethod.OPTIONS.equals(method)) {
            original.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
            return original.getResponse().setComplete();
        }
        if (HttpMethod.GET.equals(method) && "/actuator/health".equals(path)) return chain.filter(stripIdentity(original, requestId, null));

        String clientIp = clientIp(original);
        long retryAfter = rateLimiter.consume(clientIp, isAuthEndpoint(path, method));
        if (retryAfter > 0) return writeError(original, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Request rate limit exceeded", retryAfter);

        boolean publicRoute = isPublicRoute(path, method);
        String authorization = original.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        AuthenticatedUser user = null;
        if (StringUtils.hasText(authorization)) {
            if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7))
                return writeError(original, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid Bearer token is required", 0);
            try { user = tokens.validate(authorization.substring(7).trim()); }
            catch (JwtException | IllegalArgumentException exception) {
                return writeError(original, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "The access token is invalid or expired", 0);
            }
        } else if (!publicRoute) {
            return writeError(original, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid Bearer token is required", 0);
        }

        if (!publicRoute && user != null && !rules.allowedRoles(method, path).contains(user.role()))
            return writeError(original, HttpStatus.FORBIDDEN, "FORBIDDEN", "The authenticated role is not allowed for this operation", 0);

        ServerWebExchange secured = stripIdentity(original, requestId, user);
        if (user != null) original.getAttributes().put(USER_ID_ATTRIBUTE, user.userId().toString());
        return chain.filter(secured)
                .onErrorResume(exception -> writeDownstreamError(original, exception))
                .then(Mono.defer(() -> writeEmptyGatewayError(original)));
    }

    private ServerWebExchange stripIdentity(ServerWebExchange exchange, String requestId, AuthenticatedUser user) {
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            for (String header : IDENTITY_HEADERS) headers.remove(header);
            headers.set("X-Request-Id", requestId);
            if (user != null) {
                headers.set("X-User-Id", user.userId().toString());
                headers.set("X-User-Role", user.role());
                headers.set("X-User-Email", user.email());
            }
        }).build();
        return exchange.mutate().request(request).build();
    }

    private void applyCors(ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (origin == null || !allowedOrigins.contains(origin)) return;
        var headers = exchange.getResponse().getHeaders();
        headers.setAccessControlAllowOrigin(origin);
        headers.setAccessControlAllowMethods(java.util.List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.OPTIONS));
        headers.setAccessControlAllowHeaders(java.util.List.of("Authorization", "Content-Type", "X-Request-Id", "Accept"));
        headers.setAccessControlExposeHeaders(java.util.List.of("X-Request-Id", "Retry-After"));
        headers.setAccessControlMaxAge(3600L);
        headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    }

    private boolean isPublicRoute(String path, HttpMethod method) {
        if (HttpMethod.POST.equals(method) && ("/api/auth/register".equals(path) || "/api/auth/login".equals(path))) return true;
        return HttpMethod.GET.equals(method) && path.matches("/api/track/[^/]+");
    }

    private boolean isAuthEndpoint(String path, HttpMethod method) {
        return HttpMethod.POST.equals(method) && ("/api/auth/register".equals(path) || "/api/auth/login".equals(path));
    }

    private boolean isBlocked(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return path.equals("/internal") || path.startsWith("/internal/")
                || (path.startsWith("/actuator") && !path.equals("/actuator/health"))
                || lower.contains("/swagger-ui") || lower.contains("/v3/api-docs")
                || lower.contains("/api-docs") || lower.contains("/webjars/");
    }

    private String requestId(String incoming) {
        if (incoming != null) {
            try {
                UUID id = UUID.fromString(incoming);
                if (id.toString().equalsIgnoreCase(incoming)) return incoming;
            } catch (IllegalArgumentException ignored) { /* Generate an ID for malformed client input. */ }
        }
        return UUID.randomUUID().toString();
    }

    private String clientIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null || exchange.getRequest().getRemoteAddress().getAddress() == null) return "unknown";
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private Mono<Void> writeDownstreamError(ServerWebExchange exchange, Throwable exception) {
        ResponseStatusException responseStatus = findResponseStatus(exception);
        if (responseStatus != null && responseStatus.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.resolve(responseStatus.getStatusCode().value());
            return writeError(exchange, status == null ? HttpStatus.NOT_FOUND : status,
                    status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : "GATEWAY_REQUEST_ERROR",
                    status == HttpStatus.NOT_FOUND ? "No gateway route matches this request" : "The gateway could not handle this request", 0);
        }
        boolean timeout = hasCause(exception, TimeoutException.class) || exception.getClass().getSimpleName().toLowerCase().contains("timeout");
        boolean unavailable = hasCause(exception, ConnectException.class) || hasCause(exception, UnknownHostException.class)
                || exception.getClass().getName().contains("WebClientRequestException");
        HttpStatus status = timeout ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        String code = timeout ? "DOWNSTREAM_TIMEOUT" : unavailable ? "DOWNSTREAM_UNAVAILABLE" : "GATEWAY_ERROR";
        String message = timeout ? "A downstream service did not respond in time" : "A downstream service is unavailable";
        return writeError(exchange, status, code, message, 0);
    }

    private ResponseStatusException findResponseStatus(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause())
            if (current instanceof ResponseStatusException responseStatus) return responseStatus;
        return null;
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) if (type.isInstance(current)) return true;
        return false;
    }

    private Mono<Void> writeEmptyGatewayError(ServerWebExchange exchange) {
        if (exchange.getResponse().isCommitted()) return Mono.empty();
        HttpStatus status = HttpStatus.resolve(exchange.getResponse().getStatusCode() == null ? 0 : exchange.getResponse().getStatusCode().value());
        if (status == HttpStatus.NOT_FOUND) return writeError(exchange, status, "NOT_FOUND", "No gateway route matches this request", 0);
        if (status == HttpStatus.BAD_GATEWAY || status == HttpStatus.SERVICE_UNAVAILABLE)
            return writeError(exchange, status, "DOWNSTREAM_UNAVAILABLE", "A downstream service is unavailable", 0);
        return Mono.empty();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message, long retryAfter) {
        var response = exchange.getResponse();
        if (response.isCommitted()) return Mono.empty();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (retryAfter > 0) response.getHeaders().set("Retry-After", Long.toString(retryAfter));
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), code, message,
                exchange.getRequest().getPath().value(), java.util.Map.of());
        byte[] bytes;
        try { bytes = json.writeValueAsBytes(body); }
        catch (Exception ignored) { bytes = ("{\"status\":" + status.value() + ",\"error\":\"" + code + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8); }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
