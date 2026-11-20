package com.courier.shipment.controller;

import com.courier.shipment.dto.InternalShipmentResponse;
import com.courier.shipment.service.ShipmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalShipmentController {
    private final ShipmentService shipments;
    public InternalShipmentController(ShipmentService shipments) { this.shipments = shipments; }

    @GetMapping("/internal/shipments/by-tracking/{trackingNumber}")
    public InternalShipmentResponse byTrackingNumber(@PathVariable String trackingNumber) {
        return shipments.getInternalByTrackingNumber(trackingNumber);
    }
}
