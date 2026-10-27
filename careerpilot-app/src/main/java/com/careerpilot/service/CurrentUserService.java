package com.careerpilot.service;

import com.careerpilot.model.User;
import com.careerpilot.repository.UserRepository;
import com.careerpilot.security.CareerPilotUserDetails;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that answers "who is logged in?". Every other service relies on this,
 * so swapping the Week 4 placeholder for real security changed only this file.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Read from the security context, with no database query. */
    public Long currentUserId() {
        return currentPrincipal().getId();
    }

    /** Loads the full entity, for when you need more than the id. */
    @Transactional(readOnly = true)
    public User currentUser() {
        Long id = currentUserId();
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Logged-in user " + id + " no longer exists"));
    }

    private CareerPilotUserDetails currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CareerPilotUserDetails details) {
            return details;
        }
        throw new AuthenticationCredentialsNotFoundException("No logged-in CareerPilot user");
    }
}