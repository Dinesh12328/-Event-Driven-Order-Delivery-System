package com.dinesh.orderdelivery.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestedEvent(
        UUID eventId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        Instant occurredAt
) {
}

