package com.courier.user.dto;

import com.courier.user.model.Role;
import java.time.Instant;

public record AuthResponse(String token, String tokenType, Instant expiresAt, Long userId, String email, Role role) {}
