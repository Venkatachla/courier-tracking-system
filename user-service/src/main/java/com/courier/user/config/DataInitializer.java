package com.courier.user.config;

import com.courier.user.model.Role;
import com.courier.user.model.User;
import com.courier.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() != 0) return;
            users.save(new User("Courier Admin", "admin@courier.com", encoder.encode("Admin@123"), Role.ADMIN, null));
            users.save(new User("Courier Staff", "courier@courier.com", encoder.encode("Courier@123"), Role.COURIER, null));
            users.save(new User("Sample Customer", "customer@courier.com", encoder.encode("Customer@123"), Role.CUSTOMER, null));
        };
    }
}
