package com.example.dxvision.domain.dashboard.dto;

public record DashboardSummaryResponse(
        long correctCount,
        long wrongCount,
        long reattemptCorrectCount,
        int xp,
        int level,
        int streak,
        double correctThreshold
) {
}
