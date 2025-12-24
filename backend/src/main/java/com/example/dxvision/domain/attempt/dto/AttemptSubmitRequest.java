package com.example.dxvision.domain.attempt.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AttemptSubmitRequest(
        @NotNull Long caseId,
        @NotNull Long caseVersion,
        @NotNull List<Long> findingIds,
        @NotNull List<Long> diagnosisIds,
        @NotNull Double clickX,
        @NotNull Double clickY
) {
}
