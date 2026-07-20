package com.dinesh.orderdelivery.restaurant.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        UUID ownerId,
        String ownerName,
        String name,
        String cuisine,
        String location,
        String address,
        boolean active,
        Instant createdAt,
        List<MenuItemResponse> menu
) {
}

