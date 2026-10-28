package com.careerpilot;

import com.careerpilot.config.DataSeeder;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import com.careerpilot.model.User;
import com.careerpilot.repository.JobApplicationRepository;
import com.careerpilot.security.CareerPilotUserDetails;
import com.careerpilot.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real HTTP flow through security → controller → service → JPA → PostgreSQL (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApplicationCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JobApplicationRepository repository;

    private CareerPilotUserDetails newUser() {
        User user = registrationService.register("Test User", "user-" + UUID.randomUUID() + "@example.com", "Password123");
        return CareerPilotUserDetails.from(user);
    }

    @Test
    void createViewEditAndDelete() throws Exception {
        CareerPilotUserDetails me = newUser();

        // Create
        String location = mockMvc.perform(post("/applications").with(csrf()).with(user(me))
                        .param("companyName", "Andela")
                        .param("jobTitle", "Backend Engineer")
                        .param("location", "Remote")
                        .param("recruiterName", "Brian Otieno"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        // View
        mockMvc.perform(get("/applications/" + id).with(user(me)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Backend Engineer")))
                .andExpect(content().string(containsString("Brian Otieno")));

        // Edit
        mockMvc.perform(post("/applications/" + id).with(csrf()).with(user(me))
                        .param("companyName", "Andela")
                        .param("jobTitle", "Senior Backend Engineer")
                        .param("applicationDate", "2026-10-01"))
                .andExpect(redirectedUrl("/applications/" + id));

        JobApplication saved = repository.findById(id).orElseThrow();
        assertThat(saved.getJobTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(saved.getRecruiter()).isNull(); // cleared because the edit omitted it
        assertThat(saved.getStatus()).isEqualTo(Status.SAVED); // editing never changes status

        // Delete
        mockMvc.perform(post("/applications/" + id + "/delete").with(csrf()).with(user(me)))
                .andExpect(redirectedUrl("/applications"));

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void strangersCannotEditOrDeleteSomeoneElsesApplication() throws Exception {
        CareerPilotUserDetails stranger = newUser();
        var demo = (CareerPilotUserDetails) userDetailsService.loadUserByUsername(DataSeeder.DEMO_EMAIL);
        JobApplication target = repository.findAllForOwner(demo.getId()).get(0);
        long id = target.getId();
        String originalTitle = target.getJobTitle();

        mockMvc.perform(get("/applications/" + id + "/edit").with(user(stranger)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/applications/" + id).with(csrf()).with(user(stranger))
                        .param("companyName", "Hacked")
                        .param("jobTitle", "Hacked"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/applications/" + id + "/delete").with(csrf()).with(user(stranger)))
                .andExpect(status().isNotFound());

        JobApplication unchanged = repository.findById(id).orElseThrow();
        assertThat(unchanged.getJobTitle()).isEqualTo(originalTitle);
    }
}