package com.dinesh.orderdelivery.payment.dto;

import com.dinesh.orderdelivery.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}

