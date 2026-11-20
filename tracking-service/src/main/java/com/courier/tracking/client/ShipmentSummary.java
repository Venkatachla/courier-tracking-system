package com.courier.tracking.client;

import com.courier.tracking.model.ShipmentStatus;

public record ShipmentSummary(Long id, String trackingNumber, ShipmentStatus status,
                              Long senderId, Long assignedCourierId) {}
