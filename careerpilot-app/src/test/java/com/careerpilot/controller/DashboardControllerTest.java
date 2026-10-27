package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.dto.ApplicationStats;
import com.careerpilot.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardController.class)
@EnableConfigurationProperties(AppProperties.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void emptyDashboardShowsTheInfoAlert() throws Exception {
        when(dashboardService.currentStats()).thenReturn(ApplicationStats.empty());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("No applications yet")));
    }

    @Test
    void dashboardWithDataHidesTheAlert() throws Exception {
        ApplicationStats stats = new ApplicationStats(10, 2, 4, 2, 1, 1);
        when(dashboardService.currentStats()).thenReturn(stats);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("stats", stats))
                .andExpect(content().string(not(containsString("No applications yet"))))
                .andExpect(content().string(containsString("of 8 submitted")));
    }
}