package com.courier.shipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import com.courier.shipment.client.UserClient;
import com.courier.shipment.dto.CreateShipmentRequest;
import com.courier.shipment.dto.ShipmentResponse;
import com.courier.shipment.event.ShipmentEvent;
import com.courier.shipment.event.ShipmentEventPublisher;
import com.courier.shipment.model.ShipmentStatus;
import com.courier.shipment.security.Caller;
import com.courier.shipment.service.ShipmentService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ShipmentServiceIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("shipment_db").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.rabbitmq.dynamic", () -> false);
    }

    @MockBean UserClient userClient;
    @MockBean RabbitTemplate rabbitTemplate;
    @Autowired ShipmentService shipments;

    @Test
    void createsAndFetchesShipmentFromMysqlWithUserClientMocked() {
        ShipmentResponse created = shipments.create(
                new CreateShipmentRequest("Integration Receiver", "1 Test Road", "555-0111", new BigDecimal("3.250")),
                new Caller(77L, "CUSTOMER"));

        ShipmentResponse fetched = shipments.getById(created.id(), new Caller(77L, "CUSTOMER"));
        assertEquals(created.trackingNumber(), fetched.trackingNumber());
        assertEquals(ShipmentStatus.CREATED, fetched.status());
        assertEquals(77L, fetched.senderId());
        verify(rabbitTemplate).convertAndSend(eq(ShipmentEventPublisher.EXCHANGE), eq("shipment.created"), any(ShipmentEvent.class));
    }
}
