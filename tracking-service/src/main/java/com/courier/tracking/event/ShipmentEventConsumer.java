package com.courier.tracking.event;

import com.courier.tracking.repository.TrackingEventRepository;
import com.courier.tracking.service.TrackingEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(ShipmentEventConsumer.class);
    private final TrackingEventService eventService;
    private final TrackingEventRepository events;

    public ShipmentEventConsumer(TrackingEventService eventService, TrackingEventRepository events) {
        this.eventService = eventService; this.events = events;
    }

    @RabbitListener(queues = TrackingRabbitConfig.QUEUE)
    public void consume(ShipmentEvent message) {
        try { eventService.validateShipmentEvent(message); }
        catch (InvalidShipmentEventException exception) {
            throw new AmqpRejectAndDontRequeueException("Malformed shipment event", exception);
        }
        String eventId = message.eventId().toString();
        if (events.existsByEventId(eventId)) {
            log.info("Skipping duplicate shipment event {}", eventId);
            return;
        }
        try {
            eventService.recordShipmentEvent(message);
        } catch (DataIntegrityViolationException exception) {
            if (events.existsByEventId(eventId)) {
                log.info("Skipping concurrently delivered shipment event {}", eventId);
                return;
            }
            throw exception;
        }
    }
}
