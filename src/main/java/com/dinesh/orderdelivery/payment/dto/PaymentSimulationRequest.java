package com.dinesh.orderdelivery.payment.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentSimulationRequest(
        @NotNull Boolean success,
        String failureReason
) {
}

