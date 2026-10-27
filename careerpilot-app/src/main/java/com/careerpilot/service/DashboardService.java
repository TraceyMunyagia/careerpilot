package com.careerpilot.service;

import com.careerpilot.dto.ApplicationStats;
import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.dto.StatusCount;
import com.careerpilot.model.Status;
import com.careerpilot.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** Derived from Status.isClosed(), so adding a new closed status updates this automatically. */
    private static final Set<Status> CLOSED_STATUSES = EnumSet.copyOf(
            Arrays.stream(Status.values()).filter(Status::isClosed).toList());

    private final JobApplicationRepository repository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public DashboardService(JobApplicationRepository repository,
                            CurrentUserService currentUserService,
                            Clock clock) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    public ApplicationStats currentStats() {
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            counts.put(status, 0L);
        }
        for (StatusCount row : repository.countByStatus(currentUserService.currentUserId())) {
            counts.put(row.getStatus(), row.getTotal());
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        return new ApplicationStats(
                total,
                counts.get(Status.SAVED),
                counts.get(Status.APPLIED),
                counts.get(Status.INTERVIEW),
                counts.get(Status.OFFER),
                counts.get(Status.REJECTED));
    }

    public List<ApplicationSummary> upcomingInterviews() {
        return repository.findUpcomingInterviews(currentUserService.currentUserId(), today())
                .stream()
                .map(ApplicationSummary::from)
                .toList();
    }

    public List<ApplicationSummary> overdueFollowUps() {
        return repository.findOverdueFollowUps(currentUserService.currentUserId(), today(), CLOSED_STATUSES)
                .stream()
                .map(ApplicationSummary::from)
                .toList();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}