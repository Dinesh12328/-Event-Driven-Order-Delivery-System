package com.dinesh.orderdelivery.admin.service;

import com.dinesh.orderdelivery.admin.dto.AdminScenarioResponse;
import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.delivery.domain.DeliveryStatus;
import com.dinesh.orderdelivery.delivery.dto.DeliveryAssignmentRequest;
import com.dinesh.orderdelivery.delivery.dto.DeliveryResponse;
import com.dinesh.orderdelivery.delivery.service.DeliveryService;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import com.dinesh.orderdelivery.order.dto.CreateOrderItemRequest;
import com.dinesh.orderdelivery.order.dto.CreateOrderRequest;
import com.dinesh.orderdelivery.order.dto.OrderResponse;
import com.dinesh.orderdelivery.order.service.OrderService;
import com.dinesh.orderdelivery.payment.dto.PaymentResponse;
import com.dinesh.orderdelivery.payment.dto.PaymentSimulationRequest;
import com.dinesh.orderdelivery.payment.service.PaymentService;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemRequest;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemResponse;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantRequest;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantResponse;
import com.dinesh.orderdelivery.restaurant.service.RestaurantService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminScenarioService {

    private static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DeliveryService deliveryService;

    public AdminScenarioService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RestaurantService restaurantService,
            OrderService orderService,
            PaymentService paymentService,
            DeliveryService deliveryService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.restaurantService = restaurantService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.deliveryService = deliveryService;
    }

    @Transactional
    public AdminScenarioResponse runOrderJourney() {
        List<String> steps = new ArrayList<>();
        AppUser owner = ensureUser("Demo Owner", "demo.owner@example.com", Role.RESTAURANT_OWNER);
        AppUser customer = ensureUser("Demo Customer", "demo.customer@example.com", Role.CUSTOMER);
        AppUser agent = ensureUser("Demo Agent", "demo.agent@example.com", Role.DELIVERY_AGENT);
        steps.add("Demo users ready");

        long suffix = Instant.now().toEpochMilli();
        RestaurantResponse restaurant = restaurantService.create(new RestaurantRequest(
                "Dinesh Spice Kitchen " + suffix,
                "Indian",
                "Hyderabad",
                "Hitech City Road",
                true
        ), owner.getEmail());
        steps.add("Restaurant created");

        MenuItemResponse menuItem = restaurantService.addMenuItem(restaurant.id(), new MenuItemRequest(
                "Chicken Biryani",
                "Slow cooked biryani with raita",
                new BigDecimal("249.00"),
                true
        ), owner.getEmail());
        steps.add("Menu item created");

        OrderResponse order = orderService.create(new CreateOrderRequest(
                restaurant.id(),
                "Flat 42, Jubilee Hills",
                List.of(new CreateOrderItemRequest(menuItem.id(), 2))
        ), customer.getEmail());
        steps.add("Customer order placed");

        PaymentResponse payment = paymentService.simulate(order.id(), new PaymentSimulationRequest(true, null), customer.getEmail());
        steps.add("Payment marked successful");

        order = orderService.updateStatus(order.id(), OrderStatus.ACCEPTED, owner.getEmail());
        order = orderService.updateStatus(order.id(), OrderStatus.PREPARING, owner.getEmail());
        order = orderService.updateStatus(order.id(), OrderStatus.READY, owner.getEmail());
        steps.add("Restaurant prepared order");

        DeliveryResponse delivery = deliveryService.assign(order.id(), new DeliveryAssignmentRequest(agent.getId(), 35), owner.getEmail());
        steps.add("Delivery agent assigned");

        delivery = deliveryService.updateStatus(order.id(), DeliveryStatus.PICKED_UP, agent.getEmail());
        delivery = deliveryService.updateStatus(order.id(), DeliveryStatus.DELIVERED, agent.getEmail());
        order = orderService.get(order.id(), customer.getEmail());
        steps.add("Order delivered");

        return new AdminScenarioResponse(restaurant, menuItem, order, payment, delivery, steps);
    }

    private AppUser ensureUser(String fullName, String email, Role role) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> user.getRole() == role ? user : createUser(fullName, alternateEmail(email), role))
                .orElseGet(() -> createUser(fullName, email, role));
    }

    private AppUser createUser(String fullName, String email, Role role) {
        return userRepository.save(new AppUser(
                fullName,
                email,
                passwordEncoder.encode(DEMO_PASSWORD),
                role
        ));
    }

    private String alternateEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, at) + "." + Instant.now().toEpochMilli() + email.substring(at);
    }
}
