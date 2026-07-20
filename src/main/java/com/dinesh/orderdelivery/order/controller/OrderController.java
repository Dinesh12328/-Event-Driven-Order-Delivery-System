package com.dinesh.orderdelivery.order.controller;

import com.dinesh.orderdelivery.common.api.ApiResponse;
import com.dinesh.orderdelivery.order.dto.CreateOrderRequest;
import com.dinesh.orderdelivery.order.dto.OrderResponse;
import com.dinesh.orderdelivery.order.dto.OrderStatusUpdateRequest;
import com.dinesh.orderdelivery.order.service.OrderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed", orderService.create(request, principal.getName())));
    }

    @GetMapping("/my")
    ApiResponse<List<OrderResponse>> myOrders(Principal principal) {
        return ApiResponse.success("My orders", orderService.myOrders(principal.getName()));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ApiResponse<List<OrderResponse>> restaurantOrders(@PathVariable UUID restaurantId, Principal principal) {
        return ApiResponse.success("Restaurant orders", orderService.restaurantOrders(restaurantId, principal.getName()));
    }

    @GetMapping("/{orderId}")
    ApiResponse<OrderResponse> get(@PathVariable UUID orderId, Principal principal) {
        return ApiResponse.success("Order", orderService.get(orderId, principal.getName()));
    }

    @PatchMapping("/{orderId}/status")
    ApiResponse<OrderResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Order status updated", orderService.updateStatus(orderId, request.status(), principal.getName()));
    }
}

