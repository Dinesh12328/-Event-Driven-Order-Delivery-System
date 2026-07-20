package com.dinesh.orderdelivery.order.service;

import com.dinesh.orderdelivery.common.error.BadRequestException;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusPolicy {

    private final Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);

    public OrderStatusPolicy() {
        transitions.put(OrderStatus.PLACED, Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.ACCEPTED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PREPARING, Set.of(OrderStatus.READY, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.READY, Set.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PICKED_UP, Set.of(OrderStatus.DELIVERED));
        transitions.put(OrderStatus.DELIVERED, Set.of());
        transitions.put(OrderStatus.CANCELLED, Set.of());
    }

    public void validate(OrderStatus current, OrderStatus next) {
        if (current == next) {
            throw new BadRequestException("Order is already " + current);
        }
        if (!transitions.getOrDefault(current, Set.of()).contains(next)) {
            throw new BadRequestException("Invalid order status transition from " + current + " to " + next);
        }
    }
}

