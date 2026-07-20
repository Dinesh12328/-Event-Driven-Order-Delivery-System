package com.dinesh.orderdelivery.admin.controller;

import com.dinesh.orderdelivery.admin.dto.AdminDashboardResponse;
import com.dinesh.orderdelivery.admin.service.AdminDashboardService;
import com.dinesh.orderdelivery.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.success("Admin dashboard", dashboardService.dashboard());
    }
}

