package com.dinesh.orderdelivery.payment.repository;

import com.dinesh.orderdelivery.payment.domain.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByCustomerOrderId(UUID orderId);
}

