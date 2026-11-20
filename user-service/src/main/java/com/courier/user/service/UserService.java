package com.courier.user.service;

import com.courier.user.dto.AuthResponse;
import com.courier.user.dto.LoginRequest;
import com.courier.user.dto.RegisterRequest;
import com.courier.user.dto.UserResponse;
import com.courier.user.error.ApiException;
import com.courier.user.mapper.UserMapper;
import com.courier.user.model.Role;
import com.courier.user.model.User;
import com.courier.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final JwtTokenService jwt;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, UserMapper mapper, JwtTokenService jwt) {
        this.users = users; this.passwordEncoder = passwordEncoder; this.mapper = mapper; this.jwt = jwt;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "An account with this email already exists");
        User created = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.CUSTOMER, request.phone()));
        return mapper.toResponse(created);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect"));
        Instant expiresAt = jwt.expiryFromNow();
        return new AuthResponse(jwt.issue(user, expiresAt), "Bearer", expiresAt, user.getId(), user.getEmail(), user.getRole());
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return users.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() { return users.findAll().stream().map(mapper::toResponse).toList(); }
}
