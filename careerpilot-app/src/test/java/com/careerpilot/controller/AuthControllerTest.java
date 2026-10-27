package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.exception.EmailAlreadyRegisteredException;
import com.careerpilot.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest({LoginController.class, RegistrationController.class})
@EnableConfigurationProperties(AppProperties.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------- login page ----------

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Welcome back")));
    }

    @Test
    void loginPageShowsAGenericErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Incorrect email or password.")));
    }

    @Test
    @WithMockUser
    void loggedInUsersAreSentAwayFromTheLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    // ---------- registration ----------

    @Test
    void registrationPageIsPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("Create your account")));
    }

    @Test
    void invalidFormIsRedisplayedWithFieldErrors() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "")
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("form", "fullName", "email", "password", "passwordConfirmed"));

        verify(registrationService, never()).register(any(), any(), any());
    }

    @Test
    void validFormCreatesTheAccountAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Tracey")
                        .param("email", "Tracey@Example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123"))
                .andExpect(redirectedUrl("/login?registered"))
                .andExpect(flash().attribute("registeredEmail", "tracey@example.com"));

        verify(registrationService).register("Tracey", "Tracey@Example.com", "Password123");
    }

    @Test
    void duplicateEmailIsShownAsAFieldError() throws Exception {
        when(registrationService.register(any(), any(), any()))
                .thenThrow(new EmailAlreadyRegisteredException("taken@example.com"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Someone")
                        .param("email", "taken@example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("form", "email", "email.taken"));
    }

    @Test
    void postWithoutACsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Tracey")
                        .param("email", "tracey@example.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123"))
                .andExpect(status().isForbidden());
    }
}