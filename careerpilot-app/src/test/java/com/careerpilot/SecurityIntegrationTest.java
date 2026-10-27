package com.careerpilot;

import com.careerpilot.config.DataSeeder;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The real thing: full app, real SecurityConfig, real PostgreSQL (Testcontainers),
 * real BCrypt. Proves the pieces work together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    /** Tests share one database, so every new user needs a unique email. */
    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void anonymousVisitorsAreSentToLogin() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login")); // Spring Security 7 redirects with a relative URL
    }

    @Test
    void demoUserCanLogInWithEmailAndPassword() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", DataSeeder.DEMO_EMAIL)
                        .password(DataSeeder.DEMO_PASSWORD))
                .andExpect(authenticated().withUsername(DataSeeder.DEMO_EMAIL))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", DataSeeder.DEMO_EMAIL)
                        .password("not-the-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void registeringThroughTheFormThenLoggingInWorks() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "New Person")
                        .param("email", email)
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123"))
                .andExpect(redirectedUrl("/login?registered"));

        mockMvc.perform(formLogin("/login").user("email", email).password("Password123"))
                .andExpect(authenticated().withUsername(email));
    }

    @Test
    void demoUserSeesTheirSeededApplications() throws Exception {
        var demo = userDetailsService.loadUserByUsername(DataSeeder.DEMO_EMAIL);

        mockMvc.perform(get("/applications").with(user(demo)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Safaricom")));
    }

    @Test
    void newUsersCannotSeeAnyoneElsesApplications() throws Exception {
        User newcomer = registrationService.register("New Person", uniqueEmail(), "Password123");
        CareerPilotUserDetails principal = CareerPilotUserDetails.from(newcomer);
        Long demoApplicationId = jobApplicationRepository.findAll().get(0).getId();

        mockMvc.perform(get("/applications").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No applications yet")));

        mockMvc.perform(get("/applications/" + demoApplicationId).with(user(principal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        var demo = userDetailsService.loadUserByUsername(DataSeeder.DEMO_EMAIL);

        mockMvc.perform(post("/logout").with(csrf()).with(user(demo)))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
    }

    @Test
    void securityHeadersAreSentOnEveryPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }
}