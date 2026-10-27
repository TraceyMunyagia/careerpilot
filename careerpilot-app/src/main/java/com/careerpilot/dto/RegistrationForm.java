package com.careerpilot.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Backs the registration form. A plain class (not a record), because
 * Thymeleaf's th:field needs getters and setters to re-fill the form after errors.
 */
public class RegistrationForm {

    @NotBlank(message = "Enter your full name")
    @Size(max = 120, message = "Name must be at most 120 characters")
    private String fullName;

    @NotBlank(message = "Enter your email address")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    /** BCrypt only uses the first 72 bytes, so longer passwords give a false sense of security. */
    @NotBlank(message = "Choose a password")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Use at least one letter and one number")
    private String password;

    @NotBlank(message = "Confirm your password")
    private String confirmPassword;

    /** Cross-field check: reported as an error on the "passwordConfirmed" property. */
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null || confirmPassword.isBlank()) {
            return true; // @NotBlank already reports the missing value
        }
        return password.equals(confirmPassword);
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}