package com.example.dxvision.domain.dashboard.dto;

import com.example.dxvision.domain.progress.UserCaseStatus;
import java.time.Instant;

public record DashboardCaseItemResponse(
        Long caseId,
        String title,
        UserCaseStatus status,
        Instant lastAttemptAt,
        Double lastScore
) {
}
