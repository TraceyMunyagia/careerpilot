package com.careerpilot.repository;

import com.careerpilot.TestcontainersConfiguration;
import com.careerpilot.dto.StatusCount;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JobApplicationRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobApplicationRepository repository;

    @Autowired
    private EntityManager entityManager;

    private User tracey;
    private User other;

    @BeforeEach
    void setUp() {
        tracey = userRepository.save(new User("tracey@example.com", "Tracey", "{noop}x"));
        other = userRepository.save(new User("other@example.com", "Other", "{noop}x"));
    }

    private JobApplication add(User owner, String company, Status status, LocalDate applied) {
        JobApplication app = new JobApplication(company, "Java Developer", "Nairobi", applied);
        app.setStatus(status);
        owner.addApplication(app);
        return repository.save(app);
    }

    /** Push pending SQL to the database and empty Hibernate's cache, so the next read hits PostgreSQL. */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void usersOnlySeeTheirOwnApplications() {
        add(tracey, "Safaricom", Status.APPLIED, TODAY);
        add(tracey, "Andela", Status.APPLIED, TODAY);
        JobApplication someoneElses = add(other, "Cellulant", Status.APPLIED, TODAY);

        assertThat(repository.findAllForOwner(tracey.getId())).hasSize(2);
        assertThat(repository.findByIdAndOwnerId(someoneElses.getId(), tracey.getId())).isEmpty();
        assertThat(repository.findByIdAndOwnerId(someoneElses.getId(), other.getId())).isPresent();
    }

    @Test
    void listIsNewestFirstWithUndatedLast() {
        add(tracey, "Old", Status.APPLIED, TODAY.minusDays(30));
        add(tracey, "Undated", Status.SAVED, null);
        add(tracey, "New", Status.APPLIED, TODAY.minusDays(1));
        flushAndClear();

        List<String> companies = repository.findAllForOwner(tracey.getId()).stream()
                .map(JobApplication::getCompanyName)
                .toList();

        assertThat(companies).containsExactly("New", "Old", "Undated");
    }

    @Test
    void countByStatusGroupsInTheDatabase() {
        add(tracey, "A", Status.APPLIED, TODAY);
        add(tracey, "B", Status.APPLIED, TODAY);
        add(tracey, "C", Status.INTERVIEW, TODAY);
        add(other, "D", Status.APPLIED, TODAY); // must not be counted
        flushAndClear();

        Map<Status, Long> counts = repository.countByStatus(tracey.getId()).stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getTotal));

        assertThat(counts).containsEntry(Status.APPLIED, 2L)
                .containsEntry(Status.INTERVIEW, 1L)
                .hasSize(2);
    }

    @Test
    void upcomingInterviewsIncludeTodayAndAreSortedByDate() {
        JobApplication later = add(tracey, "Later", Status.INTERVIEW, TODAY);
        later.setInterviewDate(TODAY.plusDays(5));
        JobApplication today = add(tracey, "Today", Status.INTERVIEW, TODAY);
        today.setInterviewDate(TODAY);
        JobApplication past = add(tracey, "Past", Status.OFFER, TODAY);
        past.setInterviewDate(TODAY.minusDays(1));
        flushAndClear();

        List<String> companies = repository.findUpcomingInterviews(tracey.getId(), TODAY).stream()
                .map(JobApplication::getCompanyName)
                .toList();

        assertThat(companies).containsExactly("Today", "Later");
    }

    @Test
    void overdueFollowUpsSkipClosedApplications() {
        JobApplication open = add(tracey, "Open", Status.APPLIED, TODAY);
        open.setFollowUpDate(TODAY.minusDays(2));
        JobApplication closed = add(tracey, "Closed", Status.REJECTED, TODAY);
        closed.setFollowUpDate(TODAY.minusDays(2));
        JobApplication dueToday = add(tracey, "DueToday", Status.APPLIED, TODAY);
        dueToday.setFollowUpDate(TODAY);
        flushAndClear();

        List<JobApplication> overdue = repository.findOverdueFollowUps(
                tracey.getId(), TODAY, EnumSet.of(Status.REJECTED, Status.OFFER));

        assertThat(overdue).extracting(JobApplication::getCompanyName).containsExactly("Open");
    }

    @Test
    void statusIsStoredAsText() {
        JobApplication app = add(tracey, "Safaricom", Status.INTERVIEW, TODAY);
        entityManager.flush();

        Object raw = entityManager
                .createNativeQuery("select status from job_applications where id = :id")
                .setParameter("id", app.getId())
                .getSingleResult();

        assertThat(raw).isEqualTo("INTERVIEW");
    }

    @Test
    void recruiterRoundTripsThroughEmbeddedColumns() {
        JobApplication app = add(tracey, "Safaricom", Status.APPLIED, TODAY);
        app.setRecruiter(new RecruiterContact("Jane Wanjiku", "jane@example.com", "+254700000001"));
        flushAndClear();

        JobApplication reloaded = repository.findById(app.getId()).orElseThrow();

        assertThat(reloaded.getRecruiter())
                .isEqualTo(new RecruiterContact("Jane Wanjiku", "jane@example.com", "+254700000001"));
    }

    @Test
    void missingRecruiterLoadsAsNull() {
        JobApplication app = add(tracey, "Andela", Status.APPLIED, TODAY);
        flushAndClear();

        assertThat(repository.findById(app.getId()).orElseThrow().findRecruiter()).isEmpty();
    }

    @Test
    void removingFromTheUserDeletesTheOrphan() {
        JobApplication app = add(tracey, "Safaricom", Status.APPLIED, TODAY);
        add(tracey, "Andela", Status.APPLIED, TODAY);
        entityManager.flush();

        tracey.removeApplication(app);
        flushAndClear();

        assertThat(repository.countByOwnerId(tracey.getId())).isEqualTo(1);
    }

    @Test
    void deletingAUserCascadesToTheirApplications() {
        add(tracey, "Safaricom", Status.APPLIED, TODAY);
        add(other, "Cellulant", Status.APPLIED, TODAY);
        entityManager.flush();

        userRepository.delete(tracey);
        flushAndClear();

        assertThat(repository.count()).isEqualTo(1);
    }
}