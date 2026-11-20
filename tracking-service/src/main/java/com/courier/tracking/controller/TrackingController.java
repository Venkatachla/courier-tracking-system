package com.courier.tracking.controller;

import com.courier.tracking.dto.ManualTrackingEventRequest;
import com.courier.tracking.dto.TrackingHistoryResponse;
import com.courier.tracking.dto.TrackingTimelineResponse;
import com.courier.tracking.security.Caller;
import com.courier.tracking.service.TrackingEventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class TrackingController {
    private final TrackingEventService events;
    public TrackingController(TrackingEventService events) { this.events = events; }

    @GetMapping("/api/track/{trackingNumber}")
    public TrackingTimelineResponse publicTimeline(@PathVariable String trackingNumber) {
        return events.publicTimeline(trackingNumber);
    }

    @GetMapping("/api/tracking/{trackingNumber}/history")
    public List<TrackingHistoryResponse> history(@PathVariable String trackingNumber,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return events.history(trackingNumber, Caller.fromHeaders(userId, role));
    }

    @PostMapping("/api/tracking/events")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackingHistoryResponse addManualEvent(@Valid @RequestBody ManualTrackingEventRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return events.addManualEvent(request, Caller.fromHeaders(userId, role));
    }
}
