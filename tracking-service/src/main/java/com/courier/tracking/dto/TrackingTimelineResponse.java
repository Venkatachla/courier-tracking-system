package com.courier.tracking.dto;

import com.courier.tracking.model.ShipmentStatus;
import java.util.List;

public record TrackingTimelineResponse(String trackingNumber, ShipmentStatus currentStatus,
                                       List<TimelineEntryResponse> events) {}
