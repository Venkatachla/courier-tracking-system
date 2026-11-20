package com.courier.shipment.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import feign.FeignException;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return id -> {
            if (cause instanceof FeignException.NotFound) throw new UserNotFoundException(id, cause);
            throw new UserServiceUnavailableException("User service is temporarily unavailable; the request was not completed", cause);
        };
    }
}
