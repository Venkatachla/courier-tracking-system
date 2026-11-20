package com.courier.tracking.repository;

import com.courier.tracking.model.TrackingEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    boolean existsByEventId(String eventId);
    long countByEventId(String eventId);
    boolean existsByTrackingNumber(String trackingNumber);
    List<TrackingEvent> findByTrackingNumberOrderByOccurredAtAscCreatedAtAscIdAsc(String trackingNumber);
}
