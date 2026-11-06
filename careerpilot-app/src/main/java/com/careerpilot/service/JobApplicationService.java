package com.careerpilot.service;

import com.careerpilot.dto.ApplicationForm;
import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import com.careerpilot.model.User;
import com.careerpilot.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
/**
 * Class-level readOnly = true is the default for every method;
 * write methods override it with a plain @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class JobApplicationService {

       /** "Thu 19 Nov 2026": used in notes and on-screen messages. */
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);

    private final JobApplicationRepository repository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository repository,
                                 CurrentUserService currentUserService,
                                 Clock clock) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    /** "Today" in the app's time zone (Africa/Nairobi), from the Clock bean. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ---------- reads ----------

    /** All of the current user's applications, or only those with the given status. */
    public List<ApplicationSummary> listForCurrentUser(Status status) {
        Long userId = currentUserService.currentUserId();
        List<JobApplication> applications = (status == null)
                ? repository.findAllForOwner(userId)
                : repository.findByOwnerIdAndStatusOrderByApplicationDateDesc(userId, status);
        return applications.stream()
                .map(ApplicationSummary::from)
                .toList();
    }

    /**
     * The ownership check for every read AND write.
     * Someone else's id behaves exactly like a missing id: 404, without revealing it exists.
     */
    public JobApplication getForCurrentUser(long id) {
        return repository.findByIdAndOwnerId(id, currentUserService.currentUserId())
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    // ---------- writes ----------

    /** With a date applied it starts as APPLIED, otherwise SAVED (the same rule as Week 1). */
    @Transactional
    public JobApplication create(ApplicationForm form) {
        User owner = currentUserService.currentUser();

        JobApplication application = new JobApplication(
                form.getCompanyName(), form.getJobTitle(), form.getLocation(), form.getApplicationDate());
        form.applyTo(application);
        if (application.getApplicationDate() != null) {
            application.setStatus(Status.APPLIED);
        }

        owner.addApplication(application); // keeps both sides of the relationship in sync
        return repository.save(application);
    }
       @Transactional
    public JobApplication update(long id, ApplicationForm form) {
        JobApplication application = getForCurrentUser(id);
        form.applyTo(application);
        return application;
    }
     @Transactional
    public ApplicationSummary delete(long id) {
        JobApplication application = getForCurrentUser(id); // ownership check first
        ApplicationSummary deleted = ApplicationSummary.from(application);
        repository.delete(application);
        return deleted;
    }

    // ---------- workflow (Week 7) ----------
    // Each method: ownership check → entity rule → log it. Dirty checking saves the changes.

    /** Every status change leaves a dated note, so the notes double as a history. */
    @Transactional
    public JobApplication changeStatus(long id, Status next) {
        JobApplication application = getForCurrentUser(id);
        Status previous = application.getStatus();

        application.changeStatus(next, today()); // throws if the move isn't allowed
        application.addNote("Status changed from " + previous.getLabel() + " to " + next.getLabel(), today());
        return application;
    }

    @Transactional
    public JobApplication scheduleInterview(long id, LocalDate date) {
        JobApplication application = getForCurrentUser(id);
        boolean rescheduling = application.getInterviewDate() != null;

        application.scheduleInterview(date, today());
        application.addNote((rescheduling ? "Interview rescheduled to " : "Interview scheduled for ")
                + DISPLAY_DATE.format(date), today());
        return application;
    }

    /** null clears the reminder. Not logged as a note: reminders change often and aren't history. */
    @Transactional
    public JobApplication scheduleFollowUp(long id, LocalDate date) {
        JobApplication application = getForCurrentUser(id);
        application.scheduleFollowUp(date, today());
        return application;
    }

    @Transactional
    public JobApplication addNote(long id, String note) {
        JobApplication application = getForCurrentUser(id);
        application.addNote(note, today());
        return application;
    }
}