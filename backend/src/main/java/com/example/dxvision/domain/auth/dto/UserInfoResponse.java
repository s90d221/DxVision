package com.example.dxvision.domain.auth.dto;

import com.example.dxvision.domain.auth.Role;

public record UserInfoResponse(
        Long id,
        String email,
        String name,
        Role role
) {
}
