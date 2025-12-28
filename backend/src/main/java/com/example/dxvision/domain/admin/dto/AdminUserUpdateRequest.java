package com.example.dxvision.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserUpdateRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
