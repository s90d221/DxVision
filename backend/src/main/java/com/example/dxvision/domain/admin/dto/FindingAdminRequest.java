package com.example.dxvision.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record FindingAdminRequest(
        @NotBlank(message = "Label is required")
        String label,
        String description,
        List<Long> folderIds
) {
}
