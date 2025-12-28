package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.progress.UserCaseStatus;
import java.time.Instant;

public record AdminUserCaseProgressDto(
        Long caseId,
        String caseTitle,
        UserCaseStatus status,
        int attemptCount,
        Instant lastAttemptAt
) {
}
