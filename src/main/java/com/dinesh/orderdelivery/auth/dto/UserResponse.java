package com.dinesh.orderdelivery.auth.dto;

import com.dinesh.orderdelivery.auth.domain.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        Instant createdAt
) {
}

