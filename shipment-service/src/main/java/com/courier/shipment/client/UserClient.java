package com.courier.shipment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url:http://user-service:5001}", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @GetMapping("/internal/users/{id}")
    UserProfile getUser(@PathVariable("id") Long id);
}
