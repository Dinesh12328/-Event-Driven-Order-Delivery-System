package com.dinesh.orderdelivery.auth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.dto.LoginRequest;
import com.dinesh.orderdelivery.auth.dto.RegisterRequest;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.delivery.repository.DeliveryRepository;
import com.dinesh.orderdelivery.event.repository.IntegrationEventLogRepository;
import com.dinesh.orderdelivery.order.repository.OrderItemRepository;
import com.dinesh.orderdelivery.order.repository.OrderRepository;
import com.dinesh.orderdelivery.payment.repository.PaymentRepository;
import com.dinesh.orderdelivery.restaurant.repository.MenuItemRepository;
import com.dinesh.orderdelivery.restaurant.repository.RestaurantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

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
    void registerLoginAndReadCurrentUser() throws Exception {
        register("Customer One", "customer@example.com", Role.CUSTOMER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.role", is("CUSTOMER")));

        String token = login("customer@example.com");

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("customer@example.com")))
                .andExpect(jsonPath("$.data.role", is("CUSTOMER")));
    }

    @Test
    void adminEndpointRequiresAdminRole() throws Exception {
        register("Customer One", "customer@example.com", Role.CUSTOMER);
        String customerToken = login("customer@example.com");

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        register("Admin One", "admin@example.com", Role.ADMIN);
        String adminToken = login("admin@example.com");

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@example.com", "Password123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void duplicateRegistrationReturnsBadRequest() throws Exception {
        register("Admin One", "admin@example.com", Role.ADMIN)
                .andExpect(status().isCreated());

        register("Admin Copy", "ADMIN@example.com", Role.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Email is already registered")));
    }

    private ResultActionsAdapter register(String name, String email, Role role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(name, email, "Password123!", role))))
                .andReturn();
        return new ResultActionsAdapter(result);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private final class ResultActionsAdapter {
        private final MvcResult result;

        private ResultActionsAdapter(MvcResult result) {
            this.result = result;
        }

        private ResultActionsAdapter andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            matcher.match(result);
            return this;
        }
    }
}
