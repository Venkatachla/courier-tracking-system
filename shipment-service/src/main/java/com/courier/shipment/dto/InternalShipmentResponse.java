package com.courier.shipment.dto;

import com.courier.shipment.model.ShipmentStatus;

// Minimal internal lookup projection; not intended for public shipment responses.
public record InternalShipmentResponse(Long id, String trackingNumber, ShipmentStatus status,
                                        Long senderId, Long assignedCourierId) {}
