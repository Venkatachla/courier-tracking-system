package com.courier.tracking.dto;

import com.courier.tracking.model.ShipmentStatus;
import java.time.Instant;

public record TimelineEntryResponse(ShipmentStatus status, String location, String description, Instant timestamp) {}
