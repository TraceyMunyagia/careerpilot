package com.careerpilot.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationStatsTest {

    @Test
    void ratesAreZeroWhenNothingWasSubmitted() {
        ApplicationStats stats = new ApplicationStats(2, 2, 0, 0, 0, 0);

        assertEquals(0, stats.submitted());
        assertEquals(0.0, stats.interviewRate());
        assertEquals(0.0, stats.offerRate());
    }

    @Test
    void ratesAreBasedOnSubmittedApplications() {
        // 10 total: 2 saved, 4 applied, 2 interview, 1 offer, 1 rejected
        ApplicationStats stats = new ApplicationStats(10, 2, 4, 2, 1, 1);

        assertEquals(8, stats.submitted());
        assertEquals(6, stats.active());
        assertEquals(37.5, stats.interviewRate(), 0.001); // (2 + 1) / 8
        assertEquals(12.5, stats.offerRate(), 0.001);     // 1 / 8
    }
}