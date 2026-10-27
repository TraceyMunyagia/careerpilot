package com.careerpilot.service;

import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import com.careerpilot.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read operations for now. Create, update and delete arrive in Week 6.
 * readOnly = true lets Hibernate skip dirty-checking, which makes reads cheaper.
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

    /** Someone else's id behaves exactly like a missing id: 404, without revealing it exists. */
    public JobApplication getForCurrentUser(long id) {
        return repository.findByIdAndOwnerId(id, currentUserService.currentUserId())
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }
}