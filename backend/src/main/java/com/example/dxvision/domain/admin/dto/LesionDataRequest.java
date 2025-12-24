package com.example.dxvision.domain.admin.dto;

import jakarta.validation.constraints.NotNull;

public record LesionDataRequest(
        @NotNull(message = "cx is required")
        Double cx,
        @NotNull(message = "cy is required")
        Double cy,
        @NotNull(message = "r is required")
        Double r
) {
}
