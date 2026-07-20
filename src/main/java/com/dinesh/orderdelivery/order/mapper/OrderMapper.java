package com.dinesh.orderdelivery.order.mapper;

import com.dinesh.orderdelivery.order.domain.CustomerOrder;
import com.dinesh.orderdelivery.order.domain.OrderItem;
import com.dinesh.orderdelivery.order.dto.OrderItemResponse;
import com.dinesh.orderdelivery.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getFullName(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getDeliveryAddress(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getItems().stream().map(this::toResponse).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getItemName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }
}

