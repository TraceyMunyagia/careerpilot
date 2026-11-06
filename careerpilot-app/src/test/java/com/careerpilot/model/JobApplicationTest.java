package com.careerpilot.model;

import com.careerpilot.exception.BusinessRuleException;
import com.careerpilot.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The workflow rules, tested directly on the entity. No Spring, no database, no mocks. */
class JobApplicationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    private static JobApplication saved() {
        return new JobApplication("Safaricom", "Java Developer", "Nairobi", null);
    }

    private static JobApplication withStatus(Status status) {
        JobApplication app = new JobApplication("Safaricom", "Java Developer", "Nairobi", TODAY.minusDays(10));
        app.setStatus(status);
        return app;
    }

    @Nested
    class ChangingStatus {

        @Test
        void movingToAppliedStampsTodayWhenThereIsNoDate() {
            JobApplication app = saved();

            app.changeStatus(Status.APPLIED, TODAY);

            assertThat(app.getStatus()).isEqualTo(Status.APPLIED);
            assertThat(app.getApplicationDate()).isEqualTo(TODAY);
        }

        @Test
        void movingToAppliedKeepsAnExistingDate() {
            JobApplication app = new JobApplication("Andela", "Engineer", null, TODAY.minusDays(3)); // still SAVED

            app.changeStatus(Status.APPLIED, TODAY);

            assertThat(app.getApplicationDate()).isEqualTo(TODAY.minusDays(3));
        }

        @Test
        void illegalMovesAreRejectedAndChangeNothing() {
            JobApplication app = saved();

            assertThatThrownBy(() -> app.changeStatus(Status.OFFER, TODAY))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageStartingWith("Cannot move from Saved to Offer");

            assertThat(app.getStatus()).isEqualTo(Status.SAVED);
            assertThat(app.getApplicationDate()).isNull();
        }

        @Test
        void movingToTheSameStatusIsRejected() {
            JobApplication app = withStatus(Status.APPLIED);

            assertThatThrownBy(() -> app.changeStatus(Status.APPLIED, TODAY))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Application is already Applied");
        }

        @Test
        void closingAnApplicationClearsItsFollowUp() {
            JobApplication app = withStatus(Status.APPLIED);
            app.setFollowUpDate(TODAY.plusDays(2));

            app.changeStatus(Status.REJECTED, TODAY);

            assertThat(app.getFollowUpDate()).isNull();
        }

        @Test
        void aMissingStatusIsRejected() {
            JobApplication app = saved();

            assertThatThrownBy(() -> app.changeStatus(null, TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Choose a status");
        }
    }

    @Nested
    class SchedulingInterviews {

        @Test
        void schedulingMovesAppliedToInterview() {
            JobApplication app = withStatus(Status.APPLIED);

            app.scheduleInterview(TODAY.plusDays(3), TODAY);

            assertThat(app.getStatus()).isEqualTo(Status.INTERVIEW);
            assertThat(app.getInterviewDate()).isEqualTo(TODAY.plusDays(3));
        }

        @Test
        void reschedulingKeepsTheInterviewStatus() {
            JobApplication app = withStatus(Status.INTERVIEW);
            app.setInterviewDate(TODAY.plusDays(1));

            app.scheduleInterview(TODAY.plusDays(7), TODAY);

            assertThat(app.getStatus()).isEqualTo(Status.INTERVIEW);
            assertThat(app.getInterviewDate()).isEqualTo(TODAY.plusDays(7));
        }

        @Test
        void todayIsAllowedButYesterdayIsNot() {
            JobApplication app = withStatus(Status.APPLIED);

            assertThatCode(() -> app.scheduleInterview(TODAY, TODAY)).doesNotThrowAnyException();
            assertThatThrownBy(() -> app.scheduleInterview(TODAY.minusDays(1), TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Interview date cannot be in the past");
        }

        @Test
        void savedApplicationsCannotHaveInterviews() {
            JobApplication app = saved();

            assertThatThrownBy(() -> app.scheduleInterview(TODAY.plusDays(1), TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("this one is Saved");
            assertThat(app.getInterviewDate()).isNull();
        }

        @Test
        void closedApplicationsCannotHaveInterviews() {
            JobApplication app = withStatus(Status.OFFER);

            assertThatThrownBy(() -> app.scheduleInterview(TODAY.plusDays(1), TODAY))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void aMissingDateIsRejected() {
            JobApplication app = withStatus(Status.APPLIED);

            assertThatThrownBy(() -> app.scheduleInterview(null, TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Choose an interview date");
        }
    }

    @Nested
    class FollowUps {

        @Test
        void aFollowUpCanBeSetAndCleared() {
            JobApplication app = withStatus(Status.APPLIED);

            app.scheduleFollowUp(TODAY.plusDays(5), TODAY);
            assertThat(app.getFollowUpDate()).isEqualTo(TODAY.plusDays(5));

            app.scheduleFollowUp(null, TODAY);
            assertThat(app.getFollowUpDate()).isNull();
        }

        @Test
        void pastDatesAreRejected() {
            JobApplication app = withStatus(Status.APPLIED);

            assertThatThrownBy(() -> app.scheduleFollowUp(TODAY.minusDays(1), TODAY))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void closedApplicationsNeedNoFollowUp() {
            JobApplication app = withStatus(Status.REJECTED);

            assertThatThrownBy(() -> app.scheduleFollowUp(TODAY.plusDays(1), TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("No follow-up needed: this application is Rejected");
        }
    }

    @Nested
    class Notes {

        @Test
        void notesAreDatedAndNewestIsLast() {
            JobApplication app = saved();

            app.addNote("First", TODAY.minusDays(1));
            app.addNote("Second", TODAY);

            assertThat(app.getNotes()).isEqualTo("[2026-11-15] First\n[2026-11-16] Second");
        }

        @Test
        void lineBreaksInsideANoteBecomeSpaces() {
            JobApplication app = saved();

            app.addNote("  Called Jane.\n\nShe'll reply on Friday.  ", TODAY);

            assertThat(app.getNotes()).isEqualTo("[2026-11-16] Called Jane. She'll reply on Friday.");
        }

        @Test
        void blankNotesAreRejected() {
            JobApplication app = saved();

            assertThatThrownBy(() -> app.addNote("   ", TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Write something before adding a note");
        }

        @Test
        void notesHaveAMaximumLength() {
            JobApplication app = saved();

            assertThatCode(() -> app.addNote("x".repeat(JobApplication.MAX_NOTE_LENGTH), TODAY))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> app.addNote("x".repeat(JobApplication.MAX_NOTE_LENGTH + 1), TODAY))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}