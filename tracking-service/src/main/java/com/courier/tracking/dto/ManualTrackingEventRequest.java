package com.courier.tracking.dto;

import com.courier.tracking.model.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualTrackingEventRequest(
        @NotBlank @Size(max = 20) String trackingNumber,
        @NotNull ShipmentStatus status,
        @Size(max = 200) String location,
        @Size(max = 1000) String description) {}
