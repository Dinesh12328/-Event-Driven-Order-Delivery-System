package com.dinesh.orderdelivery.auth.controller;

import com.dinesh.orderdelivery.auth.dto.UserResponse;
import com.dinesh.orderdelivery.auth.service.AuthService;
import com.dinesh.orderdelivery.common.api.ApiResponse;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> me(Principal principal) {
        return ApiResponse.success("Current user", authService.currentUser(principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<UserResponse>> users() {
        return ApiResponse.success("Users", authService.users());
    }
}

