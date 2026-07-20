package com.dinesh.orderdelivery.restaurant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        Boolean available
) {
}

