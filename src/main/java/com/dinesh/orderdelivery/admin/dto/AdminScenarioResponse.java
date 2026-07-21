package com.dinesh.orderdelivery.admin.dto;

import com.dinesh.orderdelivery.delivery.dto.DeliveryResponse;
import com.dinesh.orderdelivery.order.dto.OrderResponse;
import com.dinesh.orderdelivery.payment.dto.PaymentResponse;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemResponse;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantResponse;
import java.util.List;

public record AdminScenarioResponse(
        RestaurantResponse restaurant,
        MenuItemResponse menuItem,
        OrderResponse order,
        PaymentResponse payment,
        DeliveryResponse delivery,
        List<String> completedSteps
) {
}
