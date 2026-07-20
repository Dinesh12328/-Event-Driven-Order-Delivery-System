package com.dinesh.orderdelivery.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UserResponse user
) {
}

