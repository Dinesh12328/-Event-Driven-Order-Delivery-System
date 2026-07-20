package com.dinesh.orderdelivery.event.dto;

import com.dinesh.orderdelivery.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID eventId,
        UUID orderId,
        UUID restaurantId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt
) {
}

