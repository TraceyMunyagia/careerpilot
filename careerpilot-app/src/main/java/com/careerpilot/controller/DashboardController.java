package com.careerpilot.controller;

import com.careerpilot.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

        @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.currentStats());
        model.addAttribute("upcoming", dashboardService.upcomingInterviews());
        model.addAttribute("overdue", dashboardService.overdueFollowUps());
        return "dashboard";
    }
}