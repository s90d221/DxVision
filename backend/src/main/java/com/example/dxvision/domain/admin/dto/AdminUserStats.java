package com.example.dxvision.domain.admin.dto;

import java.time.Instant;

public record AdminUserStats(
        long attemptedCount,
        long correctCount,
        long wrongCount,
        long reattemptCorrectCount,
        Instant lastActiveAt
) {
}
