package com.dinesh.orderdelivery.order;

import static org.hamcrest.Matchers.hasSize;
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
import com.dinesh.orderdelivery.delivery.repository.DeliveryRepository;
import com.dinesh.orderdelivery.event.repository.IntegrationEventLogRepository;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import com.dinesh.orderdelivery.order.dto.CreateOrderItemRequest;
import com.dinesh.orderdelivery.order.dto.CreateOrderRequest;
import com.dinesh.orderdelivery.order.dto.OrderStatusUpdateRequest;
import com.dinesh.orderdelivery.order.repository.OrderItemRepository;
import com.dinesh.orderdelivery.order.repository.OrderRepository;
import com.dinesh.orderdelivery.payment.repository.PaymentRepository;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemRequest;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantRequest;
import com.dinesh.orderdelivery.restaurant.repository.MenuItemRepository;
import com.dinesh.orderdelivery.restaurant.repository.RestaurantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
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
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private IntegrationEventLogRepository eventLogRepository;

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
    void customerPlacesOrderAndRolesMoveThroughValidStatuses() throws Exception {
        String ownerToken = tokenFor("Owner One", "owner@example.com", Role.RESTAURANT_OWNER);
        String customerToken = tokenFor("Customer One", "customer@example.com", Role.CUSTOMER);
        String agentToken = tokenFor("Agent One", "agent@example.com", Role.DELIVERY_AGENT);
        String restaurantId = createRestaurant(ownerToken);
        String menuItemId = createMenuItem(ownerToken, restaurantId);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                java.util.UUID.fromString(restaurantId),
                                "Flat 42, Jubilee Hills",
                                List.of(new CreateOrderItemRequest(java.util.UUID.fromString(menuItemId), 2))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("PLACED")))
                .andExpect(jsonPath("$.data.totalPrice", is(498.00)))
                .andReturn();
        String orderId = JsonPath.read(orderResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/orders/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/orders/restaurant/{restaurantId}", restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id", is(orderId)));

        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusUpdateRequest(OrderStatus.READY))))
                .andExpect(status().isBadRequest());

        updateStatus(orderId, ownerToken, OrderStatus.ACCEPTED, "ACCEPTED");
        updateStatus(orderId, ownerToken, OrderStatus.PREPARING, "PREPARING");
        updateStatus(orderId, ownerToken, OrderStatus.READY, "READY");
        updateStatus(orderId, agentToken, OrderStatus.PICKED_UP, "PICKED_UP");
        updateStatus(orderId, agentToken, OrderStatus.DELIVERED, "DELIVERED");

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DELIVERED")));
    }

    private void updateStatus(String orderId, String token, OrderStatus next, String expected) throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusUpdateRequest(next))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(expected)));
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
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(name, email, "Password123!", role))))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }
}
