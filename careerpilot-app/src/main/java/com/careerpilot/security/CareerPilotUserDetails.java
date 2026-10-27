package com.careerpilot.security;

import com.careerpilot.model.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * What Spring Security keeps in the session for a logged-in user.
 * Holding the id here means "who is logged in?" never needs a database query.
 */
public class CareerPilotUserDetails implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final Long id;
    private final String email;
    private final String fullName;
    private String passwordHash;

    public CareerPilotUserDetails(Long id, String email, String fullName, String passwordHash) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
    }

    public static CareerPilotUserDetails from(User user) {
        return new CareerPilotUserDetails(user.getId(), user.getEmail(), user.getFullName(), user.getPasswordHash());
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String getUsername() {
        return email; // users log in with their email
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    /** Called after login, so the hash isn't kept in the session. */
    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}