package com.courier.user.dto;

import com.courier.user.model.Role;
import java.time.Instant;

public record UserResponse(Long id, String name, String email, Role role, String phone, Instant createdAt) {}
