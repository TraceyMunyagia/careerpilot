package com.careerpilot.service;

import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.exception.BusinessRuleException;
import com.careerpilot.exception.InvalidStatusTransitionException;
import com.careerpilot.model.ApplicationStats;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import com.careerpilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobApplicationServiceTest {

    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(NAIROBI).toInstant(), NAIROBI);
        service = new JobApplicationService(new JobApplicationRepository(), fixedClock);
    }

    // ---------- helpers ----------

    private JobApplication applied(String company, LocalDate on) {
        return service.addApplication(company, "Java Developer", "Nairobi", null, null, on);
    }

    private JobApplication applied(String company) {
        return applied(company, TODAY.minusDays(5));
    }

    private JobApplication saved(String company) {
        return service.addApplication(company, "Java Developer", "Nairobi", null, null, null);
    }

    // ---------- tests ----------

    @Nested
    class AddingApplications {

        @Test
        void withADateStartsAsApplied() {
            JobApplication app = applied("Safaricom");

            assertNotNull(app.getId());
            assertEquals(Status.APPLIED, app.getStatus());
        }

        @Test
        void withoutADateStartsAsSaved() {
            JobApplication app = saved("Andela");

            assertEquals(Status.SAVED, app.getStatus());
            assertNull(app.getApplicationDate());
        }
    }

    @Nested
    class ChangingStatus {

        @Test
        void savedToAppliedStampsTodayAsTheApplicationDate() {
            JobApplication app = saved("Andela");

            service.changeStatus(app.getId(), Status.APPLIED);

            assertEquals(TODAY, app.getApplicationDate());
        }

        @Test
        void movingToTheSameStatusIsRejected() {
            JobApplication app = applied("Safaricom");

            assertThrows(InvalidStatusTransitionException.class,
                    () -> service.changeStatus(app.getId(), Status.APPLIED));
        }

        @Test
        void closedApplicationsCannotMove() {
            JobApplication app = applied("Cellulant");
            service.changeStatus(app.getId(), Status.REJECTED);

            InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class,
                    () -> service.changeStatus(app.getId(), Status.OFFER));

            assertEquals(Status.REJECTED, ex.getFrom());
            assertEquals(Status.OFFER, ex.getTo());
        }
    }

    @Nested
    class Interviews {

        @Test
        void schedulingMovesAppliedToInterview() {
            JobApplication app = applied("Safaricom");

            service.scheduleInterview(app.getId(), TODAY.plusDays(3));

            assertEquals(Status.INTERVIEW, app.getStatus());
            assertEquals(TODAY.plusDays(3), app.getInterviewDate());
        }

        @Test
        void todayIsAllowedButYesterdayIsNot() {
            JobApplication app = applied("Safaricom");

            assertDoesNotThrow(() -> service.scheduleInterview(app.getId(), TODAY));
            assertThrows(BusinessRuleException.class,
                    () -> service.scheduleInterview(app.getId(), TODAY.minusDays(1)));
        }

        @Test
        void savedApplicationsCannotBeScheduled() {
            JobApplication app = saved("Andela");

            assertThrows(BusinessRuleException.class,
                    () -> service.scheduleInterview(app.getId(), TODAY.plusDays(1)));
        }

        @Test
        void nextInterviewIsTheSoonest() {
            JobApplication later = applied("Andela");
            JobApplication sooner = applied("Safaricom");
            service.scheduleInterview(later.getId(), TODAY.plusDays(10));
            service.scheduleInterview(sooner.getId(), TODAY.plusDays(2));

            assertEquals("Safaricom", service.nextInterview(TODAY).orElseThrow().getCompanyName());
        }

        @Test
        void nextInterviewIsEmptyWhenNothingIsScheduled() {
            applied("Safaricom");

            assertTrue(service.nextInterview(TODAY).isEmpty());
        }
    }

    @Nested
    class MissingApplications {

        @Test
        void unknownIdThrowsAndCarriesTheId() {
            ApplicationNotFoundException ex = assertThrows(ApplicationNotFoundException.class,
                    () -> service.getApplication(99));

            assertEquals(99, ex.getApplicationId());
        }

        @Test
        void findApplicationReturnsEmptyInsteadOfThrowing() {
            assertTrue(service.findApplication(99).isEmpty());
        }

        @Test
        void deletingAnUnknownIdThrows() {
            assertThrows(ApplicationNotFoundException.class, () -> service.deleteApplication(42));
        }
    }

    @Nested
    class Notes {

        @Test
        void notesAreStampedWithTheClockDate() {
            JobApplication app = applied("Safaricom");

            service.addNote(app.getId(), "Called the recruiter");

            assertEquals("[2026-11-02] Called the recruiter", app.getNotes());
        }
    }

    @Nested
    class DashboardNumbers {

        @Test
        void followUpIsOverdueOnlyAfterItsDate() {
            JobApplication late = applied("Andela");
            JobApplication dueToday = applied("Safaricom");
            service.setFollowUp(late.getId(), TODAY.minusDays(1));
            service.setFollowUp(dueToday.getId(), TODAY);

            List<JobApplication> overdue = service.overdueFollowUps(TODAY);

            assertEquals(1, overdue.size());
            assertEquals("Andela", overdue.get(0).getCompanyName());
        }

        @Test
        void statsCountStatusesAndRates() {
            applied("Safaricom");
            JobApplication interviewing = applied("Andela");
            JobApplication offered = applied("Cellulant");
            saved("Twiga Foods");
            service.scheduleInterview(interviewing.getId(), TODAY.plusDays(1));
            service.changeStatus(offered.getId(), Status.OFFER);

            ApplicationStats stats = service.stats();

            assertEquals(4, stats.total());
            assertEquals(3, stats.submitted());
            assertEquals(2, stats.active());
            assertEquals(66.67, stats.interviewRate(), 0.01);
            assertEquals(33.33, stats.offerRate(), 0.01);
        }

        @Test
        void applicationsAreGroupedByMonthOldestFirst() {
            applied("A", LocalDate.of(2026, 10, 5));
            applied("B", LocalDate.of(2026, 10, 20));
            applied("C", LocalDate.of(2026, 9, 1));
            saved("D");

            Map<YearMonth, Long> perMonth = service.applicationsPerMonth();

            assertEquals(List.of(YearMonth.of(2026, 9), YearMonth.of(2026, 10)), List.copyOf(perMonth.keySet()));
            assertEquals(2L, perMonth.get(YearMonth.of(2026, 10)));
        }

        @Test
        void averageWaitCoversActiveApplicationsOnly() {
            applied("A", TODAY.minusDays(10));
            applied("B", TODAY.minusDays(20));
            JobApplication rejected = applied("C", TODAY.minusDays(100));
            service.changeStatus(rejected.getId(), Status.REJECTED);

            assertEquals(15.0, service.averageDaysWaiting(TODAY).orElseThrow(), 0.001);
        }

        @Test
        void companiesAreListedOnceAlphabetically() {
            applied("safaricom");
            applied("Andela");
            saved("Andela");

            assertEquals(List.of("Andela", "safaricom"), service.companiesAppliedTo());
        }
    }
}