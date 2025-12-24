package com.example.dxvision.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record DiagnosisAdminRequest(
        @NotBlank(message = "Name is required")
        String name,
        String description
) {
}
