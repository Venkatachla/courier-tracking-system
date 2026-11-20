package com.courier.tracking.security;

import com.courier.tracking.error.ApiException;
import org.springframework.http.HttpStatus;

public record Caller(Long userId, String role) {
    public static Caller fromHeaders(String id, String role) {
        try {
            long parsedId = Long.parseLong(id);
            if (parsedId <= 0 || role == null || role.isBlank()) throw new NumberFormatException();
            return new Caller(parsedId, role.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Valid gateway user identity headers are required");
        }
    }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
