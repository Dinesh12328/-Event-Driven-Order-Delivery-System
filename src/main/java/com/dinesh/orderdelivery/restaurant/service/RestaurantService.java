package com.dinesh.orderdelivery.restaurant.service;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.auth.domain.Role;
import com.dinesh.orderdelivery.auth.repository.UserRepository;
import com.dinesh.orderdelivery.common.error.ResourceNotFoundException;
import com.dinesh.orderdelivery.restaurant.domain.MenuItem;
import com.dinesh.orderdelivery.restaurant.domain.Restaurant;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemRequest;
import com.dinesh.orderdelivery.restaurant.dto.MenuItemResponse;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantRequest;
import com.dinesh.orderdelivery.restaurant.dto.RestaurantResponse;
import com.dinesh.orderdelivery.restaurant.mapper.RestaurantMapper;
import com.dinesh.orderdelivery.restaurant.repository.MenuItemRepository;
import com.dinesh.orderdelivery.restaurant.repository.RestaurantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final RestaurantMapper mapper;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            UserRepository userRepository,
            RestaurantMapper mapper
    ) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    @CacheEvict(value = {"restaurants", "menus"}, allEntries = true)
    public RestaurantResponse create(RestaurantRequest request, String ownerEmail) {
        AppUser owner = userRepository.findByEmailIgnoreCase(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        Restaurant restaurant = restaurantRepository.save(new Restaurant(
                owner,
                request.name().trim(),
                request.cuisine().trim(),
                request.location().trim(),
                request.address().trim()
        ));
        return mapper.toResponse(restaurant);
    }

    @Transactional
    @CacheEvict(value = {"restaurants", "menus"}, allEntries = true)
    public RestaurantResponse update(UUID restaurantId, RestaurantRequest request, String ownerEmail) {
        Restaurant restaurant = findRestaurant(restaurantId);
        assertOwnerOrAdmin(restaurant, ownerEmail);
        restaurant.update(
                request.name().trim(),
                request.cuisine().trim(),
                request.location().trim(),
                request.address().trim(),
                request.active() == null || request.active()
        );
        return mapper.toResponse(restaurant);
    }

    @Transactional
    @CacheEvict(value = {"restaurants", "menus"}, allEntries = true)
    public void delete(UUID restaurantId, String ownerEmail) {
        Restaurant restaurant = findRestaurant(restaurantId);
        assertOwnerOrAdmin(restaurant, ownerEmail);
        restaurantRepository.delete(restaurant);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "restaurants", key = "'search:' + #query + ':' + #cuisine + ':' + #location")
    public List<RestaurantResponse> search(String query, String cuisine, String location) {
        return restaurantRepository.search(clean(query), clean(cuisine), clean(location))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse get(UUID restaurantId) {
        Restaurant restaurant = findRestaurant(restaurantId);
        List<MenuItemResponse> menu = menu(restaurantId);
        return mapper.toResponse(restaurant, menu);
    }

    @Transactional
    @CacheEvict(value = {"restaurants", "menus"}, allEntries = true)
    public MenuItemResponse addMenuItem(UUID restaurantId, MenuItemRequest request, String ownerEmail) {
        Restaurant restaurant = findRestaurant(restaurantId);
        assertOwnerOrAdmin(restaurant, ownerEmail);
        MenuItem item = menuItemRepository.save(new MenuItem(
                restaurant,
                request.name().trim(),
                request.description().trim(),
                request.price(),
                request.available() == null || request.available()
        ));
        return mapper.toResponse(item);
    }

    @Transactional
    @CacheEvict(value = {"restaurants", "menus"}, allEntries = true)
    public MenuItemResponse updateMenuItem(UUID restaurantId, UUID itemId, MenuItemRequest request, String ownerEmail) {
        Restaurant restaurant = findRestaurant(restaurantId);
        assertOwnerOrAdmin(restaurant, ownerEmail);
        MenuItem item = findMenuItem(itemId);
        if (!item.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Menu item does not belong to restaurant");
        }
        item.update(
                request.name().trim(),
                request.description().trim(),
                request.price(),
                request.available() == null || request.available()
        );
        return mapper.toResponse(item);
    }

    @Transactional
    @CacheEvict(value = {"restaurants", "menus"}, allEntries = true)
    public void deleteMenuItem(UUID restaurantId, UUID itemId, String ownerEmail) {
        Restaurant restaurant = findRestaurant(restaurantId);
        assertOwnerOrAdmin(restaurant, ownerEmail);
        MenuItem item = findMenuItem(itemId);
        if (!item.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Menu item does not belong to restaurant");
        }
        menuItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "menus", key = "#restaurantId")
    public List<MenuItemResponse> menu(UUID restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant not found");
        }
        return menuItemRepository.findByRestaurantIdAndAvailableTrueOrderByName(restaurantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private Restaurant findRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
    }

    private MenuItem findMenuItem(UUID itemId) {
        return menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private void assertOwnerOrAdmin(Restaurant restaurant, String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN || restaurant.getOwner().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("Only the restaurant owner or admin can change this restaurant");
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

