package com.dinesh.orderdelivery.health;

import com.dinesh.orderdelivery.common.api.ApiResponse;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;

    public SystemHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    ApiResponse<Map<String, Object>> health() {
        Integer databasePing = jdbcTemplate.queryForObject("select 1", Integer.class);
        return ApiResponse.success("Backend is running", Map.of(
                "application", "UP",
                "database", databasePing != null && databasePing == 1 ? "UP" : "UNKNOWN"
        ));
    }
}

