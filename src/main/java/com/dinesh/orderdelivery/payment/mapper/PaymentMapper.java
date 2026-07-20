package com.dinesh.orderdelivery.payment.mapper;

import com.dinesh.orderdelivery.payment.domain.Payment;
import com.dinesh.orderdelivery.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCustomerOrder().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}

