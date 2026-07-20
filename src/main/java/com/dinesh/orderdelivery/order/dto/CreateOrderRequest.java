package com.dinesh.orderdelivery.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID restaurantId,
        @NotBlank String deliveryAddress,
        @NotEmpty List<@Valid CreateOrderItemRequest> items
) {
}

