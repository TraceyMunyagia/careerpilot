package com.careerpilot.model;

import java.time.LocalDate;
import java.util.Optional;
import java.time.temporal.ChronoUnit;

/**
 * One job you are tracking. This is a plain Java class for now;
 * in Week 4 it becomes a JPA @Entity.
 */
public class JobApplication {

    private Long id;                    // null until the repository saves it
    private String companyName;
    private String jobTitle;
    private String location;
    private String jobDescription;
    private String applicationLink;
    private LocalDate applicationDate;  // null while the status is SAVED
    private Status status;
    private RecruiterContact recruiter;  // optional
    private LocalDate interviewDate;     // optional
    private LocalDate followUpDate;      // optional
    private String notes = "";           // newest note last



    public JobApplication(String companyName, String jobTitle, String location, LocalDate applicationDate) {
        setCompanyName(companyName);
        setJobTitle(jobTitle);
        this.location = location;
        this.applicationDate = applicationDate;
        this.status = Status.SAVED;
    }

    // ---------- validation helper ----------

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    // ---------- getters and setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    // ---------- behaviour ----------

    /** True when the location mentions "remote" in any case, e.g. "Remote" or "Nairobi (REMOTE)". */
    public boolean isRemote() {
        return location != null && location.toLowerCase().contains("remote");
    }

        public RecruiterContact getRecruiter() { return recruiter; }
    public void setRecruiter(RecruiterContact recruiter) { this.recruiter = recruiter; }

    /** Optional view of the recruiter, so callers can't forget the "no recruiter" case. */
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

    // ---------- date helpers ----------

    public boolean hasUpcomingInterview(LocalDate today) {
        return interviewDate != null && !interviewDate.isBefore(today);
    }

    public boolean isFollowUpOverdue(LocalDate today) {
        return followUpDate != null && followUpDate.isBefore(today) && !status.isClosed();
    }

    /** Whole days from applicationDate to today, or -1 if you haven't applied yet. */
    public long daysSinceApplied(LocalDate today) {
        if (applicationDate == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(applicationDate, today);
    }

    // ---------- identity ----------

    /** Two applications are equal only if both are saved and share an id (the JPA-friendly pattern). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobApplication other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "JobApplication{id=" + id
                + ", company='" + companyName + '\''
                + ", title='" + jobTitle + '\''
                + ", status=" + status
                + ", applied=" + applicationDate + '}';
    }
}
