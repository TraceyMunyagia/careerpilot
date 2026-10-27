package com.careerpilot.service;

import com.careerpilot.dto.ApplicationStats;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    /** Placeholder: returns zeros until Week 4 stores applications in PostgreSQL. */
    public ApplicationStats currentStats() {
        return ApplicationStats.empty();
    }
}