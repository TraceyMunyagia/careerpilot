package com.careerpilot.dto;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Backs both the "add" and "edit" forms.
 * Why not bind straight to the entity? Because then a user could post fields you never
 * meant to expose (owner, status, id). The form holds only what the page may change.
 * Validation lives here; the entity keeps its own guards as a second line of defence.
 */
public class ApplicationForm {

    @NotBlank(message = "Enter the company name")
    @Size(max = 150, message = "Company name must be at most 150 characters")
    private String companyName;

    @NotBlank(message = "Enter the job title")
    @Size(max = 150, message = "Job title must be at most 150 characters")
    private String jobTitle;

    @Size(max = 120, message = "Location must be at most 120 characters")
    private String location;

    @Size(max = 500, message = "Link must be at most 500 characters")
    @Pattern(regexp = "^https?://\\S+$", message = "Enter a full link starting with http:// or https://")
    private String applicationLink;

    /** type="date" inputs send yyyy-MM-dd; ISO.DATE reads and writes exactly that format. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "The date applied can't be in the future")
    private LocalDate applicationDate;

    @Size(max = 20_000, message = "Job description must be at most 20,000 characters")
    private String jobDescription;

    @Size(max = 120, message = "Recruiter name must be at most 120 characters")
    private String recruiterName;

    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String recruiterEmail;

    @Size(max = 40, message = "Phone must be at most 40 characters")
    @Pattern(regexp = "^[+0-9 ()-]+$", message = "Use digits, spaces, +, ( ) and - only")
    private String recruiterPhone;

    // ---------- cross-field rule ----------

    /** Reported as an error on the "recruiterNameProvided" property. */
    @AssertTrue(message = "Add the recruiter's name, or clear their email and phone")
    public boolean isRecruiterNameProvided() {
        boolean hasContactDetails = hasText(recruiterEmail) || hasText(recruiterPhone);
        return !hasContactDetails || hasText(recruiterName);
    }

    // ---------- mapping ----------

    /** Pre-fills the edit form from an existing application. */
    public static ApplicationForm from(JobApplication application) {
        ApplicationForm form = new ApplicationForm();
        form.companyName = application.getCompanyName();
        form.jobTitle = application.getJobTitle();
        form.location = application.getLocation();
        form.applicationLink = application.getApplicationLink();
        form.applicationDate = application.getApplicationDate();
        form.jobDescription = application.getJobDescription();
        RecruiterContact recruiter = application.getRecruiter();
        if (recruiter != null) {
            form.recruiterName = recruiter.name();
            form.recruiterEmail = recruiter.email();
            form.recruiterPhone = recruiter.phone();
        }
        return form;
    }

    /** Copies the editable fields onto the entity. Status, owner and notes are never touched here. */
    public void applyTo(JobApplication application) {
        application.setCompanyName(companyName);
        application.setJobTitle(jobTitle);
        application.setLocation(blankToNull(location));
        application.setApplicationLink(blankToNull(applicationLink));
        application.setApplicationDate(applicationDate);
        application.setJobDescription(blankToNull(jobDescription));
        application.setRecruiter(toRecruiter());
    }

    /** null when all three recruiter fields are empty. */
    public RecruiterContact toRecruiter() {
        if (!hasText(recruiterName) && !hasText(recruiterEmail) && !hasText(recruiterPhone)) {
            return null;
        }
        return new RecruiterContact(recruiterName, blankToNull(recruiterEmail), blankToNull(recruiterPhone));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    // ---------- getters and setters (Thymeleaf's th:field needs them) ----------

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getApplicationLink() { return applicationLink; }
    public void setApplicationLink(String applicationLink) { this.applicationLink = applicationLink; }

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }

    public String getRecruiterEmail() { return recruiterEmail; }
    public void setRecruiterEmail(String recruiterEmail) { this.recruiterEmail = recruiterEmail; }

    public String getRecruiterPhone() { return recruiterPhone; }
    public void setRecruiterPhone(String recruiterPhone) { this.recruiterPhone = recruiterPhone; }
}