package com.dinesh.orderdelivery.delivery.controller;

import com.dinesh.orderdelivery.common.api.ApiResponse;
import com.dinesh.orderdelivery.delivery.dto.DeliveryAssignmentRequest;
import com.dinesh.orderdelivery.delivery.dto.DeliveryResponse;
import com.dinesh.orderdelivery.delivery.dto.DeliveryStatusUpdateRequest;
import com.dinesh.orderdelivery.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{orderId}")
    ApiResponse<DeliveryResponse> get(@PathVariable UUID orderId, Principal principal) {
        return ApiResponse.success("Delivery", deliveryService.get(orderId, principal.getName()));
    }

    @PostMapping("/{orderId}/assign")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ApiResponse<DeliveryResponse> assign(
            @PathVariable UUID orderId,
            @Valid @RequestBody DeliveryAssignmentRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Delivery assigned", deliveryService.assign(orderId, request, principal.getName()));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT','ADMIN')")
    ApiResponse<DeliveryResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody DeliveryStatusUpdateRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Delivery status updated", deliveryService.updateStatus(orderId, request.status(), principal.getName()));
    }
}

