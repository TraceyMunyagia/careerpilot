package com.careerpilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The Week 1 record, now @Embeddable: its fields become three columns
 * (recruiter_name, recruiter_email, recruiter_phone) in job_applications. No separate table.
 * When all three columns are null, Hibernate gives you a null recruiter.
 */
@Embeddable
public record RecruiterContact(
        @Column(name = "recruiter_name", length = 120) String name,
        @Column(name = "recruiter_email", length = 254) String email,
        @Column(name = "recruiter_phone", length = 40) String phone) {

    public RecruiterContact {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Recruiter name must not be blank");
        }
        name = name.trim();
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new IllegalArgumentException("Recruiter email looks invalid: " + email);
        }
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}