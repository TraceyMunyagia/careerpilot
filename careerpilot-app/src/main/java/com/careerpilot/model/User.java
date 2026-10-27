package com.careerpilot.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users") // "user" is a reserved word in PostgreSQL
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    /** Filled with a real BCrypt hash in Week 5. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;


    /**
     * Inverse side: mappedBy points at JobApplication.owner, which holds the foreign key.
     * cascade ALL: saving or deleting a user saves or deletes their applications.
     * orphanRemoval: an application removed from this list is deleted.
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobApplication> applications = new ArrayList<>();

    /** JPA needs a no-argument constructor; protected keeps application code from using it. */
    protected User() {
    }

    public User(String email, String fullName, String passwordHash) {
        setEmail(email);
        setFullName(fullName);
        setPasswordHash(passwordHash);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = requireText(email, "Email").toLowerCase(); // store one canonical form
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {
        this.fullName = requireText(fullName, "Full name");
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = requireText(passwordHash, "Password hash");
    }

    // ---------- relationship helpers: always keep both sides in sync ----------

    public List<JobApplication> getApplications() {
        return Collections.unmodifiableList(applications);
    }

    public void addApplication(JobApplication application) {
        applications.add(application);
        application.setOwner(this);
    }

    /** orphanRemoval deletes the row on the next flush. */
    public void removeApplication(JobApplication application) {
        applications.remove(application);
    }

    @Override
    public String toString() {
        return "User{id=" + getId() + ", email='" + email + "'}";
    }
}