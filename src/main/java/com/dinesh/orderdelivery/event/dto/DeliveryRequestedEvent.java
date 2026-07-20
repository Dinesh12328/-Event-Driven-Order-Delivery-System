package com.dinesh.orderdelivery.event.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryRequestedEvent(
        UUID eventId,
        UUID orderId,
        UUID restaurantId,
        String restaurantName,
        String deliveryAddress,
        Instant occurredAt
) {
}

