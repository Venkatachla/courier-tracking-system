package com.courier.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;

import com.courier.tracking.event.ShipmentEvent;
import com.courier.tracking.event.ShipmentEventConsumer;
import com.courier.tracking.model.ShipmentStatus;
import com.courier.tracking.repository.TrackingEventRepository;
import com.courier.tracking.config.TrackingRabbitConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TrackingMessageIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("tracking_db").withUsername("test").withPassword("test");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management")
            .withUser("tracking-test", "tracking-test-password");

    @DynamicPropertySource
    static void serviceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "tracking-test");
        registry.add("spring.rabbitmq.password", () -> "tracking-test-password");
    }

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired TrackingEventRepository events;
    @SpyBean ShipmentEventConsumer consumer;

    @Test
    void shipmentCreatedMessageIsConsumedIdempotently() {
        UUID eventId = UUID.randomUUID();
        ShipmentEvent message = new ShipmentEvent(eventId, "SHIPMENT_CREATED", 19L,
                "CT-20261120-ABC123", ShipmentStatus.CREATED, null, "Shipment created", 3L,
                Instant.parse("2026-11-20T10:00:00Z"));

        rabbitTemplate.convertAndSend(TrackingRabbitConfig.SHIPMENT_EXCHANGE, "shipment.created", message);
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(20))
                .untilAsserted(() -> assertEquals(1L, events.countByEventId(eventId.toString())));

        rabbitTemplate.convertAndSend(TrackingRabbitConfig.SHIPMENT_EXCHANGE, "shipment.created", message);
        verify(consumer, timeout(20000).times(2)).consume(any(ShipmentEvent.class));
        assertEquals(1L, events.countByEventId(eventId.toString()));
    }
}
