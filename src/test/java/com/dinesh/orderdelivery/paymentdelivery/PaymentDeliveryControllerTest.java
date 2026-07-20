package com.dinesh.orderdelivery.paymentdelivery;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.dto.LoginRequest;
import com.dinesh.orderdelivery.auth.dto.RegisterRequest;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.delivery.domain.DeliveryStatus;
import com.dinesh.orderdelivery.delivery.dto.DeliveryAssignmentRequest;
import com.dinesh.orderdelivery.delivery.dto.DeliveryStatusUpdateRequest;
import com.dinesh.orderdelivery.delivery.repository.DeliveryRepository;
import com.dinesh.orderdelivery.event.repository.IntegrationEventLogRepository;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import com.dinesh.orderdelivery.order.dto.CreateOrderItemRequest;
import com.dinesh.orderdelivery.order.dto.CreateOrderRequest;
import com.dinesh.orderdelivery.order.dto.OrderStatusUpdateRequest;
import com.dinesh.orderdelivery.order.repository.OrderItemRepository;
import com.dinesh.orderdelivery.order.repository.OrderRepository;
import com.dinesh.orderdelivery.payment.dto.PaymentSimulationRequest;
import com.dinesh.orderdelivery.payment.repository.PaymentRepository;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemRequest;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantRequest;
import com.dinesh.orderdelivery.restaurant.repository.MenuItemRepository;
import com.dinesh.orderdelivery.restaurant.repository.RestaurantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentDeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IntegrationEventLogRepository eventLogRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        eventLogRepository.deleteAll();
        deliveryRepository.deleteAll();
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void paymentSuccessDeliveryJourneyAndRefundAreSimulated() throws Exception {
        String ownerToken = tokenFor("Owner One", "owner@example.com", Role.RESTAURANT_OWNER);
        String customerToken = tokenFor("Customer One", "customer@example.com", Role.CUSTOMER);
        UserToken agent = userTokenFor("Agent One", "agent@example.com", Role.DELIVERY_AGENT);
        String adminToken = tokenFor("Admin One", "admin@example.com", Role.ADMIN);
        String restaurantId = createRestaurant(ownerToken);
        String menuItemId = createMenuItem(ownerToken, restaurantId);
        String orderId = createOrder(customerToken, restaurantId, menuItemId, 1);

        mockMvc.perform(post("/api/payments/{orderId}/simulate", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentSimulationRequest(true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.amount", is(249.00)));

        updateOrderStatus(orderId, ownerToken, OrderStatus.ACCEPTED);
        updateOrderStatus(orderId, ownerToken, OrderStatus.PREPARING);
        updateOrderStatus(orderId, ownerToken, OrderStatus.READY);

        mockMvc.perform(post("/api/deliveries/{orderId}/assign", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeliveryAssignmentRequest(agent.userId(), 35))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentName", is("Agent One")))
                .andExpect(jsonPath("$.data.estimatedDeliveryTime", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("ASSIGNED")));

        updateDeliveryStatus(orderId, agent.token(), DeliveryStatus.PICKED_UP, "PICKED_UP");
        updateDeliveryStatus(orderId, agent.token(), DeliveryStatus.DELIVERED, "DELIVERED");

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DELIVERED")));

        mockMvc.perform(get("/api/deliveries/{orderId}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DELIVERED")));

        mockMvc.perform(post("/api/payments/{orderId}/refund", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REFUNDED")));
    }

    @Test
    void paymentFailureCancelsPlacedOrder() throws Exception {
        String ownerToken = tokenFor("Owner One", "owner@example.com", Role.RESTAURANT_OWNER);
        String customerToken = tokenFor("Customer One", "customer@example.com", Role.CUSTOMER);
        String restaurantId = createRestaurant(ownerToken);
        String menuItemId = createMenuItem(ownerToken, restaurantId);
        String orderId = createOrder(customerToken, restaurantId, menuItemId, 1);

        mockMvc.perform(post("/api/payments/{orderId}/simulate", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentSimulationRequest(false, "Card declined"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("FAILED")))
                .andExpect(jsonPath("$.data.failureReason", is("Card declined")));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    private void updateOrderStatus(String orderId, String token, OrderStatus status) throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusUpdateRequest(status))))
                .andExpect(status().isOk());
    }

    private void updateDeliveryStatus(String orderId, String token, DeliveryStatus status, String expected) throws Exception {
        mockMvc.perform(patch("/api/deliveries/{orderId}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeliveryStatusUpdateRequest(status))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(expected)));
    }

    private String createOrder(String customerToken, String restaurantId, String menuItemId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                UUID.fromString(restaurantId),
                                "Flat 42, Jubilee Hills",
                                List.of(new CreateOrderItemRequest(UUID.fromString(menuItemId), quantity))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private String createRestaurant(String ownerToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/restaurants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestaurantRequest(
                                "Dinesh Biryani House",
                                "Indian",
                                "Hyderabad",
                                "Hitech City Road",
                                true
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private String createMenuItem(String ownerToken, String restaurantId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/restaurants/{restaurantId}/menu", restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuItemRequest(
                                "Chicken Biryani",
                                "Slow cooked biryani with raita",
                                new BigDecimal("249.00"),
                                true
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private String tokenFor(String name, String email, Role role) throws Exception {
        return userTokenFor(name, email, role).token();
    }

    private UserToken userTokenFor(String name, String email, Role role) throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(name, email, "Password123!", role))))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.data.user.id");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");
        return new UserToken(UUID.fromString(userId), token);
    }

    private record UserToken(UUID userId, String token) {
    }
}

