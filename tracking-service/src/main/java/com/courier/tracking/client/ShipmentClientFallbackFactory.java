package com.courier.tracking.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ShipmentClientFallbackFactory implements FallbackFactory<ShipmentClient> {
    @Override
    public ShipmentClient create(Throwable cause) {
        return trackingNumber -> {
            if (cause instanceof FeignException.NotFound) throw new ShipmentNotFoundException(trackingNumber, cause);
            throw new ShipmentServiceUnavailableException("Shipment service is temporarily unavailable; the request was not completed", cause);
        };
    }
}
