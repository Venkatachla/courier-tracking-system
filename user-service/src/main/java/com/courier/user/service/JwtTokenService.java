package com.courier.user.service;

import com.courier.user.config.JwtProperties;
import com.courier.user.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        if (properties.expirationMs() < 1) throw new IllegalArgumentException("JWT expiration must be positive");
        this.properties = properties;
    }

    public String issue(User user, Instant expiresAt) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("userId", user.getId())
                .claim("email", user.getEmail()).claim("role", user.getRole().name())
                .issuedAt(Date.from(now)).expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    public Instant expiryFromNow() { return Instant.now().plusMillis(properties.expirationMs()); }
}
