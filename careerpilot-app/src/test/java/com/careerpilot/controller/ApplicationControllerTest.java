package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.RecruiterContact;
import com.careerpilot.model.Status;
import com.careerpilot.service.JobApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ApplicationController.class)
@EnableConfigurationProperties(AppProperties.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    private static ApplicationSummary summary(long id, String company, Status status) {
        return new ApplicationSummary(id, company, "Java Developer", "Nairobi", status,
                LocalDate.of(2026, 11, 2), null, null);
    }

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
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Safaricom")))
                .andExpect(content().string(containsString("Andela")))
                .andExpect(content().string(containsString("/applications/2")))
                .andExpect(content().string(containsString("2 Nov 2026")));
    }

    @Test
    void statusFilterIsPassedToTheService() throws Exception {
        mockMvc.perform(get("/applications").param("status", "OFFER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStatus", Status.OFFER))
                .andExpect(content().string(containsString("Nothing with status Offer yet.")));

        verify(jobApplicationService).listForCurrentUser(Status.OFFER);
    }

    @Test
    void detailShowsTheApplication() throws Exception {
        JobApplication app = new JobApplication("Safaricom", "Junior Java Developer", "Nairobi", LocalDate.of(2026, 11, 2));
        app.setRecruiter(new RecruiterContact("Jane Wanjiku", "jane@example.com", null));
        when(jobApplicationService.getForCurrentUser(5)).thenReturn(app);

        mockMvc.perform(get("/applications/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/detail"))
                .andExpect(content().string(containsString("Junior Java Developer")))
                .andExpect(content().string(containsString("Jane Wanjiku")))
                .andExpect(content().string(containsString("mailto:jane@example.com")));
    }

    @Test
    void unknownApplicationShowsThe404Page() throws Exception {
        when(jobApplicationService.getForCurrentUser(42)).thenThrow(new ApplicationNotFoundException(42));

        mockMvc.perform(get("/applications/42"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("message", "No application with id 42"));
    }

    @Test
    void invalidStatusShowsThe400Page() throws Exception {
        mockMvc.perform(get("/applications").param("status", "BANANA"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/4xx"));
    }
}