package com.careerpilot.model;

/**
 * Immutable recruiter details. A record gives you the constructor, accessors,
 * equals, hashCode and toString for free.
 */
public record RecruiterContact(String name, String email, String phone) {

    // Compact constructor: runs validation before the fields are assigned
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