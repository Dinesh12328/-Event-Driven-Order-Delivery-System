package com.dinesh.orderdelivery.event.controller;

import com.dinesh.orderdelivery.common.api.ApiResponse;
import com.dinesh.orderdelivery.event.dto.IntegrationEventResponse;
import com.dinesh.orderdelivery.event.service.EventLogService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final EventLogService eventLogService;

    public AdminEventController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<IntegrationEventResponse>> events() {
        return ApiResponse.success("Integration events", eventLogService.events());
    }
}

