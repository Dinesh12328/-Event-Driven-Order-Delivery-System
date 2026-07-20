package com.dinesh.orderdelivery.restaurant.mapper;

import com.dinesh.orderdelivery.restaurant.domain.MenuItem;
import com.dinesh.orderdelivery.restaurant.domain.Restaurant;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemResponse;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMapper {

    public RestaurantResponse toResponse(Restaurant restaurant) {
        return toResponse(restaurant, List.of());
    }

    public RestaurantResponse toResponse(Restaurant restaurant, List<MenuItemResponse> menu) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getOwner().getId(),
                restaurant.getOwner().getFullName(),
                restaurant.getName(),
                restaurant.getCuisine(),
                restaurant.getLocation(),
                restaurant.getAddress(),
                restaurant.isActive(),
                restaurant.getCreatedAt(),
                menu
        );
    }

    public MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getRestaurant().getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isAvailable(),
                item.getCreatedAt()
        );
    }
}

