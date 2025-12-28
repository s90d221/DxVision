package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.progress.UserCaseStatus;
import java.time.Instant;

public record AdminUserActivityDto(
        Instant timestamp,
        Long caseId,
        String caseTitle,
        UserCaseStatus status,
        double score
) {
}
