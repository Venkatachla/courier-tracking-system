package com.courier.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.courier.user.dto.RegisterRequest;
import com.courier.user.dto.UserResponse;
import com.courier.user.error.ApiException;
import com.courier.user.mapper.UserMapper;
import com.courier.user.model.Role;
import com.courier.user.model.User;
import com.courier.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock UserMapper mapper;
    @Mock JwtTokenService jwt;
    @InjectMocks UserService service;

    @Test
    void registrationAlwaysCreatesCustomerAndHashesPassword() {
        RegisterRequest request = new RegisterRequest("Pat", "PAT@example.com", "secret123", "123");
        User saved = new User("Pat", "pat@example.com", "encoded", Role.CUSTOMER, "123");
        UserResponse response = new UserResponse(7L, "Pat", "pat@example.com", Role.CUSTOMER, "123", null);
        when(users.existsByEmailIgnoreCase("pat@example.com")).thenReturn(false);
        when(encoder.encode("secret123")).thenReturn("encoded");
        when(users.save(any(User.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        assertEquals(Role.CUSTOMER, service.register(request).role());
        verify(encoder).encode("secret123");
        verify(users).save(argThat(user -> user.getRole() == Role.CUSTOMER && "encoded".equals(user.getPasswordHash())));
    }

    @Test
    void duplicateEmailIsRejected() {
        when(users.existsByEmailIgnoreCase("pat@example.com")).thenReturn(true);
        assertThrows(ApiException.class, () -> service.register(new RegisterRequest("Pat", "PAT@example.com", "secret123", null)));
        verify(users, never()).save(any());
    }
}
