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

import java.util.List;

/**
 * Class-level readOnly = true is the default for every method;
 * write methods override it with a plain @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final CurrentUserService currentUserService;

    public JobApplicationService(JobApplicationRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
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
}