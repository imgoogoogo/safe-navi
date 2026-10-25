package com.safenavi.backend.user;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, String name, String role, String status, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getStatus(),
                user.getCreatedAt());
    }
}
