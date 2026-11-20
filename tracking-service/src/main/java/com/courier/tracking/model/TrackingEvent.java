package com.courier.tracking.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tracking_events", indexes = {
        @Index(name = "idx_tracking_events_tracking_number", columnList = "tracking_number"),
        @Index(name = "idx_tracking_events_tracking_occurred", columnList = "tracking_number, occurred_at"),
        @Index(name = "uk_tracking_events_event_id", columnList = "event_id", unique = true)
})
public class TrackingEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", length = 36, unique = true)
    private String eventId;
    @Column(name = "tracking_number", nullable = false, length = 20)
    private String trackingNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ShipmentStatus status;
    @Column(length = 200)
    private String location;
    @Column(length = 1000)
    private String description;
    @Column(name = "updated_by")
    private Long updatedBy;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TrackingEvent() {}

    public TrackingEvent(String eventId, String trackingNumber, ShipmentStatus status, String location,
                         String description, Long updatedBy, Instant occurredAt) {
        this.eventId = eventId; this.trackingNumber = trackingNumber; this.status = status;
        this.location = location; this.description = description; this.updatedBy = updatedBy; this.occurredAt = occurredAt;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getTrackingNumber() { return trackingNumber; }
    public ShipmentStatus getStatus() { return status; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public Long getUpdatedBy() { return updatedBy; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
