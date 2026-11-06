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

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A whole application journey through real HTTP, security, JPA and PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JobApplicationRepository repository;

    @Autowired
    private Clock clock; // the app's own Clock bean, so "today" matches the server

    private CareerPilotUserDetails newUser() {
        User user = registrationService.register("Test User", "user-" + UUID.randomUUID() + "@example.com", "Password123");
        return CareerPilotUserDetails.from(user);
    }

    /** Creates a Saved application through the real form and returns its id. */
    private long createApplication(CareerPilotUserDetails owner) throws Exception {
        String location = mockMvc.perform(post("/applications").with(csrf()).with(user(owner))
                        .param("companyName", "Andela")
                        .param("jobTitle", "Backend Engineer"))
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void fromSavedToOffer() throws Exception {
        CareerPilotUserDetails me = newUser();
        long id = createApplication(me);
        LocalDate today = LocalDate.now(clock);
        String url = "/applications/" + id;

        mockMvc.perform(post(url + "/status").with(csrf()).with(user(me)).param("status", "APPLIED"))
                .andExpect(redirectedUrl(url));
        mockMvc.perform(post(url + "/interview").with(csrf()).with(user(me)).param("date", today.plusDays(3).toString()))
                .andExpect(flash().attributeExists("successMessage"));
        mockMvc.perform(post(url + "/follow-up").with(csrf()).with(user(me)).param("date", today.plusDays(5).toString()))
                .andExpect(flash().attributeExists("successMessage"));
        mockMvc.perform(post(url + "/notes").with(csrf()).with(user(me)).param("note", "Prepared STAR stories"))
                .andExpect(flash().attribute("successMessage", "Note added."));
        mockMvc.perform(post(url + "/status").with(csrf()).with(user(me)).param("status", "OFFER"))
                .andExpect(flash().attribute("successMessage", "Status changed to Offer."));

        JobApplication saved = repository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.OFFER);
        assertThat(saved.getApplicationDate()).isEqualTo(today);          // stamped when it became Applied
        assertThat(saved.getInterviewDate()).isEqualTo(today.plusDays(3));
        assertThat(saved.getFollowUpDate()).isNull();                     // cleared when it closed
        assertThat(saved.getNotes().split("\n")).hasSize(4)
                .anyMatch(line -> line.endsWith("Status changed from Saved to Applied"))
                .anyMatch(line -> line.contains("Interview scheduled for"))
                .anyMatch(line -> line.endsWith("Prepared STAR stories"))
                .anyMatch(line -> line.endsWith("Status changed from Interview to Offer"));

        mockMvc.perform(get(url).with(user(me)))
                .andExpect(content().string(containsString("Final outcome reached")))
                .andExpect(content().string(containsString("Prepared STAR stories")));
    }

    @Test
    void illegalMovesAreRefusedAndNothingChanges() throws Exception {
        CareerPilotUserDetails me = newUser();
        long id = createApplication(me);

        mockMvc.perform(post("/applications/" + id + "/status").with(csrf()).with(user(me)).param("status", "OFFER"))
                .andExpect(redirectedUrl("/applications/" + id))
                .andExpect(flash().attribute("errorMessage", containsString("Cannot move from Saved to Offer")));

        JobApplication unchanged = repository.findById(id).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(Status.SAVED);
        assertThat(unchanged.getNotes()).isEmpty();
    }

    @Test
    void strangersCannotRunWorkflowActions() throws Exception {
        CareerPilotUserDetails stranger = newUser();
        var demo = (CareerPilotUserDetails) userDetailsService.loadUserByUsername(DataSeeder.DEMO_EMAIL);
        JobApplication target = repository.findAllForOwner(demo.getId()).get(0);
        String url = "/applications/" + target.getId();
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

        mockMvc.perform(post(url + "/status").with(csrf()).with(user(stranger)).param("status", "REJECTED"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(url + "/interview").with(csrf()).with(user(stranger)).param("date", tomorrow.toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(url + "/follow-up").with(csrf()).with(user(stranger)).param("date", tomorrow.toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(url + "/notes").with(csrf()).with(user(stranger)).param("note", "sneaky"))
                .andExpect(status().isNotFound());

        JobApplication unchanged = repository.findById(target.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(target.getStatus());
        assertThat(unchanged.getNotes()).isEqualTo(target.getNotes());
    }
}