package com.dinesh.orderdelivery.delivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeliveryAssignmentRequest(
        @NotNull UUID agentId,
        @NotNull @Min(5) Integer estimatedMinutes
) {
}

