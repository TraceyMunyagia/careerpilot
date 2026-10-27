package com.careerpilot.service;

import com.careerpilot.model.JobApplication;
import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.exception.BusinessRuleException;
import com.careerpilot.exception.InvalidStatusTransitionException;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.repository.JobApplicationRepository;
import com.careerpilot.model.ApplicationStats;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business rules live here, not in the UI and not in the repository.
 * In Week 3 this class gets @Service and Spring injects the repository.
 */
public class JobApplicationService {

        private final JobApplicationRepository repository;
    private final Clock clock;

    /** Production constructor: uses the real system clock. Main.java keeps calling this one. */
    public JobApplicationService(JobApplicationRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    /** Test constructor: pass a fixed Clock so "today" never changes. */
    public JobApplicationService(JobApplicationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    // ---------- create / read ----------

    /** If appliedOn is given, the application starts as APPLIED; otherwise as SAVED. */
    public JobApplication addApplication(String companyName, String jobTitle, String location,
                                         String applicationLink, String jobDescription, LocalDate appliedOn) {
        JobApplication app = new JobApplication(companyName, jobTitle, location, appliedOn);
        app.setApplicationLink(applicationLink);
        app.setJobDescription(jobDescription);
        if (appliedOn != null) {
            app.setStatus(Status.APPLIED);
        }
        return repository.save(app);
    }

    public JobApplication getApplication(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    /** For callers that want to handle "missing" themselves instead of catching an exception. */
    public Optional<JobApplication> findApplication(long id) {
        return repository.findById(id);
    }

    public List<JobApplication> listAll() {
        return repository.findAllSortedByDate();
    }

    public List<JobApplication> listByStatus(Status status) {
        return repository.findByStatus(status);
    }

    public List<JobApplication> searchByCompany(String text) {
        return repository.findByCompanyContaining(text);
    }

    // ---------- updates ----------

    public JobApplication changeStatus(long id, Status newStatus) {
        JobApplication app = getApplication(id);
        Status current = app.getStatus();
        if (current == newStatus || !current.canMoveTo(newStatus)) {
            throw new InvalidStatusTransitionException(current, newStatus);
        }
        app.setStatus(newStatus);
        if (newStatus == Status.APPLIED && app.getApplicationDate() == null) {
                        app.setApplicationDate(today());
        }
        return repository.save(app);
    }

    /** Scheduling an interview moves APPLIED to INTERVIEW automatically. */
    public JobApplication scheduleInterview(long id, LocalDate date) {
                if (date.isBefore(today())) {
            throw new BusinessRuleException("Interview date cannot be in the past");
        }
        JobApplication app = getApplication(id);
        Status current = app.getStatus();
        if (current == Status.APPLIED) {
            app.setStatus(Status.INTERVIEW);
        } else if (current != Status.INTERVIEW) {
            throw new BusinessRuleException("Can only schedule interviews for Applied or Interview applications (current: "
                    + current.getLabel() + ")");
        }
        app.setInterviewDate(date);
        return repository.save(app);
    }

    /** Closed applications (Rejected or Offer) have nothing left to follow up on. */
    public JobApplication setFollowUp(long id, LocalDate date) {
        JobApplication app = getApplication(id);
        if (app.getStatus().isClosed()) {
            throw new BusinessRuleException("Cannot set a follow-up on a closed application (current: "
                    + app.getStatus().getLabel() + ")");
        }
        app.setFollowUpDate(date);
        return repository.save(app);
    }

    public JobApplication addNote(long id, String note) {
        JobApplication app = getApplication(id);
        app.addNote(note, today());
        return repository.save(app);
    }

    public JobApplication setRecruiter(long id, RecruiterContact recruiter) {
        JobApplication app = getApplication(id);
        app.setRecruiter(recruiter);
        return repository.save(app);
    }

    public void deleteApplication(long id) {
        if (!repository.deleteById(id)) {
            throw new ApplicationNotFoundException(id);
        }
    }

    // ---------- dashboard numbers ----------

    public int totalCount() {
        return repository.count();
    }

      public long activeCount() {
        return repository.findAll().stream()
                .filter(app -> app.getStatus().isActive())
                .count();
    }

        public Map<Status, Long> statusSummary() {
        return repository.countByStatus();
    }

public List<JobApplication> upcomingInterviews(LocalDate today) {
        return repository.findAll().stream()
                .filter(app -> app.hasUpcomingInterview(today))
                .sorted(Comparator.comparing(JobApplication::getInterviewDate))
                .toList();
    }

 public List<JobApplication> overdueFollowUps(LocalDate today) {
        return repository.findAll().stream()
                .filter(app -> app.isFollowUpOverdue(today))
                .sorted(Comparator.comparing(JobApplication::getFollowUpDate))
                .toList();
    }


    /** The soonest upcoming interview, if there is one. */
  public Optional<JobApplication> nextInterview(LocalDate today) {
        return repository.findAll().stream()
                .filter(app -> app.hasUpcomingInterview(today))
                .min(Comparator.comparing(JobApplication::getInterviewDate));
    }

    // ---------- analytics ----------

    public ApplicationStats stats() {
        Map<Status, Long> counts = repository.countByStatus();
        return new ApplicationStats(
                repository.count(),
                counts.get(Status.SAVED),
                counts.get(Status.APPLIED),
                counts.get(Status.INTERVIEW),
                counts.get(Status.OFFER),
                counts.get(Status.REJECTED));
    }

    /** Applications per month, oldest month first. Saved-only jobs (no date) are skipped. */
    public Map<YearMonth, Long> applicationsPerMonth() {
        return repository.findAll().stream()
                .filter(app -> app.getApplicationDate() != null)
                .collect(Collectors.groupingBy(
                        app -> YearMonth.from(app.getApplicationDate()),
                        TreeMap::new,
                        Collectors.counting()));
    }

    /** Each company once, A–Z. */
    public List<String> companiesAppliedTo() {
        return repository.findAll().stream()
                .map(JobApplication::getCompanyName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /** Average days since applying, across active applications only. Empty if there are none. */
    public OptionalDouble averageDaysWaiting(LocalDate today) {
        return repository.findAll().stream()
                .filter(app -> app.getStatus().isActive())
                .filter(app -> app.getApplicationDate() != null)
                .mapToLong(app -> ChronoUnit.DAYS.between(app.getApplicationDate(), today))
                .average();
    }
}