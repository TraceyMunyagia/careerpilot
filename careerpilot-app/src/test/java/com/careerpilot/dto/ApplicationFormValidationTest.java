package com.careerpilot.dto;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Validation rules tested directly with Hibernate Validator. No Spring, runs in milliseconds. */
class ApplicationFormValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static ApplicationForm validForm() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("Safaricom");
        form.setJobTitle("Junior Java Developer");
        return form;
    }

    /** The names of the properties that failed validation. */
    private static Set<String> invalidFields(ApplicationForm form) {
        return validator.validate(form).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void companyAndTitleAloneAreEnough() {
        assertThat(invalidFields(validForm())).isEmpty();
    }

    @Test
    void companyAndTitleAreRequired() {
        ApplicationForm form = new ApplicationForm();

        assertThat(invalidFields(form)).containsExactlyInAnyOrder("companyName", "jobTitle");
    }

    @Test
    void linkMustBeAFullWebAddress() {
        ApplicationForm form = validForm();
        form.setApplicationLink("careers.safaricom.co.ke");

        assertThat(invalidFields(form)).containsExactly("applicationLink");

        form.setApplicationLink("https://careers.safaricom.co.ke/jobs/123");
        assertThat(invalidFields(form)).isEmpty();
    }

    @Test
    void dateAppliedCannotBeInTheFuture() {
        ApplicationForm form = validForm();
        form.setApplicationDate(LocalDate.now().plusDays(1));

        assertThat(invalidFields(form)).containsExactly("applicationDate");
    }

    @Test
    void recruiterContactDetailsNeedAName() {
        ApplicationForm form = validForm();
        form.setRecruiterEmail("jane@example.com");

        assertThat(invalidFields(form)).containsExactly("recruiterNameProvided");

        form.setRecruiterName("Jane Wanjiku");
        assertThat(invalidFields(form)).isEmpty();
    }

    @Test
    void recruiterEmailAndPhoneAreChecked() {
        ApplicationForm form = validForm();
        form.setRecruiterName("Jane Wanjiku");
        form.setRecruiterEmail("not-an-email");
        form.setRecruiterPhone("call me maybe");

        assertThat(invalidFields(form)).containsExactlyInAnyOrder("recruiterEmail", "recruiterPhone");
    }

    @Test
    void recruiterIsNullWhenAllFieldsAreEmpty() {
        assertThat(validForm().toRecruiter()).isNull();
    }

    @Test
    void recruiterIsBuiltFromTheFields() {
        ApplicationForm form = validForm();
        form.setRecruiterName("Jane Wanjiku");
        form.setRecruiterPhone("+254 700 000 001");

        assertThat(form.toRecruiter()).isEqualTo(new RecruiterContact("Jane Wanjiku", null, "+254 700 000 001"));
    }

    @Test
    void formRoundTripsThroughAnEntity() {
        JobApplication app = new JobApplication("Andela", "Backend Engineer", "Remote", LocalDate.of(2026, 10, 1));
        app.setRecruiter(new RecruiterContact("Brian Otieno", "brian@example.com", null));

        ApplicationForm form = ApplicationForm.from(app);
        form.setJobTitle("Senior Backend Engineer");
        form.applyTo(app);

        assertThat(app.getJobTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(app.getLocation()).isEqualTo("Remote");
        assertThat(app.getRecruiter().email()).isEqualTo("brian@example.com");
    }
}