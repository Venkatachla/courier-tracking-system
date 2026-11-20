package com.courier.shipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.courier.shipment.client.UserDirectoryService;
import com.courier.shipment.client.UserProfile;
import com.courier.shipment.dto.AssignCourierRequest;
import com.courier.shipment.dto.CreateShipmentRequest;
import com.courier.shipment.dto.ShipmentResponse;
import com.courier.shipment.dto.UpdateStatusRequest;
import com.courier.shipment.error.ApiException;
import com.courier.shipment.event.ShipmentEvent;
import com.courier.shipment.mapper.ShipmentMapper;
import com.courier.shipment.model.Shipment;
import com.courier.shipment.model.ShipmentStatus;
import com.courier.shipment.repository.ShipmentRepository;
import com.courier.shipment.security.Caller;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {
    @Mock ShipmentRepository shipments;
    @Mock ShipmentMapper mapper;
    @Mock UserDirectoryService userDirectory;
    @Mock ApplicationEventPublisher events;
    @InjectMocks ShipmentService service;
    @Spy TrackingNumberGenerator trackingNumbers = new TrackingNumberGenerator();

    @Test
    void trackingNumberMatchesExpectedFormat() {
        assertTrue(trackingNumbers.generate().matches("CT-\\d{8}-[A-Z0-9]{6}"));
    }

    @Test
    void createRetriesWhenTrackingNumberAlreadyExists() {
        doReturn("CT-20260101-AAAAAA", "CT-20260101-BBBBBB").when(trackingNumbers).generate();
        when(shipments.existsByTrackingNumber(any())).thenReturn(true, false);
        when(shipments.saveAndFlush(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(response(42L, ShipmentStatus.CREATED));

        service.create(new CreateShipmentRequest("Receiver", "Address", "555", BigDecimal.ONE), new Caller(3L, "CUSTOMER"));

        verify(shipments).existsByTrackingNumber("CT-20260101-AAAAAA");
        verify(shipments).existsByTrackingNumber("CT-20260101-BBBBBB");
        verify(shipments).saveAndFlush(argThat(shipment -> "CT-20260101-BBBBBB".equals(shipment.getTrackingNumber())));
    }

    @Test
    void createPersistsShipmentAndPublishesCreationEvent() throws Exception {
        when(shipments.existsByTrackingNumber(any())).thenReturn(false);
        when(shipments.saveAndFlush(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0); setId(shipment, 42L); return shipment;
        });
        when(mapper.toResponse(any())).thenReturn(response(42L, ShipmentStatus.CREATED));

        ShipmentResponse result = service.create(new CreateShipmentRequest("Receiver", "Address", "555", new BigDecimal("1.25")), new Caller(3L, "CUSTOMER"));

        assertEquals(42L, result.id());
        ArgumentCaptor<ShipmentEvent> event = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("SHIPMENT_CREATED", event.getValue().eventType());
        assertTrue(event.getValue().trackingNumber().matches("CT-\\d{8}-[A-Z0-9]{6}"));
        assertEquals(3L, event.getValue().updatedBy());
    }

    @Test
    void invalidStatusTransitionReturnsConflictWithoutSavingOrPublishing() {
        Shipment shipment = new Shipment("CT-20260101-ABC123", 3L, "Receiver", "Address", "555", BigDecimal.ONE);
        when(shipments.findById(9L)).thenReturn(Optional.of(shipment));

        ApiException error = assertThrows(ApiException.class, () -> service.updateStatus(9L,
                new UpdateStatusRequest(ShipmentStatus.DELIVERED, null, null), new Caller(1L, "ADMIN")));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(shipments, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void onlyAssignedCourierOrAdminCanUpdateStatus() {
        Shipment shipment = new Shipment("CT-20260101-ABC123", 3L, "Receiver", "Address", "555", BigDecimal.ONE);
        when(shipments.findById(9L)).thenReturn(Optional.of(shipment));

        ApiException error = assertThrows(ApiException.class, () -> service.updateStatus(9L,
                new UpdateStatusRequest(ShipmentStatus.PICKED_UP, null, null), new Caller(2L, "COURIER")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(shipments, never()).saveAndFlush(any());
    }

    @Test
    void statusUpdatePublishesStatusEvent() throws Exception {
        Shipment shipment = new Shipment("CT-20260101-ABC123", 3L, "Receiver", "Address", "555", BigDecimal.ONE);
        shipment.assignCourier(2L); shipment.setStatus(ShipmentStatus.PICKED_UP); setId(shipment, 9L);
        when(shipments.findById(9L)).thenReturn(Optional.of(shipment));
        when(shipments.saveAndFlush(shipment)).thenReturn(shipment);
        when(mapper.toResponse(shipment)).thenReturn(response(9L, ShipmentStatus.IN_TRANSIT));

        service.updateStatus(9L, new UpdateStatusRequest(ShipmentStatus.IN_TRANSIT, "Depot", "Departed"), new Caller(2L, "COURIER"));

        ArgumentCaptor<ShipmentEvent> event = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("SHIPMENT_STATUS_UPDATED", event.getValue().eventType());
        assertEquals(ShipmentStatus.IN_TRANSIT, event.getValue().status());
        assertEquals("Depot", event.getValue().location());
    }

    @Test
    void assignmentRequiresActualCourierRoleFromUserService() {
        when(userDirectory.getUser(4L)).thenReturn(new UserProfile(4L, "Customer", "CUSTOMER"));
        ApiException error = assertThrows(ApiException.class, () -> service.assignCourier(9L,
                new AssignCourierRequest(4L), new Caller(1L, "ADMIN")));
        assertEquals("INVALID_COURIER_ROLE", error.getCode());
        verify(shipments, never()).findById(any());
    }

    private static ShipmentResponse response(Long id, ShipmentStatus status) {
        return new ShipmentResponse(id, "CT-20260101-ABC123", 3L, "Receiver", "Address", "555",
                BigDecimal.ONE, status, 2L, null, null, 0L);
    }

    private static void setId(Shipment shipment, Long id) throws Exception {
        Field field = Shipment.class.getDeclaredField("id"); field.setAccessible(true); field.set(shipment, id);
    }
}
