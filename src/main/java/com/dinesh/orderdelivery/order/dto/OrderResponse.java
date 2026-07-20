package com.dinesh.orderdelivery.order.dto;

import com.dinesh.orderdelivery.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID restaurantId,
        String restaurantName,
        String deliveryAddress,
        OrderStatus status,
        BigDecimal totalPrice,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}

