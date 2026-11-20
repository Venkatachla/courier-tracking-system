package com.courier.tracking.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.courier.tracking.client.ShipmentDirectoryService;
import com.courier.tracking.mapper.TrackingEventMapper;
import com.courier.tracking.model.ShipmentStatus;
import com.courier.tracking.model.TrackingEvent;
import com.courier.tracking.repository.TrackingEventRepository;
import com.courier.tracking.service.TrackingEventService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@ExtendWith(MockitoExtension.class)
class ShipmentEventConsumerTest {
    @Mock TrackingEventRepository events;
    @Mock TrackingEventMapper mapper;
    @Mock ShipmentDirectoryService shipmentDirectory;
    private ShipmentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        TrackingEventService service = new TrackingEventService(events, mapper, shipmentDirectory);
        consumer = new ShipmentEventConsumer(service, events);
    }

    @Test
    void validShipmentMessageCreatesTrackingRow() {
        ShipmentEvent message = event(UUID.randomUUID());
        when(events.existsByEventId(message.eventId().toString())).thenReturn(false);
        when(events.saveAndFlush(any(TrackingEvent.class))).thenAnswer(call -> call.getArgument(0));

        consumer.consume(message);

        ArgumentCaptor<TrackingEvent> saved = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(events).saveAndFlush(saved.capture());
        org.junit.jupiter.api.Assertions.assertEquals(message.eventId().toString(), saved.getValue().getEventId());
        org.junit.jupiter.api.Assertions.assertEquals("CT-20261120-ABC123", saved.getValue().getTrackingNumber());
        org.junit.jupiter.api.Assertions.assertEquals(ShipmentStatus.CREATED, saved.getValue().getStatus());
    }

    @Test
    void duplicateEventIdIsSkipped() {
        ShipmentEvent message = event(UUID.randomUUID());
        when(events.existsByEventId(message.eventId().toString())).thenReturn(true);

        consumer.consume(message);

        verify(events, never()).saveAndFlush(any());
    }

    @Test
    void malformedMessageIsRejectedWithoutRequeue() {
        ShipmentEvent malformed = new ShipmentEvent(null, "SHIPMENT_CREATED", 1L,
                "CT-20261120-ABC123", ShipmentStatus.CREATED, null, null, 3L, Instant.now());

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consume(malformed));
        verifyNoInteractions(events);
    }

    private static ShipmentEvent event(UUID id) {
        return new ShipmentEvent(id, "SHIPMENT_CREATED", 10L, "CT-20261120-ABC123",
                ShipmentStatus.CREATED, null, "Shipment created", 3L, Instant.parse("2026-11-20T10:00:00Z"));
    }
}
