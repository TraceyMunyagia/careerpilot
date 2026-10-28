package com.careerpilot.service;

import com.careerpilot.dto.ApplicationForm;
import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.model.User;
import com.careerpilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring, no database. Mockito stands in for the repository and the
 * current-user lookup, so each test controls exactly what the service sees.
 */
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private JobApplicationRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(repository, currentUserService);
    }

    // ---------- helpers ----------

    private static ApplicationForm form(String company, String title) {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName(company);
        form.setJobTitle(title);
        return form;
    }

    /** Pretends application #id exists AND belongs to the current user. */
    private JobApplication givenOwnedApplication(long id) {
        JobApplication app = new JobApplication("Safaricom", "Java Developer", "Nairobi", LocalDate.of(2026, 10, 20));
        app.setStatus(Status.APPLIED);
        when(currentUserService.currentUserId()).thenReturn(USER_ID);
        when(repository.findByIdAndOwnerId(id, USER_ID)).thenReturn(Optional.of(app));
        return app;
    }

    /** Pretends application #id doesn't exist for the current user (missing, or someone else's). */
    private void givenNotOwned(long id) {
        when(currentUserService.currentUserId()).thenReturn(USER_ID);
        when(repository.findByIdAndOwnerId(id, USER_ID)).thenReturn(Optional.empty());
    }

    // ---------- tests ----------

    @Nested
    class Creating {

        private User owner;

        @BeforeEach
        void givenALoggedInUser() {
            owner = new User("tracey@example.com", "Tracey", "{noop}x");
            when(currentUserService.currentUser()).thenReturn(owner);
            // save() returns whatever it was given, like the real repository
            when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void withADateItStartsAsAppliedAndBelongsToTheUser() {
            ApplicationForm form = form("Andela", "Backend Engineer");
            form.setApplicationDate(LocalDate.of(2026, 10, 25));
            form.setRecruiterName("Brian Otieno");
            form.setRecruiterEmail("brian@example.com");

            JobApplication created = service.create(form);

            assertThat(created.getStatus()).isEqualTo(Status.APPLIED);
            assertThat(created.getOwner()).isSameAs(owner);
            assertThat(owner.getApplications()).containsExactly(created);
            assertThat(created.getRecruiter()).isEqualTo(new RecruiterContact("Brian Otieno", "brian@example.com", null));
        }

        @Test
        void withoutADateItStaysSavedAndBlankFieldsBecomeNull() {
            ApplicationForm form = form("Twiga Foods", "Full-Stack Developer");
            form.setLocation("   ");

            JobApplication created = service.create(form);

            assertThat(created.getStatus()).isEqualTo(Status.SAVED);
            assertThat(created.getLocation()).isNull();
            assertThat(created.getRecruiter()).isNull();
        }
    }

    @Nested
    class Updating {

        @Test
        void changesAreAppliedWithoutAnExplicitSave() {
            JobApplication app = givenOwnedApplication(5L);
            ApplicationForm form = ApplicationForm.from(app);
            form.setJobTitle("Senior Java Developer");

            JobApplication updated = service.update(5L, form);

            assertThat(updated.getJobTitle()).isEqualTo("Senior Java Developer");
            assertThat(updated.getStatus()).isEqualTo(Status.APPLIED); // the form never changes status
            verify(repository, never()).save(any()); // dirty checking does the work
        }

        @Test
        void someoneElsesApplicationIsNotFound() {
            givenNotOwned(99L);

            assertThatThrownBy(() -> service.update(99L, form("X", "Y")))
                    .isInstanceOf(ApplicationNotFoundException.class)
                    .hasMessage("No application with id 99");
        }
    }

    @Nested
    class Deleting {

        @Test
        void deletesAndDescribesWhatWasDeleted() {
            JobApplication app = givenOwnedApplication(5L);

            ApplicationSummary deleted = service.delete(5L);

            verify(repository).delete(app);
            assertThat(deleted.companyName()).isEqualTo("Safaricom");
            assertThat(deleted.jobTitle()).isEqualTo("Java Developer");
        }

        @Test
        void someoneElsesApplicationIsNeverDeleted() {
            givenNotOwned(42L);

            assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(ApplicationNotFoundException.class);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    class Reading {

        @Test
        void listAsksForTheCurrentUsersApplicationsOnly() {
            when(currentUserService.currentUserId()).thenReturn(USER_ID);
            when(repository.findAllForOwner(USER_ID)).thenReturn(List.of(
                    new JobApplication("Safaricom", "Java Developer", "Nairobi", null)));

            List<ApplicationSummary> list = service.listForCurrentUser(null);

            assertThat(list).extracting(ApplicationSummary::companyName).containsExactly("Safaricom");
        }

        @Test
        void filteringByStatusUsesTheStatusQuery() {
            when(currentUserService.currentUserId()).thenReturn(USER_ID);

            service.listForCurrentUser(Status.OFFER);

            verify(repository).findByOwnerIdAndStatusOrderByApplicationDateDesc(USER_ID, Status.OFFER);
            verify(repository, never()).findAllForOwner(any());
        }
    }
}