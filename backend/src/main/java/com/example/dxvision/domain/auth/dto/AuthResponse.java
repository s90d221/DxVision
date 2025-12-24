package com.example.dxvision.domain.auth.dto;

public record AuthResponse(
        String token,
        UserInfoResponse user
) {
}
