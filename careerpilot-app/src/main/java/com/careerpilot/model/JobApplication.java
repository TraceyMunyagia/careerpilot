package com.careerpilot.model;


import com.careerpilot.exception.BusinessRuleException;
import com.careerpilot.exception.InvalidStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Optional;

/** The console app's JobApplication, now mapped to a PostgreSQL table. */
@Entity
@Table(name = "job_applications", indexes = {
        @Index(name = "idx_job_applications_user", columnList = "user_id"),
        @Index(name = "idx_job_applications_user_status", columnList = "user_id, status")
})
public class JobApplication extends BaseEntity {

    /** The owning side of the relationship: this entity holds the foreign key column. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(length = 120)
    private String location;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "application_link", length = 500)
    private String applicationLink;

    @Column(name = "application_date")
    private LocalDate applicationDate;

    /** STRING stores 'APPLIED', not 1. Reordering the enum later can't corrupt data. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.SAVED;

        @Embedded
    private RecruiterContact recruiter;

    @Column(name = "interview_date")
    private LocalDate interviewDate;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes = "";

    /** Required by JPA; not for application code. */
    protected JobApplication() {
    }

    public JobApplication(String companyName, String jobTitle, String location, LocalDate applicationDate) {
        setCompanyName(companyName);
        setJobTitle(jobTitle);
        this.location = location;
        this.applicationDate = applicationDate;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    // ---------- relationship ----------

    public User getOwner() { return owner; }

    /** Package-private: always go through User.addApplication so both sides stay in sync. */
    void setOwner(User owner) { this.owner = owner; }

    // ---------- getters and setters ----------

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) {
        this.companyName = requireText(companyName, "Company name");
    }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) {
        this.jobTitle = requireText(jobTitle, "Job title");
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getApplicationLink() { return applicationLink; }
    public void setApplicationLink(String applicationLink) { this.applicationLink = applicationLink; }

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.status = status;
    }

        public RecruiterContact getRecruiter() { return recruiter; }
    public void setRecruiter(RecruiterContact recruiter) { this.recruiter = recruiter; }

    /** Optional view of the recruiter, as in Week 2. */
    public Optional<RecruiterContact> findRecruiter() {
        return Optional.ofNullable(recruiter);
    }

    public LocalDate getInterviewDate() { return interviewDate; }
    public void setInterviewDate(LocalDate interviewDate) { this.interviewDate = interviewDate; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public String getNotes() { return notes; }

    /** Appends a note dated with the given day on its own line. */
       public static final int MAX_NOTE_LENGTH = 2000;

    /**
     * Appends a note dated with the given day. Line breaks inside a note become spaces,
     * so one note is always exactly one line: "[2026-11-16] text".
     */
    public void addNote(String note, LocalDate date) {
        if (note == null || note.isBlank()) {
            throw new BusinessRuleException("Write something before adding a note");
        }
        String text = note.strip().replaceAll("\\s*\\R\\s*", " ");
        if (text.length() > MAX_NOTE_LENGTH) {
            throw new BusinessRuleException("Notes can be at most " + MAX_NOTE_LENGTH + " characters");
        }
        String line = "[" + date + "] " + text;
        notes = notes.isEmpty() ? line : notes + "\n" + line;
    }
    // ---------- workflow (Week 7): the rules live with the data they protect ----------
    // Each method checks everything first and only then changes fields,
    // so a broken rule leaves the application exactly as it was.

    /**
     * Moves to the next status if Status allows it.
     * Moving to APPLIED without a date stamps today (the same rule as the Week 2 console app).
     * Closing an application (Rejected/Offer) clears its follow-up reminder.
     */
    public void changeStatus(Status next, LocalDate today) {
        if (next == null) {
            throw new BusinessRuleException("Choose a status");
        }
        if (status == next || !status.canMoveTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
        status = next;
        if (next == Status.APPLIED && applicationDate == null) {
            applicationDate = today;
        }
        if (next.isClosed()) {
            followUpDate = null;
        }
    }

    /** Only Applied or Interview applications can have interviews. Applied moves to Interview. */
    public void scheduleInterview(LocalDate date, LocalDate today) {
        if (date == null) {
            throw new BusinessRuleException("Choose an interview date");
        }
        if (date.isBefore(today)) {
            throw new BusinessRuleException("Interview date cannot be in the past");
        }
        if (status != Status.APPLIED && status != Status.INTERVIEW) {
            throw new BusinessRuleException("Interviews can only be scheduled for Applied or Interview applications (this one is "
                    + status.getLabel() + ")");
        }
        status = Status.INTERVIEW;
        interviewDate = date;
    }

    /** null clears the reminder. Closed applications don't need follow-ups. */
    public void scheduleFollowUp(LocalDate date, LocalDate today) {
        if (date == null) {
            followUpDate = null;
            return;
        }
        if (status.isClosed()) {
            throw new BusinessRuleException("No follow-up needed: this application is " + status.getLabel());
        }
        if (date.isBefore(today)) {
            throw new BusinessRuleException("Follow-up date cannot be in the past");
        }
        followUpDate = date;
    }


    // ---------- date helpers (from Week 1) ----------

    public boolean hasUpcomingInterview(LocalDate today) {
        return interviewDate != null && !interviewDate.isBefore(today);
    }

    public boolean isFollowUpOverdue(LocalDate today) {
        return followUpDate != null && followUpDate.isBefore(today) && !status.isClosed();
    }

    /** Never include owner here: printing it would trigger a lazy database load. */
    @Override
    public String toString() {
        return "JobApplication{id=" + getId()
                + ", company='" + companyName + '\''
                + ", title='" + jobTitle + '\''
                + ", status=" + status + '}';
    }
}