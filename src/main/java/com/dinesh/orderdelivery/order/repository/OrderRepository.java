package com.dinesh.orderdelivery.order.repository;

import com.dinesh.orderdelivery.order.domain.CustomerOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    @EntityGraph(attributePaths = {"customer", "restaurant", "restaurant.owner", "items", "items.menuItem"})
    List<CustomerOrder> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"customer", "restaurant", "restaurant.owner", "items", "items.menuItem"})
    List<CustomerOrder> findByRestaurantOwnerEmailIgnoreCaseOrderByCreatedAtDesc(String ownerEmail);

    @EntityGraph(attributePaths = {"customer", "restaurant", "restaurant.owner", "items", "items.menuItem"})
    List<CustomerOrder> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);

    List<CustomerOrder> findTop5ByOrderByCreatedAtDesc();
}
