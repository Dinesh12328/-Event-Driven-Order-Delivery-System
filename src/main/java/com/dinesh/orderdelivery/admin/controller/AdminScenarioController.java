package com.dinesh.orderdelivery.admin.controller;

import com.dinesh.orderdelivery.admin.dto.AdminScenarioResponse;
import com.dinesh.orderdelivery.admin.service.AdminScenarioService;
import com.dinesh.orderdelivery.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/scenarios")
public class AdminScenarioController {

    private final AdminScenarioService scenarioService;

    public AdminScenarioController(AdminScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping("/order-journey")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<AdminScenarioResponse> runOrderJourney() {
        return ApiResponse.success("Order journey completed", scenarioService.runOrderJourney());
    }
}
