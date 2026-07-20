package com.dinesh.orderdelivery.event.dto;

import com.dinesh.orderdelivery.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        UUID customerId,
        UUID restaurantId,
        String restaurantName,
        String deliveryAddress,
        BigDecimal totalPrice,
        OrderStatus status,
        Instant occurredAt
) {
}

