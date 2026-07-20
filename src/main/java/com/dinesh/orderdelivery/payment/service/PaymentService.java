package com.dinesh.orderdelivery.payment.service;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.common.error.BadRequestException;
import com.dinesh.orderdelivery.common.error.ResourceNotFoundException;
import com.dinesh.orderdelivery.event.DomainEventPublisher;
import com.dinesh.orderdelivery.event.dto.NotificationEvent;
import com.dinesh.orderdelivery.event.dto.PaymentRequestedEvent;
import com.dinesh.orderdelivery.order.domain.CustomerOrder;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import com.dinesh.orderdelivery.order.repository.OrderRepository;
import com.dinesh.orderdelivery.order.service.OrderStatusPolicy;
import com.dinesh.orderdelivery.payment.domain.Payment;
import com.dinesh.orderdelivery.payment.domain.PaymentStatus;
import com.dinesh.orderdelivery.payment.dto.PaymentResponse;
import com.dinesh.orderdelivery.payment.dto.PaymentSimulationRequest;
import com.dinesh.orderdelivery.payment.mapper.PaymentMapper;
import com.dinesh.orderdelivery.payment.repository.PaymentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentMapper mapper;
    private final OrderStatusPolicy statusPolicy;
    private final DomainEventPublisher eventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            PaymentMapper mapper,
            OrderStatusPolicy statusPolicy,
            DomainEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.statusPolicy = statusPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentResponse preparePayment(PaymentRequestedEvent event) {
        CustomerOrder order = order(event.orderId());
        Payment payment = paymentRepository.findByCustomerOrderId(event.orderId())
                .orElseGet(() -> paymentRepository.save(new Payment(order, event.amount())));
        return mapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse simulate(UUID orderId, PaymentSimulationRequest request, String email) {
        CustomerOrder order = order(orderId);
        AppUser user = user(email);
        assertCustomerOrAdmin(order, user);
        Payment payment = paymentRepository.findByCustomerOrderId(orderId)
                .orElseGet(() -> paymentRepository.save(new Payment(order, order.getTotalPrice())));

        if (request.success()) {
            payment.succeed();
            eventPublisher.publishNotification(notification(orderId, "CUSTOMER", "Payment successful for order " + orderId));
        } else {
            String reason = StringUtils.hasText(request.failureReason()) ? request.failureReason().trim() : "Payment gateway declined the transaction";
            payment.fail(reason);
            cancelOrderAfterFailure(order);
            eventPublisher.publishNotification(notification(orderId, "CUSTOMER", "Payment failed and order was cancelled"));
        }
        return mapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID orderId, String email) {
        CustomerOrder order = order(orderId);
        assertCanView(order, user(email));
        Payment payment = paymentRepository.findByCustomerOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse refund(UUID orderId) {
        Payment payment = paymentRepository.findByCustomerOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Only successful payments can be refunded");
        }
        payment.refund();
        eventPublisher.publishNotification(notification(orderId, "CUSTOMER", "Refund processed for order " + orderId));
        return mapper.toResponse(payment);
    }

    private void cancelOrderAfterFailure(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            return;
        }
        statusPolicy.validate(order.getStatus(), OrderStatus.CANCELLED);
        order.changeStatus(OrderStatus.CANCELLED);
    }

    private NotificationEvent notification(UUID orderId, String recipientRole, String message) {
        return new NotificationEvent(UUID.randomUUID(), orderId, recipientRole, message, Instant.now());
    }

    private CustomerOrder order(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private AppUser user(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void assertCustomerOrAdmin(CustomerOrder order, AppUser user) {
        if (user.getRole() == Role.ADMIN || order.getCustomer().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("Only the customer or admin can simulate payment");
    }

    private void assertCanView(CustomerOrder order, AppUser user) {
        boolean customer = order.getCustomer().getId().equals(user.getId());
        boolean owner = order.getRestaurant().getOwner().getId().equals(user.getId());
        if (customer || owner || user.getRole() == Role.ADMIN) {
            return;
        }
        throw new AccessDeniedException("You cannot view this payment");
    }
}

