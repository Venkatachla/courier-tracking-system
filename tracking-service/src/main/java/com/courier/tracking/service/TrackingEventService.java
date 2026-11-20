package com.courier.tracking.service;

import com.courier.tracking.client.ShipmentDirectoryService;
import com.courier.tracking.client.ShipmentNotFoundException;
import com.courier.tracking.client.ShipmentSummary;
import com.courier.tracking.dto.ManualTrackingEventRequest;
import com.courier.tracking.dto.TimelineEntryResponse;
import com.courier.tracking.dto.TrackingHistoryResponse;
import com.courier.tracking.dto.TrackingTimelineResponse;
import com.courier.tracking.error.ApiException;
import com.courier.tracking.event.InvalidShipmentEventException;
import com.courier.tracking.event.ShipmentEvent;
import com.courier.tracking.mapper.TrackingEventMapper;
import com.courier.tracking.model.TrackingEvent;
import com.courier.tracking.repository.TrackingEventRepository;
import com.courier.tracking.security.Caller;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackingEventService {
    private final TrackingEventRepository events;
    private final TrackingEventMapper mapper;
    private final ShipmentDirectoryService shipmentDirectory;

    public TrackingEventService(TrackingEventRepository events, TrackingEventMapper mapper,
                                ShipmentDirectoryService shipmentDirectory) {
        this.events = events; this.mapper = mapper; this.shipmentDirectory = shipmentDirectory;
    }

    @Transactional
    public void recordShipmentEvent(ShipmentEvent message) {
        validateShipmentEvent(message);
        events.saveAndFlush(new TrackingEvent(message.eventId().toString(), message.trackingNumber(),
                message.status(), message.location(), message.description(), message.updatedBy(), message.occurredAt()));
    }

    @Transactional(readOnly = true)
    public TrackingTimelineResponse publicTimeline(String trackingNumber) {
        List<TrackingEvent> rows = requireTimeline(trackingNumber);
        List<TimelineEntryResponse> timeline = rows.stream().map(mapper::toTimelineEntry).toList();
        return new TrackingTimelineResponse(trackingNumber, rows.get(rows.size() - 1).getStatus(), timeline);
    }

    @Transactional(readOnly = true)
    public List<TrackingHistoryResponse> history(String trackingNumber, Caller caller) {
        if (caller == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "A gateway user identity is required");
        return requireTimeline(trackingNumber).stream().map(mapper::toHistory).toList();
    }

    @Transactional
    public TrackingHistoryResponse addManualEvent(ManualTrackingEventRequest request, Caller caller) {
        if (!caller.isAdmin() && !"COURIER".equals(caller.role()))
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "COURIER or ADMIN role is required");
        ShipmentSummary shipment;
        try { shipment = shipmentDirectory.getByTrackingNumber(request.trackingNumber()); }
        catch (ShipmentNotFoundException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", "Shipment was not found");
        }
        if (!caller.isAdmin() && !caller.userId().equals(shipment.assignedCourierId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned courier may add an event");
        TrackingEvent saved = events.saveAndFlush(new TrackingEvent(null, shipment.trackingNumber(), request.status(),
                request.location(), request.description(), caller.userId(), Instant.now()));
        return mapper.toHistory(saved);
    }

    private List<TrackingEvent> requireTimeline(String trackingNumber) {
        List<TrackingEvent> rows = events.findByTrackingNumberOrderByOccurredAtAscCreatedAtAscIdAsc(trackingNumber);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "TRACKING_NOT_FOUND", "No tracking history was found for this tracking number");
        return rows;
    }

    public void validateShipmentEvent(ShipmentEvent event) {
        if (event == null || event.eventId() == null || event.eventType() == null
                || !(event.eventType().equals("SHIPMENT_CREATED") || event.eventType().equals("SHIPMENT_STATUS_UPDATED"))
                || event.shipmentId() == null || event.trackingNumber() == null || event.trackingNumber().isBlank()
                || event.trackingNumber().length() > 20 || event.status() == null
                || event.updatedBy() == null || event.occurredAt() == null)
            throw new InvalidShipmentEventException("Shipment event payload is missing required contract fields");
    }
}
