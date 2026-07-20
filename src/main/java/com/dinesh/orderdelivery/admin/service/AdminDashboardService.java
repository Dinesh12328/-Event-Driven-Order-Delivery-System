package com.dinesh.orderdelivery.admin.service;

import com.dinesh.orderdelivery.admin.dto.AdminDashboardResponse;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.delivery.repository.DeliveryRepository;
import com.dinesh.orderdelivery.event.repository.IntegrationEventLogRepository;
import com.dinesh.orderdelivery.order.mapper.OrderMapper;
import com.dinesh.orderdelivery.order.repository.OrderRepository;
import com.dinesh.orderdelivery.payment.repository.PaymentRepository;
import com.dinesh.orderdelivery.restaurant.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final IntegrationEventLogRepository eventLogRepository;
    private final OrderMapper orderMapper;

    public AdminDashboardService(
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            DeliveryRepository deliveryRepository,
            IntegrationEventLogRepository eventLogRepository,
            OrderMapper orderMapper
    ) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryRepository = deliveryRepository;
        this.eventLogRepository = eventLogRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                userRepository.count(),
                restaurantRepository.count(),
                orderRepository.count(),
                paymentRepository.count(),
                deliveryRepository.count(),
                eventLogRepository.count(),
                orderRepository.findTop5ByOrderByCreatedAtDesc().stream().map(orderMapper::toResponse).toList()
        );
    }
}

