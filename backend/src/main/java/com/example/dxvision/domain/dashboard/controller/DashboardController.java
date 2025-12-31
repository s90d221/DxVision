package com.example.dxvision.domain.dashboard.controller;

import com.example.dxvision.domain.dashboard.dto.DashboardActivityResponse;
import com.example.dxvision.domain.dashboard.dto.DashboardCaseItemResponse;
import com.example.dxvision.domain.dashboard.dto.DashboardSummaryResponse;
import com.example.dxvision.domain.dashboard.service.DashboardService;
import com.example.dxvision.domain.progress.UserCaseStatus;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/cases")
    public List<DashboardCaseItemResponse> getCases(@RequestParam("status") UserCaseStatus status) {
        return dashboardService.getCases(status);
    }

    @GetMapping("/activity")
    public DashboardActivityResponse getActivity(@RequestParam(value = "days", defaultValue = "30") int days) {
        return dashboardService.getActivity(days);
    }
}
