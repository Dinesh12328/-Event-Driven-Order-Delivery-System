package com.dinesh.orderdelivery.order.service;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.common.error.BadRequestException;
import com.dinesh.orderdelivery.common.error.ResourceNotFoundException;
import com.dinesh.orderdelivery.event.DomainEventPublisher;
import com.dinesh.orderdelivery.order.domain.CustomerOrder;
import com.dinesh.orderdelivery.order.domain.OrderItem;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import com.dinesh.orderdelivery.order.dto.CreateOrderItemRequest;
import com.dinesh.orderdelivery.order.dto.CreateOrderRequest;
import com.dinesh.orderdelivery.order.dto.OrderResponse;
import com.dinesh.orderdelivery.order.mapper.OrderMapper;
import com.dinesh.orderdelivery.order.repository.OrderRepository;
import com.dinesh.orderdelivery.restaurant.domain.MenuItem;
import com.dinesh.orderdelivery.restaurant.domain.Restaurant;
import com.dinesh.orderdelivery.restaurant.repository.MenuItemRepository;
import com.dinesh.orderdelivery.restaurant.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Set<OrderStatus> RESTAURANT_STATUSES = Set.of(
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.CANCELLED
    );
    private static final Set<OrderStatus> DELIVERY_STATUSES = Set.of(OrderStatus.PICKED_UP, OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderMapper mapper;
    private final OrderStatusPolicy statusPolicy;
    private final DomainEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            OrderMapper mapper,
            OrderStatusPolicy statusPolicy,
            DomainEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.mapper = mapper;
        this.statusPolicy = statusPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String customerEmail) {
        AppUser customer = user(customerEmail);
        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!restaurant.isActive()) {
            throw new BadRequestException("Restaurant is not accepting orders");
        }

        CustomerOrder order = new CustomerOrder(customer, restaurant, request.deliveryAddress().trim());
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
            if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BadRequestException("Menu item does not belong to the selected restaurant");
            }
            if (!menuItem.isAvailable()) {
                throw new BadRequestException(menuItem.getName() + " is unavailable");
            }
            OrderItem item = new OrderItem(order, menuItem, itemRequest.quantity());
            order.addItem(item);
            total = total.add(item.getLineTotal());
        }
        order.setTotalPrice(total);
        CustomerOrder saved = orderRepository.save(order);
        eventPublisher.publishOrderPlaced(saved);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId, String email) {
        CustomerOrder order = findOrder(orderId);
        assertCanView(order, user(email));
        return mapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(String email) {
        return orderRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> restaurantOrders(UUID restaurantId, String email) {
        AppUser user = user(email);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (user.getRole() != Role.ADMIN && !restaurant.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only restaurant owner or admin can view restaurant orders");
        }
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus nextStatus, String email) {
        CustomerOrder order = findOrder(orderId);
        AppUser user = user(email);
        OrderStatus previousStatus = order.getStatus();
        statusPolicy.validate(previousStatus, nextStatus);
        assertCanChangeStatus(order, user, nextStatus);
        order.changeStatus(nextStatus);
        eventPublisher.publishOrderStatusChanged(order, previousStatus, nextStatus);
        return mapper.toResponse(order);
    }

    private CustomerOrder findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private AppUser user(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void assertCanView(CustomerOrder order, AppUser user) {
        boolean customer = order.getCustomer().getId().equals(user.getId());
        boolean owner = order.getRestaurant().getOwner().getId().equals(user.getId());
        boolean deliveryOrAdmin = user.getRole() == Role.DELIVERY_AGENT || user.getRole() == Role.ADMIN;
        if (customer || owner || deliveryOrAdmin) {
            return;
        }
        throw new AccessDeniedException("You cannot view this order");
    }

    private void assertCanChangeStatus(CustomerOrder order, AppUser user, OrderStatus nextStatus) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.CUSTOMER
                && order.getCustomer().getId().equals(user.getId())
                && nextStatus == OrderStatus.CANCELLED) {
            return;
        }
        if (user.getRole() == Role.RESTAURANT_OWNER
                && order.getRestaurant().getOwner().getId().equals(user.getId())
                && RESTAURANT_STATUSES.contains(nextStatus)) {
            return;
        }
        if (user.getRole() == Role.DELIVERY_AGENT && DELIVERY_STATUSES.contains(nextStatus)) {
            return;
        }
        throw new AccessDeniedException("Role cannot move order to " + nextStatus);
    }
}
