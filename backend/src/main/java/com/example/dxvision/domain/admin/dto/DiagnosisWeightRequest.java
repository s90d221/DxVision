package com.example.dxvision.domain.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DiagnosisWeightRequest(
        @NotNull(message = "diagnosisId is required")
        Long diagnosisId,
        @NotNull(message = "weight is required")
        @Positive(message = "weight must be greater than 0")
        Double weight
) {
}
