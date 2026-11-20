package com.courier.shipment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipments_tracking_number", columnList = "tracking_number", unique = true),
        @Index(name = "idx_shipments_sender_id", columnList = "sender_id"),
        @Index(name = "idx_shipments_assigned_courier_id", columnList = "assigned_courier_id"),
        @Index(name = "idx_shipments_status", columnList = "status")
})
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tracking_number", nullable = false, unique = true, length = 20)
    private String trackingNumber;
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(name = "receiver_name", nullable = false, length = 120)
    private String receiverName;
    @Column(name = "receiver_address", nullable = false, length = 500)
    private String receiverAddress;
    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal weight;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ShipmentStatus status = ShipmentStatus.CREATED;
    @Column(name = "assigned_courier_id")
    private Long assignedCourierId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private Long version;

    protected Shipment() {}

    public Shipment(String trackingNumber, Long senderId, String receiverName, String receiverAddress,
                    String receiverPhone, BigDecimal weight) {
        this.trackingNumber = trackingNumber;
        this.senderId = senderId;
        this.receiverName = receiverName;
        this.receiverAddress = receiverAddress;
        this.receiverPhone = receiverPhone;
        this.weight = weight;
    }

    @PrePersist
    void onCreate() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public Long getSenderId() { return senderId; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverAddress() { return receiverAddress; }
    public String getReceiverPhone() { return receiverPhone; }
    public BigDecimal getWeight() { return weight; }
    public ShipmentStatus getStatus() { return status; }
    public Long getAssignedCourierId() { return assignedCourierId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void assignCourier(Long courierId) { this.assignedCourierId = courierId; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
}
