package com.careerpilot.ui;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.service.JobApplicationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The console "front end". In Week 3 this is replaced by Spring MVC controllers and Thymeleaf pages. */
public class ConsoleMenu {

    private static final String DIVIDER = "-".repeat(78);

    private final JobApplicationService service;
    private final InputHelper input;
    private boolean running = true;

    public ConsoleMenu(JobApplicationService service, InputHelper input) {
        this.service = service;
        this.input = input;
    }

    public void start() {
        System.out.println("=== CareerPilot Console ===");
        while (running) {
            printMenu();
            int choice = input.readInt("Choose an option: ", 0, 12);
            try {
                handle(choice);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("! " + e.getMessage());
            }
        }
        System.out.println("Goodbye, and good luck with the applications!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println(" 1. Add application        6. Set follow-up date");
        System.out.println(" 2. List all               7. Add note");
        System.out.println(" 3. View details           8. Add recruiter contact");
        System.out.println(" 4. Update status          9. Dashboard summary");
        System.out.println(" 5. Schedule interview     0. Exit");
        System.out.println("10. Filter by status      11. Search by company");
        System.out.println("12. Delete application");
        System.out.println(DIVIDER);
    }

    private void handle(int choice) {
        switch (choice) {
            case 1 -> addApplication();
            case 2 -> printTable(service.listAll());
            case 3 -> viewDetails();
            case 4 -> updateStatus();
            case 5 -> scheduleInterview();
            case 6 -> setFollowUp();
            case 7 -> addNote();
            case 8 -> addRecruiter();
            case 9 -> showDashboard();
            case 10 -> filterByStatus();
            case 11 -> searchByCompany();
            case 12 -> deleteApplication();
            case 0 -> running = false;
            default -> System.out.println("Unknown option.");
        }
    }

    // ---------- actions ----------

    private void addApplication() {
        String company = input.readRequired("Company: ");
        String title = input.readRequired("Job title: ");
        String location = input.readOptional("Location (optional): ");
        String link = input.readOptional("Application link (optional): ");
        String description = input.readOptional("Job description (optional, one line): ");
        LocalDate appliedOn = input.readDate("Date applied (yyyy-MM-dd, 'today', or blank if only saved): ", true);

        JobApplication app = service.addApplication(company, title, location, link, description, appliedOn);
        System.out.println("Saved as #" + app.getId() + " (" + app.getStatus().getLabel() + ")");
    }

    private void viewDetails() {
        JobApplication app = service.getApplication(input.readId("Application id: "));
        System.out.println(DIVIDER);
        System.out.printf("#%d  %s @ %s%n", app.getId(), app.getJobTitle(), app.getCompanyName());
        System.out.println("Status:      " + app.getStatus().getLabel());
        System.out.println("Location:    " + orDash(app.getLocation()));
        System.out.println("Applied on:  " + orDash(app.getApplicationDate()));
        System.out.println("Link:        " + orDash(app.getApplicationLink()));
        System.out.println("Interview:   " + orDash(app.getInterviewDate()));
        System.out.println("Follow-up:   " + orDash(app.getFollowUpDate()));

        RecruiterContact r = app.getRecruiter();
        System.out.println("Recruiter:   " + (r == null ? "-"
                : r.name() + " | " + orDash(r.email()) + " | " + orDash(r.phone())));
        System.out.println("Description: " + orDash(app.getJobDescription()));
        System.out.println("Notes:");
        System.out.println(app.getNotes().isEmpty() ? "  -" : app.getNotes());
    }

    private void updateStatus() {
        long id = input.readId("Application id: ");
        JobApplication app = service.getApplication(id);
        System.out.println("Current status: " + app.getStatus().getLabel()
                + " | allowed next: " + app.getStatus().allowedNext());
        Status next = input.readStatus("New status: ");
        service.changeStatus(id, next);
        System.out.println("Updated to " + next.getLabel());
    }

    private void scheduleInterview() {
        long id = input.readId("Application id: ");
        service.getApplication(id); // fail fast before asking for the date
        LocalDate date = input.readDate("Interview date (yyyy-MM-dd): ", false);
        JobApplication app = service.scheduleInterview(id, date);
        System.out.println("Interview set for " + date + ". Status: " + app.getStatus().getLabel());
    }

    private void setFollowUp() {
        long id = input.readId("Application id: ");
        service.getApplication(id);
        LocalDate date = input.readDate("Follow-up date (yyyy-MM-dd): ", false);
        service.setFollowUp(id, date);
        System.out.println("Follow-up set for " + date);
    }

    private void addNote() {
        long id = input.readId("Application id: ");
        service.getApplication(id);
        String note = input.readRequired("Note: ");
        service.addNote(id, note);
        System.out.println("Note added.");
    }

    private void addRecruiter() {
        long id = input.readId("Application id: ");
        service.getApplication(id);
        String name = input.readRequired("Recruiter name: ");
        String email = input.readOptional("Email (optional): ");
        String phone = input.readOptional("Phone (optional): ");
        service.setRecruiter(id, new RecruiterContact(name, email, phone));
        System.out.println("Recruiter saved.");
    }

    private void showDashboard() {
        LocalDate today = LocalDate.now();
        System.out.println(DIVIDER);
        System.out.println("Total applications:         " + service.totalCount());
        System.out.println("Active (applied/interview): " + service.activeCount());

        Map<Status, Integer> summary = service.statusSummary();
        for (Map.Entry<Status, Integer> entry : summary.entrySet()) {
            System.out.printf("  %-10s %d%n", entry.getKey().getLabel(), entry.getValue());
        }

        System.out.println();
        System.out.println("Upcoming interviews:");
        List<JobApplication> upcoming = service.upcomingInterviews(today);
        if (upcoming.isEmpty()) {
            System.out.println("  none");
        }
        for (JobApplication app : upcoming) {
            System.out.printf("  %s  %s @ %s%n", app.getInterviewDate(), app.getJobTitle(), app.getCompanyName());
        }

        System.out.println("Overdue follow-ups:");
        List<JobApplication> overdue = service.overdueFollowUps(today);
        if (overdue.isEmpty()) {
            System.out.println("  none");
        }
        for (JobApplication app : overdue) {
            System.out.printf("  %s  %s @ %s%n", app.getFollowUpDate(), app.getJobTitle(), app.getCompanyName());
        }
    }

    private void filterByStatus() {
        Status status = input.readStatus("Show which status? ");
        printTable(service.listByStatus(status));
    }

    private void searchByCompany() {
        String text = input.readRequired("Company name contains: ");
        printTable(service.searchByCompany(text));
    }

    private void deleteApplication() {
        long id = input.readId("Application id to delete: ");
        JobApplication app = service.getApplication(id);
        if (input.confirm("Delete #" + id + " " + app.getJobTitle() + " @ " + app.getCompanyName() + "?")) {
            service.deleteApplication(id);
            System.out.println("Deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // ---------- formatting helpers ----------

    private void printTable(List<JobApplication> apps) {
        if (apps.isEmpty()) {
            System.out.println("No applications found.");
            return;
        }
        String format = "%-4s %-20s %-26s %-10s %-10s%n";
        System.out.printf(format, "ID", "Company", "Job title", "Status", "Applied");
        System.out.println(DIVIDER);
        for (JobApplication app : apps) {
            System.out.printf(format,
                    app.getId(),
                    cut(app.getCompanyName(), 20),
                    cut(app.getJobTitle(), 26),
                    app.getStatus().getLabel(),
                    orDash(app.getApplicationDate()));
        }
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private static String orDash(Object value) {
        return value == null ? "-" : value.toString();
    }
}