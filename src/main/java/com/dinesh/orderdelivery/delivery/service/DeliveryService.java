package com.dinesh.orderdelivery.delivery.service;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.common.error.BadRequestException;
import com.dinesh.orderdelivery.common.error.ResourceNotFoundException;
import com.dinesh.orderdelivery.delivery.domain.Delivery;
import com.dinesh.orderdelivery.delivery.domain.DeliveryStatus;
import com.dinesh.orderdelivery.delivery.dto.DeliveryAssignmentRequest;
import com.dinesh.orderdelivery.delivery.dto.DeliveryResponse;
import com.dinesh.orderdelivery.delivery.mapper.DeliveryMapper;
import com.dinesh.orderdelivery.delivery.repository.DeliveryRepository;
import com.dinesh.orderdelivery.event.DomainEventPublisher;
import com.dinesh.orderdelivery.event.dto.DeliveryRequestedEvent;
import com.dinesh.orderdelivery.event.dto.NotificationEvent;
import com.dinesh.orderdelivery.order.domain.CustomerOrder;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import com.dinesh.orderdelivery.order.repository.OrderRepository;
import com.dinesh.orderdelivery.order.service.OrderStatusPolicy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private static final Map<DeliveryStatus, Set<DeliveryStatus>> TRANSITIONS = Map.of(
            DeliveryStatus.WAITING_FOR_AGENT, Set.of(DeliveryStatus.ASSIGNED, DeliveryStatus.CANCELLED),
            DeliveryStatus.ASSIGNED, Set.of(DeliveryStatus.PICKED_UP, DeliveryStatus.CANCELLED),
            DeliveryStatus.PICKED_UP, Set.of(DeliveryStatus.DELIVERED),
            DeliveryStatus.DELIVERED, Set.of(),
            DeliveryStatus.CANCELLED, Set.of()
    );

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryMapper mapper;
    private final OrderStatusPolicy orderStatusPolicy;
    private final DomainEventPublisher eventPublisher;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            DeliveryMapper mapper,
            OrderStatusPolicy orderStatusPolicy,
            DomainEventPublisher eventPublisher
    ) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.orderStatusPolicy = orderStatusPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DeliveryResponse prepareDelivery(DeliveryRequestedEvent event) {
        CustomerOrder order = order(event.orderId());
        Delivery delivery = deliveryRepository.findByCustomerOrderId(event.orderId())
                .orElseGet(() -> deliveryRepository.save(new Delivery(order)));
        return mapper.toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse assign(UUID orderId, DeliveryAssignmentRequest request, String email) {
        CustomerOrder order = order(orderId);
        assertOwnerOrAdmin(order, user(email));
        AppUser agent = userRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));
        if (agent.getRole() != Role.DELIVERY_AGENT) {
            throw new BadRequestException("Assigned user must have DELIVERY_AGENT role");
        }
        Delivery delivery = deliveryRepository.findByCustomerOrderId(orderId)
                .orElseGet(() -> deliveryRepository.save(new Delivery(order)));
        if (delivery.getStatus() == DeliveryStatus.DELIVERED || delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new BadRequestException("Cannot assign a completed delivery");
        }
        delivery.assign(agent, Instant.now().plus(request.estimatedMinutes(), ChronoUnit.MINUTES));
        eventPublisher.publishNotification(notification(orderId, "DELIVERY_AGENT", "Delivery assigned for order " + orderId));
        return mapper.toResponse(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse get(UUID orderId, String email) {
        CustomerOrder order = order(orderId);
        AppUser user = user(email);
        Delivery delivery = deliveryRepository.findByCustomerOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        assertCanView(order, delivery, user);
        return mapper.toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID orderId, DeliveryStatus nextStatus, String email) {
        Delivery delivery = deliveryRepository.findByCustomerOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        AppUser user = user(email);
        assertAgentOrAdmin(delivery, user);
        validateDeliveryTransition(delivery.getStatus(), nextStatus);
        CustomerOrder order = delivery.getCustomerOrder();
        if (nextStatus == DeliveryStatus.PICKED_UP) {
            orderStatusPolicy.validate(order.getStatus(), OrderStatus.PICKED_UP);
            order.changeStatus(OrderStatus.PICKED_UP);
        }
        if (nextStatus == DeliveryStatus.DELIVERED) {
            orderStatusPolicy.validate(order.getStatus(), OrderStatus.DELIVERED);
            order.changeStatus(OrderStatus.DELIVERED);
        }
        delivery.changeStatus(nextStatus);
        eventPublisher.publishNotification(notification(orderId, "CUSTOMER", "Delivery status changed to " + nextStatus));
        return mapper.toResponse(delivery);
    }

    private CustomerOrder order(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private AppUser user(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateDeliveryTransition(DeliveryStatus current, DeliveryStatus next) {
        if (current == next) {
            throw new BadRequestException("Delivery is already " + current);
        }
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new BadRequestException("Invalid delivery status transition from " + current + " to " + next);
        }
    }

    private void assertOwnerOrAdmin(CustomerOrder order, AppUser user) {
        if (user.getRole() == Role.ADMIN || order.getRestaurant().getOwner().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("Only restaurant owner or admin can assign delivery");
    }

    private void assertCanView(CustomerOrder order, Delivery delivery, AppUser user) {
        boolean customer = order.getCustomer().getId().equals(user.getId());
        boolean owner = order.getRestaurant().getOwner().getId().equals(user.getId());
        boolean agent = delivery.getAgent() != null && delivery.getAgent().getId().equals(user.getId());
        if (customer || owner || agent || user.getRole() == Role.ADMIN) {
            return;
        }
        throw new AccessDeniedException("You cannot view this delivery");
    }

    private void assertAgentOrAdmin(Delivery delivery, AppUser user) {
        boolean assignedAgent = delivery.getAgent() != null && delivery.getAgent().getId().equals(user.getId());
        if (assignedAgent || user.getRole() == Role.ADMIN) {
            return;
        }
        throw new AccessDeniedException("Only assigned delivery agent or admin can update delivery");
    }

    private NotificationEvent notification(UUID orderId, String recipientRole, String message) {
        return new NotificationEvent(UUID.randomUUID(), orderId, recipientRole, message, Instant.now());
    }
}

