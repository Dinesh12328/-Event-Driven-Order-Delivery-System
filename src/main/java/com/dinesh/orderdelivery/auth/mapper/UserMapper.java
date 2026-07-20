package com.dinesh.orderdelivery.auth.mapper;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.auth.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

