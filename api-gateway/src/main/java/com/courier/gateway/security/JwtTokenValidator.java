package com.courier.gateway.security;

import com.courier.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenValidator {
    private final SecretKey signingKey;

    public JwtTokenValidator(JwtProperties properties) {
        byte[] secret = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public AuthenticatedUser validate(String token) {
        Jws<Claims> parsed = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
        if (!Jwts.SIG.HS256.getId().equals(parsed.getHeader().getAlgorithm()))
            throw new IllegalArgumentException("Only HS256 access tokens are accepted");
        Claims claims = parsed.getPayload();
        String userId = String.valueOf(claims.get("userId"));
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        long parsedId;
        try { parsedId = Long.parseLong(userId); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("JWT userId claim is invalid", exception); }
        if (claims.getExpiration() == null || claims.getIssuedAt() == null
                || parsedId <= 0 || !String.valueOf(parsedId).equals(claims.getSubject())
                || email == null || email.isBlank() || role == null
                || !java.util.Set.of("ADMIN", "CUSTOMER", "COURIER").contains(role))
            throw new IllegalArgumentException("JWT identity claims are incomplete or invalid");
        return new AuthenticatedUser(parsedId, email, role);
    }
}
