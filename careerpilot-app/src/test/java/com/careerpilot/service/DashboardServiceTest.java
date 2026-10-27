package com.careerpilot.service;

import com.careerpilot.dto.ApplicationStats;
import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.dto.StatusCount;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import com.careerpilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A pure unit test: no Spring, no database. Mockito plays the repository. */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final long USER_ID = 7L;

    @Mock
    private JobApplicationRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    private DashboardService service;

    /** A tiny implementation of the projection interface, for test data. */
    private record Count(Status status, Long total) implements StatusCount {
        @Override public Status getStatus() { return status; }
        @Override public Long getTotal() { return total; }
    }

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(NAIROBI).toInstant(), NAIROBI);
        service = new DashboardService(repository, currentUserService, clock);
        when(currentUserService.currentUserId()).thenReturn(USER_ID);
    }

    @Test
    void statsFillMissingStatusesWithZero() {
        when(repository.countByStatus(USER_ID)).thenReturn(List.of(
                new Count(Status.APPLIED, 3L),
                new Count(Status.OFFER, 1L)));

        ApplicationStats stats = service.currentStats();

        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.saved()).isZero();
        assertThat(stats.applied()).isEqualTo(3);
        assertThat(stats.offerRate()).isEqualTo(25.0);
    }

    @Test
    void upcomingInterviewsAreAskedForWithTheClockDate() {
        JobApplication app = new JobApplication("Safaricom", "Java Developer", "Nairobi", TODAY.minusDays(5));
        app.setInterviewDate(TODAY.plusDays(2));
        when(repository.findUpcomingInterviews(USER_ID, TODAY)).thenReturn(List.of(app));

        List<ApplicationSummary> upcoming = service.upcomingInterviews();

        assertThat(upcoming).extracting(ApplicationSummary::companyName).containsExactly("Safaricom");
    }

    @Test
    void overdueFollowUpsExcludeClosedStatuses() {
        service.overdueFollowUps();

        verify(repository).findOverdueFollowUps(USER_ID, TODAY, EnumSet.of(Status.REJECTED, Status.OFFER));
    }
}