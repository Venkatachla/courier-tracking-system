package com.courier.user.controller;

import com.courier.user.dto.AuthResponse;
import com.courier.user.dto.LoginRequest;
import com.courier.user.dto.RegisterRequest;
import com.courier.user.dto.UserResponse;
import com.courier.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService users;
    public AuthController(UserService users) { this.users = users; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) { return users.register(request); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return users.login(request); }
}
