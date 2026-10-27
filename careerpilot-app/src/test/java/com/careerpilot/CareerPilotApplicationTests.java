package com.careerpilot;

import com.careerpilot.config.DataSeeder;
import com.careerpilot.repository.JobApplicationRepository;
import com.careerpilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/** Starts the whole app against a Testcontainers PostgreSQL. Docker Desktop must be running. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CareerPilotApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Test
    void contextLoads() {
        // Passes if every bean can be created and wired together
    }

    @Test
    void seederCreatesTheDemoUserAndApplications() {
        assertThat(userRepository.existsByEmailIgnoreCase(DataSeeder.DEMO_EMAIL)).isTrue();
        assertThat(jobApplicationRepository.count()).isEqualTo(6);
    }
}