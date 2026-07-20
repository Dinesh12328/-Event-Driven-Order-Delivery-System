package com.dinesh.orderdelivery.order.repository;

import com.dinesh.orderdelivery.order.domain.OrderItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}

