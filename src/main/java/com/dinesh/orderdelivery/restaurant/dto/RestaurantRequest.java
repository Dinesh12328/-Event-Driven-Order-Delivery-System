package com.dinesh.orderdelivery.restaurant.dto;

import jakarta.validation.constraints.NotBlank;

public record RestaurantRequest(
        @NotBlank String name,
        @NotBlank String cuisine,
        @NotBlank String location,
        @NotBlank String address,
        Boolean active
) {
}

