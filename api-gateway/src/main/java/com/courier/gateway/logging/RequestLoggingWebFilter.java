package com.courier.gateway.logging;

import com.courier.gateway.security.GatewaySecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startedAt = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            Object requestId = exchange.getAttribute(GatewaySecurityFilter.REQUEST_ID_ATTRIBUTE);
            Object userId = exchange.getAttribute(GatewaySecurityFilter.USER_ID_ATTRIBUTE);
            HttpStatus status = HttpStatus.resolve(exchange.getResponse().getStatusCode() == null ? 0 : exchange.getResponse().getStatusCode().value());
            log.info("method={} path={} status={} durationMs={} requestId={} userId={}",
                    exchange.getRequest().getMethod(), exchange.getRequest().getPath().value(), status == null ? "UNKNOWN" : Integer.toString(status.value()),
                    durationMs, requestId == null ? "-" : requestId, userId == null ? "-" : userId);
        });
    }
}
