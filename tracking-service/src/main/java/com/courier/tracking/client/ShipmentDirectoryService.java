package com.courier.tracking.client;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class ShipmentDirectoryService {
    private final ShipmentClient client;
    public ShipmentDirectoryService(ShipmentClient client) { this.client = client; }

    @Retry(name = "shipmentService")
    public ShipmentSummary getByTrackingNumber(String trackingNumber) {
        return client.getByTrackingNumber(trackingNumber);
    }
}
