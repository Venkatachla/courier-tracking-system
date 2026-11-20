package com.courier.shipment.service;

import com.courier.shipment.client.UserDirectoryService;
import com.courier.shipment.client.UserNotFoundException;
import com.courier.shipment.client.UserProfile;
import com.courier.shipment.dto.AssignCourierRequest;
import com.courier.shipment.dto.CreateShipmentRequest;
import com.courier.shipment.dto.InternalShipmentResponse;
import com.courier.shipment.dto.ShipmentResponse;
import com.courier.shipment.dto.UpdateStatusRequest;
import com.courier.shipment.error.ApiException;
import com.courier.shipment.event.ShipmentEvent;
import com.courier.shipment.mapper.ShipmentMapper;
import com.courier.shipment.model.Shipment;
import com.courier.shipment.model.ShipmentStatus;
import com.courier.shipment.repository.ShipmentRepository;
import com.courier.shipment.security.Caller;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ShipmentStatus.CREATED, Set.of(ShipmentStatus.PICKED_UP),
            ShipmentStatus.PICKED_UP, Set.of(ShipmentStatus.IN_TRANSIT),
            ShipmentStatus.IN_TRANSIT, Set.of(ShipmentStatus.OUT_FOR_DELIVERY),
            ShipmentStatus.OUT_FOR_DELIVERY, Set.of(ShipmentStatus.DELIVERED, ShipmentStatus.FAILED_DELIVERY),
            ShipmentStatus.FAILED_DELIVERY, Set.of(ShipmentStatus.IN_TRANSIT));

    private final ShipmentRepository shipments;
    private final ShipmentMapper mapper;
    private final TrackingNumberGenerator trackingNumbers;
    private final UserDirectoryService userDirectory;
    private final ApplicationEventPublisher events;

    public ShipmentService(ShipmentRepository shipments, ShipmentMapper mapper,
                           TrackingNumberGenerator trackingNumbers, UserDirectoryService userDirectory,
                           ApplicationEventPublisher events) {
        this.shipments = shipments; this.mapper = mapper; this.trackingNumbers = trackingNumbers;
        this.userDirectory = userDirectory; this.events = events;
    }

    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request, Caller caller) {
        requireRole(caller, "CUSTOMER", "ADMIN");
        String trackingNumber = nextTrackingNumber();
        Shipment shipment = new Shipment(trackingNumber, caller.userId(), request.receiverName().trim(),
                request.receiverAddress().trim(), request.receiverPhone().trim(), request.weight());
        Shipment saved = save(shipment);
        events.publishEvent(new ShipmentEvent(UUID.randomUUID(), "SHIPMENT_CREATED", saved.getId(),
                saved.getTrackingNumber(), saved.getStatus(), null, "Shipment created", caller.userId(), Instant.now()));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponse> listMine(Caller caller, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "page must be non-negative and size must be between 1 and 100");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Shipment> result;
        if (caller.isAdmin()) result = shipments.findAll(pageable);
        else if ("CUSTOMER".equals(caller.role())) result = shipments.findBySenderId(caller.userId(), pageable);
        else if ("COURIER".equals(caller.role())) result = shipments.findByAssignedCourierId(caller.userId(), pageable);
        else throw forbidden("CUSTOMER, COURIER or ADMIN role is required");
        return result.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(Long id, Caller caller) {
        Shipment shipment = find(id);
        if (!caller.isAdmin() && !("CUSTOMER".equals(caller.role()) && caller.userId().equals(shipment.getSenderId()))
                && !("COURIER".equals(caller.role()) && caller.userId().equals(shipment.getAssignedCourierId())))
            throw forbidden("Shipment is not available to this user");
        return mapper.toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public InternalShipmentResponse getInternalByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipments.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", "Shipment was not found"));
        return new InternalShipmentResponse(shipment.getId(), shipment.getTrackingNumber(), shipment.getStatus(),
                shipment.getSenderId(), shipment.getAssignedCourierId());
    }

    @Transactional
    public ShipmentResponse assignCourier(Long id, AssignCourierRequest request, Caller caller) {
        requireRole(caller, "ADMIN");
        UserProfile courier;
        try { courier = userDirectory.getUser(request.courierId()); }
        catch (UserNotFoundException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "COURIER_NOT_FOUND", "Courier user was not found"); }
        if (!"COURIER".equals(courier.role())) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COURIER_ROLE", "The selected user does not have the COURIER role");
        Shipment shipment = find(id);
        shipment.assignCourier(courier.id());
        return mapper.toResponse(save(shipment));
    }

    @Transactional
    public ShipmentResponse updateStatus(Long id, UpdateStatusRequest request, Caller caller) {
        Shipment shipment = find(id);
        requireCanUpdateStatus(shipment, caller);
        transition(shipment, request.status());
        Shipment saved = save(shipment);
        events.publishEvent(new ShipmentEvent(UUID.randomUUID(), "SHIPMENT_STATUS_UPDATED", saved.getId(),
                saved.getTrackingNumber(), saved.getStatus(), request.location(), request.description(), caller.userId(), Instant.now()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public ShipmentResponse cancel(Long id, Caller caller) {
        Shipment shipment = find(id);
        if (!caller.isAdmin() && !("CUSTOMER".equals(caller.role()) && caller.userId().equals(shipment.getSenderId())))
            throw forbidden("Only the shipment owner or ADMIN may cancel it");
        if (shipment.getStatus() != ShipmentStatus.CREATED)
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", "A shipment can only be cancelled while CREATED");
        shipment.setStatus(ShipmentStatus.CANCELLED);
        Shipment saved = save(shipment);
        events.publishEvent(new ShipmentEvent(UUID.randomUUID(), "SHIPMENT_STATUS_UPDATED", saved.getId(),
                saved.getTrackingNumber(), saved.getStatus(), null, "Shipment cancelled", caller.userId(), Instant.now()));
        return mapper.toResponse(saved);
    }

    private Shipment find(Long id) {
        return shipments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", "Shipment was not found"));
    }

    private Shipment save(Shipment shipment) {
        try { return shipments.saveAndFlush(shipment); }
        catch (ObjectOptimisticLockingFailureException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Shipment was changed by another request; reload and retry");
        } catch (DataIntegrityViolationException exception) {
            if (shipment.getId() == null && exception.getMostSpecificCause().getMessage() != null
                    && exception.getMostSpecificCause().getMessage().contains("tracking_number"))
                throw new ApiException(HttpStatus.CONFLICT, "TRACKING_NUMBER_COLLISION", "Tracking number collision; retry the request");
            throw exception;
        }
    }

    private String nextTrackingNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = trackingNumbers.generate();
            if (!shipments.existsByTrackingNumber(candidate)) return candidate;
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TRACKING_NUMBER_UNAVAILABLE", "Could not reserve a unique tracking number; retry the request");
    }

    private void transition(Shipment shipment, ShipmentStatus next) {
        ShipmentStatus current = shipment.getStatus();
        if (current == ShipmentStatus.DELIVERED || current == ShipmentStatus.CANCELLED
                || !ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next))
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", "Status cannot change from " + current + " to " + next);
        shipment.setStatus(next);
    }

    private void requireCanUpdateStatus(Shipment shipment, Caller caller) {
        if (caller.isAdmin()) return;
        if (!"COURIER".equals(caller.role()) || !caller.userId().equals(shipment.getAssignedCourierId()))
            throw forbidden("Only the assigned COURIER or ADMIN may update status");
    }

    private void requireRole(Caller caller, String... roles) {
        for (String role : roles) if (role.equals(caller.role())) return;
        throw forbidden("Role is not allowed for this operation");
    }

    private ApiException forbidden(String message) { return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message); }
}
