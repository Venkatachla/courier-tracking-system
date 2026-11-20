package com.courier.shipment.event;

import com.courier.shipment.model.ShipmentStatus;
import java.time.Instant;
import java.util.UUID;

// Event contract copied by the Tracking Service; keep the JSON field names stable.
public record ShipmentEvent(UUID eventId, String eventType, Long shipmentId, String trackingNumber,
                            ShipmentStatus status, String location, String description,
                            Long updatedBy, Instant occurredAt) {}
