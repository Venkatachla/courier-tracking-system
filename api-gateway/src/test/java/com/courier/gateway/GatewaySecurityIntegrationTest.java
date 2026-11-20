package com.courier.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTest {
    private static final String SECRET = "gateway-test-signing-secret-at-least-32-bytes";
    private static final MockWebServer DOWNSTREAM = startMockServer();

    @Autowired WebTestClient client;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> SECRET);
        registry.add("USER_SERVICE_URL", () -> DOWNSTREAM.url("/").toString());
        registry.add("SHIPMENT_SERVICE_URL", () -> DOWNSTREAM.url("/").toString());
        registry.add("TRACKING_SERVICE_URL", () -> DOWNSTREAM.url("/").toString());
        registry.add("RATE_LIMIT_AUTH", () -> "1");
        registry.add("RATE_LIMIT_GENERAL", () -> "1000");
        registry.add("RATE_LIMIT_WINDOW_SECONDS", () -> "60");
    }

    @AfterAll
    static void closeMockServer() throws IOException { DOWNSTREAM.shutdown(); }

    @Test
    void requestWithoutTokenGetsStandard401() {
        client.get().uri("/api/users/me").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void expiredTokenGets401() {
        client.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("CUSTOMER", Instant.now().minusSeconds(30), SECRET))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void wrongSignatureGets401() {
        client.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("CUSTOMER", Instant.now().plusSeconds(60), "different-wrong-signing-secret-at-least-32-bytes"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void forgedRoleIsReplacedByVerifiedClaimAndRequestIdIsReturned() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}").addHeader("Content-Type", "application/json"));
        String requestId = "5ca99617-424c-469d-bd1e-2acccfc72001";

        var response = client.get().uri("/api/shipments/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("COURIER", Instant.now().plusSeconds(300), SECRET))
                .header("X-Request-Id", requestId)
                .header("X-User-Id", "999")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Email", "forged@example.com")
                .exchange().expectStatus().isOk()
                .expectHeader().exists("X-Request-Id")
                .returnResult(String.class);

        RecordedRequest forwarded = DOWNSTREAM.takeRequest();
        assertEquals("7", forwarded.getHeader("X-User-Id"));
        assertEquals("COURIER", forwarded.getHeader("X-User-Role"));
        assertEquals("person@example.com", forwarded.getHeader("X-User-Email"));
        assertEquals(requestId, forwarded.getHeader("X-Request-Id"));
        assertEquals(requestId, response.getResponseHeaders().getFirst("X-Request-Id"));
    }

    @Test
    void customerCannotCallAdminOnlyAssignRoute() {
        int requestsBefore = DOWNSTREAM.getRequestCount();
        client.put().uri("/api/shipments/15/assign")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("CUSTOMER", Instant.now().plusSeconds(300), SECRET))
                .exchange().expectStatus().isForbidden()
                .expectBody().jsonPath("$.error").isEqualTo("FORBIDDEN");
        assertEquals(requestsBefore, DOWNSTREAM.getRequestCount());
    }

    @Test
    void internalPathIsNotForwarded() {
        int requestsBefore = DOWNSTREAM.getRequestCount();
        client.get().uri("/internal/shipments/by-tracking/x")
                .exchange().expectStatus().isNotFound();
        assertEquals(requestsBefore, DOWNSTREAM.getRequestCount());
    }

    @Test
    void publicTrackingLookupPassesWithoutToken() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("{\"currentStatus\":\"CREATED\"}").addHeader("Content-Type", "application/json"));

        client.get().uri("/api/track/x").exchange().expectStatus().isOk();

        RecordedRequest forwarded = DOWNSTREAM.takeRequest();
        assertEquals("/api/track/x", forwarded.getPath());
        assertNull(forwarded.getHeader("X-User-Role"));
    }

    @Test
    void corsPreflightNeedsNoToken() {
        client.options().uri("/api/shipments")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type")
                .exchange().expectStatus().isNoContent()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }

    @Test
    void authEndpointRateLimitReturns429AndRetryAfter() {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("{}").addHeader("Content-Type", "application/json"));
        String body = "{\"email\":\"x@example.com\",\"password\":\"secret\"}";

        client.post().uri("/api/auth/login").header("Content-Type", "application/json").bodyValue(body).exchange().expectStatus().isOk();
        client.post().uri("/api/auth/login").header("Content-Type", "application/json").bodyValue(body).exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().exists("Retry-After")
                .expectBody().jsonPath("$.error").isEqualTo("RATE_LIMITED");
    }

    @Test
    void downstreamDisconnectReturnsGatewayJson() {
        DOWNSTREAM.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        client.get().uri("/api/shipments/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN", Instant.now().plusSeconds(300), SECRET))
                .exchange().expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody().jsonPath("$.status").isEqualTo(502)
                .jsonPath("$.error").exists();
    }

    private static String token(String role, Instant expiresAt, String secret) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(120) : now.minusSeconds(1);
        return Jwts.builder().subject("7").claim("userId", 7L).claim("email", "person@example.com")
                .claim("role", role).issuedAt(Date.from(issuedAt)).expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    private static MockWebServer startMockServer() {
        MockWebServer server = new MockWebServer();
        try { server.start(); return server; }
        catch (IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
}
