package com.courier.shipment.dto;

import com.courier.shipment.model.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotNull ShipmentStatus status,
        @Size(max = 200) String location,
        @Size(max = 1000) String description) {}
