package com.careerpilot.service;

import com.careerpilot.exception.EmailAlreadyRegisteredException;
import com.careerpilot.model.User;
import com.careerpilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void passwordIsHashedAndEmailNormalised() {
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.register("Tracey", "  Tracey@Example.com ", "Password123");

        assertThat(user.getEmail()).isEqualTo("tracey@example.com");
        assertThat(user.getPasswordHash())
                .startsWith("{bcrypt}")
                .doesNotContain("Password123");
        assertThat(passwordEncoder.matches("Password123", user.getPasswordHash())).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedBeforeSaving() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("Someone", "Taken@example.com", "Password123"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }
}