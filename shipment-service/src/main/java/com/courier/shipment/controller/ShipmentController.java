package com.courier.shipment.controller;

import com.courier.shipment.dto.AssignCourierRequest;
import com.courier.shipment.dto.CreateShipmentRequest;
import com.courier.shipment.dto.ShipmentResponse;
import com.courier.shipment.dto.UpdateStatusRequest;
import com.courier.shipment.security.Caller;
import com.courier.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService shipments;
    public ShipmentController(ShipmentService shipments) { this.shipments = shipments; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse create(@Valid @RequestBody CreateShipmentRequest request,
                                  @RequestHeader(value = "X-User-Id", required = false) String userId,
                                  @RequestHeader(value = "X-User-Role", required = false) String role) {
        return shipments.create(request, Caller.fromHeaders(userId, role));
    }

    @GetMapping("/my")
    public Page<ShipmentResponse> mine(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                       @RequestHeader(value = "X-User-Role", required = false) String role,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return shipments.listMine(Caller.fromHeaders(userId, role), page, size);
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable Long id,
                                @RequestHeader(value = "X-User-Id", required = false) String userId,
                                @RequestHeader(value = "X-User-Role", required = false) String role) {
        return shipments.getById(id, Caller.fromHeaders(userId, role));
    }

    @PutMapping("/{id}/assign")
    public ShipmentResponse assign(@PathVariable Long id, @Valid @RequestBody AssignCourierRequest request,
                                   @RequestHeader(value = "X-User-Id", required = false) String userId,
                                   @RequestHeader(value = "X-User-Role", required = false) String role) {
        return shipments.assignCourier(id, request, Caller.fromHeaders(userId, role));
    }

    @PutMapping("/{id}/status")
    public ShipmentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) String userId,
                                         @RequestHeader(value = "X-User-Role", required = false) String role) {
        return shipments.updateStatus(id, request, Caller.fromHeaders(userId, role));
    }

    @PutMapping("/{id}/cancel")
    public ShipmentResponse cancel(@PathVariable Long id,
                                   @RequestHeader(value = "X-User-Id", required = false) String userId,
                                   @RequestHeader(value = "X-User-Role", required = false) String role) {
        return shipments.cancel(id, Caller.fromHeaders(userId, role));
    }
}
