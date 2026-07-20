package com.dinesh.orderdelivery.delivery.mapper;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.delivery.domain.Delivery;
import com.dinesh.orderdelivery.delivery.dto.DeliveryResponse;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {

    public DeliveryResponse toResponse(Delivery delivery) {
        AppUser agent = delivery.getAgent();
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getCustomerOrder().getId(),
                agent == null ? null : agent.getId(),
                agent == null ? null : agent.getFullName(),
                delivery.getStatus(),
                delivery.getEstimatedDeliveryTime(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}

