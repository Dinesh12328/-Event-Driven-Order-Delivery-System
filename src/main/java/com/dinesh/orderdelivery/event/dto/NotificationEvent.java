package com.dinesh.orderdelivery.event.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        UUID orderId,
        String recipientRole,
        String message,
        Instant occurredAt
) {
}

