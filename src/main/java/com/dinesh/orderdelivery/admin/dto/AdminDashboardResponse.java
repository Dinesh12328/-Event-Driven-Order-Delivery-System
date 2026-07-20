package com.dinesh.orderdelivery.admin.dto;

import com.dinesh.orderdelivery.order.dto.OrderResponse;
import java.util.List;

public record AdminDashboardResponse(
        long users,
        long restaurants,
        long orders,
        long payments,
        long deliveries,
        long integrationEvents,
        List<OrderResponse> recentOrders
) {
}

