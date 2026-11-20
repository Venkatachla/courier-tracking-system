package com.courier.tracking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "shipment-service", url = "${shipment.service.url:http://shipment-service:5002}", fallbackFactory = ShipmentClientFallbackFactory.class)
public interface ShipmentClient {
    @GetMapping("/internal/shipments/by-tracking/{trackingNumber}")
    ShipmentSummary getByTrackingNumber(@PathVariable("trackingNumber") String trackingNumber);
}
