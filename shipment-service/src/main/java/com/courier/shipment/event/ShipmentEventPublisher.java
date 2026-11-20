package com.courier.shipment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class ShipmentEventPublisher {
    public static final String EXCHANGE = "shipment.exchange";
    private static final Logger log = LoggerFactory.getLogger(ShipmentEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ShipmentEventPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(ShipmentEvent event) {
        String routingKey = "SHIPMENT_CREATED".equals(event.eventType()) ? "shipment.created" : "shipment.status_updated";
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (RuntimeException exception) {
            log.error("Could not publish shipment event {} after transaction commit", event.eventId(), exception);
        }
    }
}
