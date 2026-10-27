package com.careerpilot.service;

import com.careerpilot.exception.EmailAlreadyRegisteredException;
import com.careerpilot.model.User;
import com.careerpilot.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String fullName, String email, String rawPassword) {
        String normalisedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalisedEmail)) {
            throw new EmailAlreadyRegisteredException(normalisedEmail);
        }

        User user = new User(normalisedEmail, fullName, passwordEncoder.encode(rawPassword));
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups with the same email at the same moment: the unique constraint catches it
            throw new EmailAlreadyRegisteredException(normalisedEmail);
        }
    }
}