package com.careerpilot.model;

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
    public void addNote(String note, LocalDate date) {
        String line = "[" + date + "] " + requireText(note, "Note");
        notes = notes.isEmpty() ? line : notes + System.lineSeparator() + line;
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