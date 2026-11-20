package com.courier.tracking.dto;

import com.courier.tracking.model.ShipmentStatus;
import java.time.Instant;

public record TrackingHistoryResponse(Long id, String eventId, String trackingNumber, ShipmentStatus status,
                                      String location, String description, Long updatedBy,
                                      Instant timestamp, Instant createdAt) {}
