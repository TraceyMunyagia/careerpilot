package com.careerpilot.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobApplicationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private JobApplication app;

    @BeforeEach
    void setUp() {
        // Runs before EVERY test, so each test gets a fresh object
        app = new JobApplication("Safaricom", "Java Developer", "Nairobi", null);
    }

    @Test
    void newApplicationStartsAsSaved() {
        assertEquals(Status.SAVED, app.getStatus());
    }

    @Test
    void companyNameIsTrimmed() {
        JobApplication trimmed = new JobApplication("  Andela  ", "Engineer", null, null);
        assertEquals("Andela", trimmed.getCompanyName());
    }

    @Test
    void blankJobTitleThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new JobApplication("Andela", " ", null, null));
    }

    @Test
    void nullStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> app.setStatus(null));
    }

    @Test
    void interviewTodayCountsAsUpcoming() {
        app.setInterviewDate(TODAY);
        assertTrue(app.hasUpcomingInterview(TODAY));
    }

    @Test
    void pastInterviewIsNotUpcoming() {
        app.setInterviewDate(TODAY.minusDays(1));
        assertFalse(app.hasUpcomingInterview(TODAY));
    }

    @Test
    void overdueFollowUpIsIgnoredOnceClosed() {
        app.setFollowUpDate(TODAY.minusDays(3));
        assertTrue(app.isFollowUpOverdue(TODAY));

        app.setStatus(Status.REJECTED);
        assertFalse(app.isFollowUpOverdue(TODAY));
    }

    @Test
    void unsavedApplicationsAreNeverEqual() {
        JobApplication other = new JobApplication("Safaricom", "Java Developer", "Nairobi", null);
        assertNotEquals(app, other);
    }

    @Test
    void applicationsWithTheSameIdAreEqual() {
        JobApplication other = new JobApplication("Different", "Title", null, null);
        app.setId(7L);
        other.setId(7L);
        assertEquals(app, other);
    }
}