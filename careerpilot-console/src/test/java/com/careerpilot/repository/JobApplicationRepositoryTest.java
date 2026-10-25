package com.careerpilot.repository;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobApplicationRepositoryTest {

    private JobApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JobApplicationRepository();
    }

    /** Small helper so each test reads clearly. */
    private JobApplication save(String company, LocalDate applied, Status status) {
        JobApplication app = new JobApplication(company, "Java Developer", "Nairobi", applied);
        app.setStatus(status);
        return repository.save(app);
    }

    @Test
    void saveAssignsSequentialIds() {
        JobApplication first = save("Safaricom", null, Status.SAVED);
        JobApplication second = save("Andela", null, Status.SAVED);

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
    }

    @Test
    void savingTwiceUpdatesInsteadOfDuplicating() {
        JobApplication app = save("Safaricom", null, Status.SAVED);
        app.setStatus(Status.APPLIED);
        repository.save(app);

        assertEquals(1, repository.count());
        assertEquals(Status.APPLIED, repository.findById(app.getId()).orElseThrow().getStatus());
    }

    @Test
    void findByIdIsEmptyWhenMissing() {
        assertTrue(repository.findById(99).isEmpty());
    }

    @Test
    void findAllReturnsACopy() {
        save("Safaricom", null, Status.SAVED);

        List<JobApplication> all = repository.findAll();
        all.clear();

        assertEquals(1, repository.count());
    }

    @Test
    void sortedByDateIsNewestFirstWithUndatedLast() {
        save("Old", LocalDate.of(2026, 9, 1), Status.APPLIED);
        save("Undated", null, Status.SAVED);
        save("New", LocalDate.of(2026, 10, 20), Status.APPLIED);

        List<JobApplication> sorted = repository.findAllSortedByDate();

        assertEquals("New", sorted.get(0).getCompanyName());
        assertEquals("Old", sorted.get(1).getCompanyName());
        assertEquals("Undated", sorted.get(2).getCompanyName());
    }

    @Test
    void deleteReportsWhetherSomethingWasRemoved() {
        JobApplication app = save("Safaricom", null, Status.SAVED);

        assertTrue(repository.deleteById(app.getId()));
        assertFalse(repository.deleteById(app.getId()));
        assertEquals(0, repository.count());
    }
    
    @Test
    void findByStatusReturnsOnlyMatches() {
        save("Safaricom", LocalDate.of(2026, 10, 1), Status.APPLIED);
        save("Andela", LocalDate.of(2026, 10, 2), Status.INTERVIEW);

        List<JobApplication> applied = repository.findByStatus(Status.APPLIED);

        assertEquals(1, applied.size());
        assertEquals("Safaricom", applied.get(0).getCompanyName());
    }

    @Test
    void companySearchIgnoresCase() {
        save("Andela", null, Status.SAVED);
        save("Safaricom", null, Status.SAVED);

        assertEquals(1, repository.findByCompanyContaining("AND").size());
    }

    @Test
    void locationSearchSkipsApplicationsWithoutALocation() {
        save("Andela", null, Status.SAVED); // helper sets "Nairobi"
        JobApplication noLocation = new JobApplication("Remote Co", "Dev", null, null);
        repository.save(noLocation);

        assertEquals(1, repository.findByLocation("nairobi").size());
    }

    @Test
    void countByStatusIncludesZeroCounts() {
        save("Safaricom", null, Status.SAVED);
        save("Andela", LocalDate.of(2026, 10, 2), Status.APPLIED);
        save("Cellulant", LocalDate.of(2026, 10, 3), Status.APPLIED);

        Map<Status, Long> counts = repository.countByStatus();

        assertEquals(5, counts.size());
        assertEquals(2L, counts.get(Status.APPLIED));
        assertEquals(0L, counts.get(Status.OFFER));
    }
}