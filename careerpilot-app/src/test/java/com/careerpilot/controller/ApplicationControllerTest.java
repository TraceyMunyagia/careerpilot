package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.ApplicationForm;
import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.service.JobApplicationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ApplicationController.class)
@EnableConfigurationProperties(AppProperties.class)
@Import(SecurityConfig.class)
@WithMockUser
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------- helpers ----------

    private static ApplicationSummary summary(long id, String company, Status status) {
        return new ApplicationSummary(id, company, "Java Developer", "Nairobi", status,
                LocalDate.of(2026, 11, 2), null, null);
    }

    /** Entities only get ids from the database, so set one by reflection for tests. */
    private static JobApplication application(long id, String company, String title) {
        JobApplication app = new JobApplication(company, title, "Nairobi", LocalDate.of(2026, 11, 2));
        ReflectionTestUtils.setField(app, "id", id);
        return app;
    }

    // ---------- tests ----------

    @Nested
    class Listing {

        @Test
        void emptyListShowsTheEmptyState() throws Exception {
            when(jobApplicationService.listForCurrentUser(null)).thenReturn(List.of());

            mockMvc.perform(get("/applications"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("applications/list"))
                    .andExpect(content().string(containsString("No applications yet")));
        }

        @Test
        void listRendersATableWithLinks() throws Exception {
            when(jobApplicationService.listForCurrentUser(null)).thenReturn(List.of(
                    summary(1, "Safaricom", Status.APPLIED),
                    summary(2, "Andela", Status.INTERVIEW)));

            mockMvc.perform(get("/applications"))
                    .andExpect(content().string(containsString("Safaricom")))
                    .andExpect(content().string(containsString("/applications/2")))
                    .andExpect(content().string(containsString("/applications/new")));
        }

        @Test
        void statusFilterIsPassedToTheService() throws Exception {
            mockMvc.perform(get("/applications").param("status", "OFFER"))
                    .andExpect(model().attribute("selectedStatus", Status.OFFER))
                    .andExpect(content().string(containsString("Nothing with status Offer yet.")));

            verify(jobApplicationService).listForCurrentUser(Status.OFFER);
        }

        @Test
        void invalidStatusShowsThe400Page() throws Exception {
            mockMvc.perform(get("/applications").param("status", "BANANA"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("error/4xx"));
        }
    }

    @Nested
    class Viewing {

        @Test
        void detailShowsTheApplicationAndItsActions() throws Exception {
            JobApplication app = application(5, "Safaricom", "Junior Java Developer");
            app.setRecruiter(new RecruiterContact("Jane Wanjiku", "jane@example.com", null));
            when(jobApplicationService.getForCurrentUser(5)).thenReturn(app);

            mockMvc.perform(get("/applications/5"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("applications/detail"))
                    .andExpect(content().string(containsString("Junior Java Developer")))
                    .andExpect(content().string(containsString("mailto:jane@example.com")))
                    .andExpect(content().string(containsString("/applications/5/edit")))
                    .andExpect(content().string(containsString("/applications/5/delete")));
        }

        @Test
        void unknownApplicationShowsThe404Page() throws Exception {
            when(jobApplicationService.getForCurrentUser(42)).thenThrow(new ApplicationNotFoundException(42));

            mockMvc.perform(get("/applications/42"))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("error/404"));
        }
    }

    @Nested
    class Creating {

        @Test
        void newFormIsEmpty() throws Exception {
            mockMvc.perform(get("/applications/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("applications/form"))
                    .andExpect(model().attribute("editing", false))
                    .andExpect(content().string(containsString("Add an application")));
        }

        @Test
        void invalidFormIsShownAgainWithFieldErrors() throws Exception {
            mockMvc.perform(post("/applications").with(csrf())
                            .param("companyName", "   ")
                            .param("jobTitle", "")
                            .param("applicationLink", "careers.example.com")
                            .param("recruiterEmail", "jane@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("applications/form"))
                    .andExpect(model().attributeHasFieldErrors("applicationForm",
                            "companyName", "jobTitle", "applicationLink", "recruiterNameProvided"))
                    .andExpect(content().string(containsString("Please fix the highlighted fields.")));

            verify(jobApplicationService, never()).create(any());
        }

        @Test
        void validFormCreatesAndRedirectsToTheNewApplication() throws Exception {
            when(jobApplicationService.create(any(ApplicationForm.class)))
                    .thenReturn(application(12, "Andela", "Backend Engineer"));

            mockMvc.perform(post("/applications").with(csrf())
                            .param("companyName", "  Andela  ")
                            .param("jobTitle", "Backend Engineer")
                            .param("applicationDate", "2026-10-02"))
                    .andExpect(redirectedUrl("/applications/12"))
                    .andExpect(flash().attribute("successMessage", "Added Backend Engineer at Andela."));

            // @InitBinder trimmed the company name before it reached the service
            verify(jobApplicationService).create(org.mockito.ArgumentMatchers.argThat(
                    form -> form.getCompanyName().equals("Andela")
                            && form.getApplicationDate().equals(LocalDate.of(2026, 10, 2))));
        }

        @Test
        void postWithoutACsrfTokenIsForbidden() throws Exception {
            mockMvc.perform(post("/applications")
                            .param("companyName", "Andela")
                            .param("jobTitle", "Backend Engineer"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class Editing {

        @Test
        void editFormIsPrefilled() throws Exception {
            when(jobApplicationService.getForCurrentUser(5)).thenReturn(application(5, "Safaricom", "Java Developer"));

            mockMvc.perform(get("/applications/5/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("applications/form"))
                    .andExpect(model().attribute("editing", true))
                    .andExpect(model().attribute("applicationForm", hasProperty("companyName", is("Safaricom"))))
                    .andExpect(content().string(containsString("value=\"2026-11-02\"")));
        }

        @Test
        void validChangesAreSavedAndRedirectToDetail() throws Exception {
            mockMvc.perform(post("/applications/5").with(csrf())
                            .param("companyName", "Safaricom")
                            .param("jobTitle", "Senior Java Developer"))
                    .andExpect(redirectedUrl("/applications/5"))
                    .andExpect(flash().attribute("successMessage", "Changes saved."));

            verify(jobApplicationService).update(eq(5L), any(ApplicationForm.class));
        }

        @Test
        void invalidChangesStayOnTheEditForm() throws Exception {
            mockMvc.perform(post("/applications/5").with(csrf())
                            .param("companyName", "")
                            .param("jobTitle", "Senior Java Developer"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("editing", true))
                    .andExpect(model().attribute("applicationId", 5L))
                    .andExpect(content().string(containsString("Edit application")));

            verify(jobApplicationService, never()).update(eq(5L), any());
        }
    }

    @Nested
    class Deleting {

        @Test
        void deleteRedirectsToTheListWithAMessage() throws Exception {
            when(jobApplicationService.delete(5)).thenReturn(summary(5, "Cellulant", Status.REJECTED));

            mockMvc.perform(post("/applications/5/delete").with(csrf()))
                    .andExpect(redirectedUrl("/applications"))
                    .andExpect(flash().attribute("successMessage", "Deleted Java Developer at Cellulant."));
        }

        @Test
        void deletingSomeoneElsesApplicationIs404() throws Exception {
            when(jobApplicationService.delete(42)).thenThrow(new ApplicationNotFoundException(42));

            mockMvc.perform(post("/applications/42/delete").with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteCannotBeTriggeredByAGet() throws Exception {
            mockMvc.perform(get("/applications/5/delete"))
                    .andExpect(status().isMethodNotAllowed());

            verify(jobApplicationService, never()).delete(any(Long.class));
        }
    }

    @Test
    @WithAnonymousUser
    void anonymousVisitorsAreSentToLogin() throws Exception {
        mockMvc.perform(get("/applications/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}