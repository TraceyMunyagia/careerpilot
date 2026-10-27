package com.careerpilot.service;

import com.careerpilot.repository.UserRepository;
import com.careerpilot.security.CareerPilotUserDetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security calls this during login with whatever was typed in the email field.
 * It then checks the password itself, using the PasswordEncoder bean.
 */
@Service
public class CareerPilotUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CareerPilotUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(CareerPilotUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
    }
}