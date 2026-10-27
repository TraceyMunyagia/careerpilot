package com.careerpilot.config;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.model.User;
import com.careerpilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Runs once after startup. Creates a demo user with sample applications,
 * dated relative to today so the dashboard always has something to show.
 * Turn it off with careerpilot.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(prefix = "careerpilot.seed", name = "enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@careerpilot.local";
    public static final String DEMO_PASSWORD = "Password123!";

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

      private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            log.info("Seed data already present for {}, skipping", DEMO_EMAIL);
            return; // idempotent: safe to run on every restart
        }

        LocalDate today = LocalDate.now(clock);
        // "{noop}" means "not hashed". Week 5 replaces this with a BCrypt hash.
        User demo = new User(DEMO_EMAIL, "Demo User", passwordEncoder.encode(DEMO_PASSWORD));
        
        JobApplication safaricom = new JobApplication("Safaricom", "Junior Java Developer", "Nairobi", today.minusDays(10));
        safaricom.setStatus(Status.INTERVIEW);
        safaricom.setInterviewDate(today.plusDays(3));
        safaricom.setApplicationLink("https://example.com/jobs/safaricom-java");
        safaricom.setJobDescription("""
                Build and maintain REST APIs with Java 21 and Spring Boot.
                Work with PostgreSQL, Docker and CI pipelines.
                2+ years of experience; familiarity with M-PESA integrations is a plus.""");
        safaricom.setRecruiter(new RecruiterContact("Jane Wanjiku", "jane@example.com", "+254700000001"));
        safaricom.addNote("Applied through the careers portal", today.minusDays(10));
        safaricom.addNote("Recruiter called; technical interview booked", today.minusDays(2));
        demo.addApplication(safaricom);

        JobApplication andela = new JobApplication("Andela", "Backend Engineer", "Remote", today.minusDays(20));
        andela.setStatus(Status.APPLIED);
        andela.setJobDescription("Java, PostgreSQL, Docker, AWS. Remote across Africa.");
        andela.setFollowUpDate(today.minusDays(2)); // already overdue
        demo.addApplication(andela);

        JobApplication microsoft = new JobApplication("Microsoft ADC", "Software Engineer II", "Nairobi", today.minusDays(3));
        microsoft.setStatus(Status.APPLIED);
        microsoft.setFollowUpDate(today.plusDays(4));
        demo.addApplication(microsoft);

        JobApplication cellulant = new JobApplication("Cellulant", "Software Engineer", "Nairobi", today.minusDays(30));
        cellulant.setStatus(Status.REJECTED);
        cellulant.addNote("Rejection email: looking for more payments experience", today.minusDays(12));
        demo.addApplication(cellulant);

        JobApplication mkopa = new JobApplication("M-KOPA", "Java Developer", "Nairobi", today.minusDays(40));
        mkopa.setStatus(Status.OFFER);
        mkopa.setInterviewDate(today.minusDays(15));
        mkopa.setRecruiter(new RecruiterContact("Brian Otieno", "brian@example.com", null));
        demo.addApplication(mkopa);

        JobApplication twiga = new JobApplication("Twiga Foods", "Full-Stack Developer", "Nairobi", null);
        twiga.setApplicationLink("https://example.com/jobs/twiga-fullstack");
        demo.addApplication(twiga); // stays SAVED

        // One save: cascade = ALL persists every application with it
        userRepository.save(demo);
        log.info("Seeded demo user {} with {} applications", demo.getEmail(), demo.getApplications().size());
    }
}