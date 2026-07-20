package com.dinesh.orderdelivery.payment.controller;

import com.dinesh.orderdelivery.common.api.ApiResponse;
import com.dinesh.orderdelivery.payment.dto.PaymentResponse;
import com.dinesh.orderdelivery.payment.dto.PaymentSimulationRequest;
import com.dinesh.orderdelivery.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{orderId}")
    ApiResponse<PaymentResponse> get(@PathVariable UUID orderId, Principal principal) {
        return ApiResponse.success("Payment", paymentService.get(orderId, principal.getName()));
    }

    @PostMapping("/{orderId}/simulate")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    ApiResponse<PaymentResponse> simulate(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentSimulationRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Payment simulated", paymentService.simulate(orderId, request, principal.getName()));
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<PaymentResponse> refund(@PathVariable UUID orderId) {
        return ApiResponse.success("Refund simulated", paymentService.refund(orderId));
    }
}

