package com.courier.user.controller;

import com.courier.user.dto.UserResponse;
import com.courier.user.error.ApiException;
import com.courier.user.service.UserService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }

    @GetMapping("/api/users/me")
    public UserResponse me(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return users.getById(parseId(userId));
    }

    @GetMapping("/api/users")
    public List<UserResponse> all(@RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ADMIN role is required");
        return users.getAll();
    }

    @GetMapping("/internal/users/{id}")
    public UserResponse internalById(@PathVariable Long id) { return users.getById(id); }

    private Long parseId(String id) {
        try { return Long.valueOf(id); }
        catch (Exception exception) { throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "A valid gateway user identity is required"); }
    }
}
