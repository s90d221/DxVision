package com.example.dxvision.domain.dashboard.dto;

import java.util.List;

public record DashboardActivityResponse(
        List<DashboardActivityDay> days,
        long totalSolved,
        int streak
) {
    public record DashboardActivityDay(String date, long solvedCount) {
    }
}
