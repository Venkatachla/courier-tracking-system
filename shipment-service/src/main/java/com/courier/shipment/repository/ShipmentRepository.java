package com.courier.shipment.repository;

import com.courier.shipment.model.Shipment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    boolean existsByTrackingNumber(String trackingNumber);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    Page<Shipment> findBySenderId(Long senderId, Pageable pageable);
    Page<Shipment> findByAssignedCourierId(Long courierId, Pageable pageable);
}
