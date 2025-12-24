package com.example.dxvision.domain.attempt.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AttemptSubmitRequest(
        @NotNull Long caseId,
        @NotNull Long caseVersion,
        @NotNull @Size(min = 0) List<Long> findingIds,
        @NotNull @Size(min = 0) List<Long> diagnosisIds,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") Double clickX,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") Double clickY
) {
}
