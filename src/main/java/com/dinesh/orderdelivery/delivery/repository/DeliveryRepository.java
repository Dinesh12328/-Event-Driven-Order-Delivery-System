package com.dinesh.orderdelivery.delivery.repository;

import com.dinesh.orderdelivery.delivery.domain.Delivery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    Optional<Delivery> findByCustomerOrderId(UUID orderId);
}

