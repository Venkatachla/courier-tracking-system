package com.courier.shipment.dto;

import com.courier.shipment.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ShipmentResponse(Long id, String trackingNumber, Long senderId, String receiverName,
                               String receiverAddress, String receiverPhone, BigDecimal weight,
                               ShipmentStatus status, Long assignedCourierId, Instant createdAt,
                               Instant updatedAt, Long version) {}
