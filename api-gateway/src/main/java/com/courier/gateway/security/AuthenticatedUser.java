package com.courier.gateway.security;

public record AuthenticatedUser(Long userId, String email, String role) {}
