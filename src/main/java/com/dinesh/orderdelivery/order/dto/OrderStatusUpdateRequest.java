package com.dinesh.orderdelivery.order.dto;

import com.dinesh.orderdelivery.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status
) {
}

