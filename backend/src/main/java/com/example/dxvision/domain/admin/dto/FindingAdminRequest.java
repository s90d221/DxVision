package com.example.dxvision.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record FindingAdminRequest(
        @NotBlank(message = "Label is required")
        String label,
        String description,
        Long folderId,
        Integer orderIndex
) {
}
