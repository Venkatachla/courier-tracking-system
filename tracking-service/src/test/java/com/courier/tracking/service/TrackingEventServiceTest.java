package com.courier.tracking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.courier.tracking.client.ShipmentDirectoryService;
import com.courier.tracking.client.ShipmentSummary;
import com.courier.tracking.dto.ManualTrackingEventRequest;
import com.courier.tracking.dto.TimelineEntryResponse;
import com.courier.tracking.dto.TrackingHistoryResponse;
import com.courier.tracking.error.ApiException;
import com.courier.tracking.mapper.TrackingEventMapper;
import com.courier.tracking.model.ShipmentStatus;
import com.courier.tracking.model.TrackingEvent;
import com.courier.tracking.repository.TrackingEventRepository;
import com.courier.tracking.security.Caller;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TrackingEventServiceTest {
    @Mock TrackingEventRepository events;
    @Mock TrackingEventMapper mapper;
    @Mock ShipmentDirectoryService shipmentDirectory;
    @InjectMocks TrackingEventService service;

    @Test
    void assignedCourierCanAddManualEventAndIdentityIsRecorded() {
        when(shipmentDirectory.getByTrackingNumber("CT-20261120-ABC123"))
                .thenReturn(new ShipmentSummary(8L, "CT-20261120-ABC123", ShipmentStatus.IN_TRANSIT, 3L, 2L));
        when(events.saveAndFlush(any(TrackingEvent.class))).thenAnswer(call -> call.getArgument(0));
        when(mapper.toHistory(any())).thenReturn(new TrackingHistoryResponse(1L, null, "CT-20261120-ABC123",
                ShipmentStatus.IN_TRANSIT, "Depot", "Arrived", 2L, Instant.now(), Instant.now()));

        service.addManualEvent(new ManualTrackingEventRequest("CT-20261120-ABC123", ShipmentStatus.IN_TRANSIT,
                "Depot", "Arrived"), new Caller(2L, "COURIER"));

        ArgumentCaptor<TrackingEvent> saved = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(events).saveAndFlush(saved.capture());
        assertNull(saved.getValue().getEventId());
        assertEquals(2L, saved.getValue().getUpdatedBy());
    }

    @Test
    void nonAssignedCourierCannotAddManualEvent() {
        when(shipmentDirectory.getByTrackingNumber("CT-20261120-ABC123"))
                .thenReturn(new ShipmentSummary(8L, "CT-20261120-ABC123", ShipmentStatus.IN_TRANSIT, 3L, 4L));

        ApiException exception = assertThrows(ApiException.class, () -> service.addManualEvent(
                new ManualTrackingEventRequest("CT-20261120-ABC123", ShipmentStatus.IN_TRANSIT, "Depot", null),
                new Caller(2L, "COURIER")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(events, never()).saveAndFlush(any());
    }

    @Test
    void customerCannotAddManualEvent() {
        ApiException error = assertThrows(ApiException.class, () -> service.addManualEvent(
                new ManualTrackingEventRequest("CT-20261120-ABC123", ShipmentStatus.IN_TRANSIT, "Depot", null),
                new Caller(3L, "CUSTOMER")));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verifyNoInteractions(shipmentDirectory);
    }

    @Test
    void adminCanAddManualEventRegardlessOfAssignedCourier() {
        when(shipmentDirectory.getByTrackingNumber("CT-20261120-ABC123"))
                .thenReturn(new ShipmentSummary(8L, "CT-20261120-ABC123", ShipmentStatus.IN_TRANSIT, 3L, 4L));
        when(events.saveAndFlush(any(TrackingEvent.class))).thenAnswer(call -> call.getArgument(0));
        when(mapper.toHistory(any())).thenReturn(new TrackingHistoryResponse(1L, null, "CT-20261120-ABC123",
                ShipmentStatus.IN_TRANSIT, null, null, 1L, Instant.now(), Instant.now()));

        service.addManualEvent(new ManualTrackingEventRequest("CT-20261120-ABC123", ShipmentStatus.IN_TRANSIT, null, null),
                new Caller(1L, "ADMIN"));

        verify(events).saveAndFlush(any(TrackingEvent.class));
    }

    @Test
    void publicTimelineUsesOldestToNewestAndLatestStatus() {
        TrackingEvent first = new TrackingEvent("event-1", "CT-20261120-ABC123", ShipmentStatus.CREATED,
                null, "Created", 3L, Instant.parse("2026-11-20T10:00:00Z"));
        TrackingEvent second = new TrackingEvent("event-2", "CT-20261120-ABC123", ShipmentStatus.PICKED_UP,
                "Depot", "Picked up", 2L, Instant.parse("2026-11-20T11:00:00Z"));
        when(events.findByTrackingNumberOrderByOccurredAtAscCreatedAtAscIdAsc("CT-20261120-ABC123"))
                .thenReturn(List.of(first, second));
        when(mapper.toTimelineEntry(first)).thenReturn(new TimelineEntryResponse(ShipmentStatus.CREATED, null, "Created", first.getOccurredAt()));
        when(mapper.toTimelineEntry(second)).thenReturn(new TimelineEntryResponse(ShipmentStatus.PICKED_UP, "Depot", "Picked up", second.getOccurredAt()));

        var result = service.publicTimeline("CT-20261120-ABC123");

        assertEquals(ShipmentStatus.PICKED_UP, result.currentStatus());
        assertEquals(ShipmentStatus.CREATED, result.events().get(0).status());
        assertEquals(ShipmentStatus.PICKED_UP, result.events().get(1).status());
        assertEquals("Depot", result.events().get(1).location());
    }

    @Test
    void unknownPublicTrackingNumberReturnsNotFound() {
        when(events.findByTrackingNumberOrderByOccurredAtAscCreatedAtAscIdAsc("missing")).thenReturn(List.of());
        ApiException error = assertThrows(ApiException.class, () -> service.publicTimeline("missing"));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}
