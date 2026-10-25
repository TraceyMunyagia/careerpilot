package com.careerpilot.service;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.repository.JobApplicationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Business rules live here, not in the UI and not in the repository.
 * In Week 3 this class gets @Service and Spring injects the repository.
 */
public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
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
        JobApplication app = repository.findById(id);
        if (app == null) {
            throw new IllegalArgumentException("No application with id " + id);
        }
        return app;
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
        if (current == newStatus) {
            throw new IllegalStateException("Application is already " + current.getLabel());
        }
        if (!current.canMoveTo(newStatus)) {
            throw new IllegalStateException("Cannot move from " + current.getLabel()
                    + " to " + newStatus.getLabel() + ". Allowed: " + current.allowedNext());
        }
        app.setStatus(newStatus);
        if (newStatus == Status.APPLIED && app.getApplicationDate() == null) {
            app.setApplicationDate(LocalDate.now());
        }
        return repository.save(app);
    }

    /** Scheduling an interview moves APPLIED to INTERVIEW automatically. */
    public JobApplication scheduleInterview(long id, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Interview date cannot be in the past");
        }
        JobApplication app = getApplication(id);
        Status current = app.getStatus();
        if (current == Status.APPLIED) {
            app.setStatus(Status.INTERVIEW);
        } else if (current != Status.INTERVIEW) {
            throw new IllegalStateException("Can only schedule interviews for Applied or Interview applications (current: "
                    + current.getLabel() + ")");
        }
        app.setInterviewDate(date);
        return repository.save(app);
    }

    /** Closed applications (Rejected or Offer) have nothing left to follow up on. */
    public JobApplication setFollowUp(long id, LocalDate date) {
        JobApplication app = getApplication(id);
        if (app.getStatus().isClosed()) {
            throw new IllegalStateException("Cannot set a follow-up on a closed application (current: "
                    + app.getStatus().getLabel() + ")");
        }
        app.setFollowUpDate(date);
        return repository.save(app);
    }

    public JobApplication addNote(long id, String note) {
        JobApplication app = getApplication(id);
        app.addNote(note);
        return repository.save(app);
    }

    public JobApplication setRecruiter(long id, RecruiterContact recruiter) {
        JobApplication app = getApplication(id);
        app.setRecruiter(recruiter);
        return repository.save(app);
    }

    public void deleteApplication(long id) {
        if (!repository.deleteById(id)) {
            throw new IllegalArgumentException("No application with id " + id);
        }
    }

    // ---------- dashboard numbers ----------

    public int totalCount() {
        return repository.count();
    }

    public int activeCount() {
        int count = 0;
        for (JobApplication app : repository.findAll()) {
            if (app.getStatus().isActive()) {
                count++;
            }
        }
        return count;
    }

    public Map<Status, Integer> statusSummary() {
        return repository.countByStatus();
    }

    public List<JobApplication> upcomingInterviews(LocalDate today) {
        List<JobApplication> result = new ArrayList<>();
        for (JobApplication app : repository.findAll()) {
            if (app.hasUpcomingInterview(today)) {
                result.add(app);
            }
        }
        result.sort(Comparator.comparing(JobApplication::getInterviewDate));
        return result;
    }

    public List<JobApplication> overdueFollowUps(LocalDate today) {
        List<JobApplication> result = new ArrayList<>();
        for (JobApplication app : repository.findAll()) {
            if (app.isFollowUpOverdue(today)) {
                result.add(app);
            }
        }
        result.sort(Comparator.comparing(JobApplication::getFollowUpDate));
        return result;
    }
}