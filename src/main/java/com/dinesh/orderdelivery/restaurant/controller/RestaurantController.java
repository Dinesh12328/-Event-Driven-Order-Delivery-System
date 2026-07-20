package com.dinesh.orderdelivery.restaurant.controller;

import com.dinesh.orderdelivery.common.api.ApiResponse;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemRequest;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemResponse;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantRequest;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantResponse;
import com.dinesh.orderdelivery.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    ApiResponse<List<RestaurantResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String location
    ) {
        return ApiResponse.success("Restaurants", restaurantService.search(query, cuisine, location));
    }

    @GetMapping("/{restaurantId}")
    ApiResponse<RestaurantResponse> get(@PathVariable UUID restaurantId) {
        return ApiResponse.success("Restaurant", restaurantService.get(restaurantId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ResponseEntity<ApiResponse<RestaurantResponse>> create(
            @Valid @RequestBody RestaurantRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created", restaurantService.create(request, principal.getName())));
    }

    @PutMapping("/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ApiResponse<RestaurantResponse> update(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Restaurant updated", restaurantService.update(restaurantId, request, principal.getName()));
    }

    @DeleteMapping("/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ResponseEntity<Void> delete(@PathVariable UUID restaurantId, Principal principal) {
        restaurantService.delete(restaurantId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{restaurantId}/menu")
    ApiResponse<List<MenuItemResponse>> menu(@PathVariable UUID restaurantId) {
        return ApiResponse.success("Menu", restaurantService.menu(restaurantId));
    }

    @PostMapping("/{restaurantId}/menu")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody MenuItemRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created", restaurantService.addMenuItem(restaurantId, request, principal.getName())));
    }

    @PutMapping("/{restaurantId}/menu/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ApiResponse<MenuItemResponse> updateMenuItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @Valid @RequestBody MenuItemRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Menu item updated", restaurantService.updateMenuItem(restaurantId, itemId, request, principal.getName()));
    }

    @DeleteMapping("/{restaurantId}/menu/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    ResponseEntity<Void> deleteMenuItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            Principal principal
    ) {
        restaurantService.deleteMenuItem(restaurantId, itemId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}

