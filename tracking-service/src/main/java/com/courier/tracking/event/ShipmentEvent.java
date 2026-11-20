package com.courier.tracking.event;

import com.courier.tracking.model.ShipmentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

// Copied from Shipment Service; preserve every contract field name exactly.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentEvent(UUID eventId, String eventType, Long shipmentId, String trackingNumber,
                            ShipmentStatus status, String location, String description,
                            Long updatedBy, Instant occurredAt) {}
