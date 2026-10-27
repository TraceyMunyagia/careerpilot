package com.careerpilot.service;

import com.careerpilot.config.DataSeeder;
import com.careerpilot.model.User;
import com.careerpilot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TEMPORARY: until logins exist (Week 5), everyone is the demo user.
 * Every other service asks this class "who is the user?", so Week 5 only changes this one file.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User currentUser() {
        return userRepository.findByEmailIgnoreCase(DataSeeder.DEMO_EMAIL)
                .orElseThrow(() -> new IllegalStateException(
                        "Demo user not found. Set careerpilot.seed.enabled=true and restart."));
    }

    public Long currentUserId() {
        return currentUser().getId();
    }
}