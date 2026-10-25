package com.careerpilot.data;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.service.JobApplicationService;

import java.time.LocalDate;

/** Sample data so the app isn't empty on start-up. */
public final class SeedData {

    private SeedData() {
        // utility class, never instantiated
    }

    public static void load(JobApplicationService service) {
        LocalDate today = LocalDate.now();

        JobApplication a = service.addApplication("Safaricom", "Junior Java Developer", "Nairobi",
                "https://example.com/jobs/safaricom-java", "Java, Spring Boot, SQL, REST APIs", today.minusDays(10));
        service.scheduleInterview(a.getId(), today.plusDays(3));
        service.setRecruiter(a.getId(), new RecruiterContact("Jane Wanjiku", "jane@example.com", "+254700000001"));
        service.addNote(a.getId(), "Prepare Spring Boot and SQL examples");

        JobApplication b = service.addApplication("Andela", "Backend Engineer", "Remote",
                null, "Java, PostgreSQL, Docker", today.minusDays(20));
        service.setFollowUp(b.getId(), today.minusDays(2)); // already overdue

        JobApplication c = service.addApplication("Cellulant", "Software Engineer", "Nairobi",
                null, null, today.minusDays(30));
        service.changeStatus(c.getId(), Status.REJECTED);

        service.addApplication("Twiga Foods", "Full-Stack Developer", "Nairobi", null, null, null); // saved only
    }
}