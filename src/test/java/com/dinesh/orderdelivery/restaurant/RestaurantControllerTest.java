package com.dinesh.orderdelivery.restaurant;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.dto.LoginRequest;
import com.dinesh.orderdelivery.auth.dto.RegisterRequest;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemRequest;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantRequest;
import com.dinesh.orderdelivery.restaurant.repository.MenuItemRepository;
import com.dinesh.orderdelivery.restaurant.repository.RestaurantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void ownerCanManageRestaurantAndPublicCanBrowseMenu() throws Exception {
        String ownerToken = tokenFor("Owner One", "owner@example.com", Role.RESTAURANT_OWNER);

        MvcResult restaurantResult = mockMvc.perform(post("/api/restaurants")
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
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("Dinesh Biryani House")))
                .andReturn();
        String restaurantId = JsonPath.read(restaurantResult.getResponse().getContentAsString(), "$.data.id");

        MvcResult itemResult = mockMvc.perform(post("/api/restaurants/{restaurantId}/menu", restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuItemRequest(
                                "Chicken Biryani",
                                "Slow cooked biryani with raita",
                                new BigDecimal("249.00"),
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name", is("Chicken Biryani")))
                .andReturn();
        String itemId = JsonPath.read(itemResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/restaurants")
                        .queryParam("query", "biryani")
                        .queryParam("location", "Hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].cuisine", is("Indian")));

        mockMvc.perform(get("/api/restaurants/{restaurantId}/menu", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].price", is(249.00)));

        mockMvc.perform(put("/api/restaurants/{restaurantId}/menu/{itemId}", restaurantId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuItemRequest(
                                "Paneer Biryani",
                                "Vegetarian biryani with paneer",
                                new BigDecimal("219.00"),
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Paneer Biryani")));

        mockMvc.perform(put("/api/restaurants/{restaurantId}", restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestaurantRequest(
                                "Dinesh Spice Kitchen",
                                "Indian",
                                "Hyderabad",
                                "Hitech City Road",
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Dinesh Spice Kitchen")));

        mockMvc.perform(delete("/api/restaurants/{restaurantId}/menu/{itemId}", restaurantId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/restaurants/{restaurantId}", restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void customerCannotCreateRestaurant() throws Exception {
        String customerToken = tokenFor("Customer One", "customer@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/restaurants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestaurantRequest(
                                "Blocked Cafe",
                                "Cafe",
                                "Bengaluru",
                                "MG Road",
                                true
                        ))))
                .andExpect(status().isForbidden());
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

