package com.courier.shipment.client;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class UserDirectoryService {
    private final UserClient client;

    public UserDirectoryService(UserClient client) {
        this.client = client;
    }

    @Retry(name = "userService")
    public UserProfile getUser(Long id) {
        return client.getUser(id);
    }
}