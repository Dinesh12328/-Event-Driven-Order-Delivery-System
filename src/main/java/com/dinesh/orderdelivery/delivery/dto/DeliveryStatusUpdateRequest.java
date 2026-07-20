package com.dinesh.orderdelivery.delivery.dto;

import com.dinesh.orderdelivery.delivery.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record DeliveryStatusUpdateRequest(
        @NotNull DeliveryStatus status
) {
}

