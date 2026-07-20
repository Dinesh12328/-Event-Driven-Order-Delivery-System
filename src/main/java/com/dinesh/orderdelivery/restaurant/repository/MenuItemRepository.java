package com.dinesh.orderdelivery.restaurant.repository;

import com.dinesh.orderdelivery.restaurant.domain.MenuItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByName(UUID restaurantId);
}

