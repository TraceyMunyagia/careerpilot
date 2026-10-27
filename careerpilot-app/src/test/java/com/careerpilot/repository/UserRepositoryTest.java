package com.careerpilot.repository;

import com.careerpilot.TestcontainersConfiguration;
import com.careerpilot.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Each @DataJpaTest method runs in a transaction that is rolled back afterwards. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // use the container, not an in-memory DB
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void emailIsStoredLowercaseAndFoundIgnoringCase() {
        userRepository.save(new User("Tracey@Example.com", "Tracey", "{noop}x"));

        User found = userRepository.findByEmailIgnoreCase("TRACEY@example.COM").orElseThrow();

        assertThat(found.getEmail()).isEqualTo("tracey@example.com");
        assertThat(userRepository.existsByEmailIgnoreCase("tracey@example.com")).isTrue();
    }

    @Test
    void idAndTimestampsAreSetOnSave() {
        User saved = userRepository.saveAndFlush(new User("a@example.com", "A", "{noop}x"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void duplicateEmailIsRejectedByTheDatabase() {
        userRepository.saveAndFlush(new User("same@example.com", "First", "{noop}x"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("SAME@example.com", "Second", "{noop}x")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}