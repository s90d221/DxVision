package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.auth.Role;
import com.example.dxvision.domain.auth.UserStatus;
import java.time.Instant;

public record AdminUserListItem(
        Long id,
        String email,
        String name,
        Role role,
        UserStatus status,
        Instant createdAt,
        AdminUserStats stats
) {
}
