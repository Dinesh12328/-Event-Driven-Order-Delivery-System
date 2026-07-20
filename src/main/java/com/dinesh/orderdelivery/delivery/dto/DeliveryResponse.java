package com.dinesh.orderdelivery.delivery.dto;

import com.dinesh.orderdelivery.delivery.domain.DeliveryStatus;
import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        UUID agentId,
        String agentName,
        DeliveryStatus status,
        Instant estimatedDeliveryTime,
        Instant createdAt,
        Instant updatedAt
) {
}

