package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.service.GreetingService;
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
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@EnableConfigurationProperties(AppProperties.class)
@Import(SecurityConfig.class) // slices don't scan @Configuration classes, so bring in your rules
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GreetingService greetingService;

    @MockitoBean // remember-me needs a UserDetailsService; the real one isn't in this slice
    private UserDetailsService userDetailsService;

    @Test
    void homePageIsPublicAndShowsGreetingAndPipeline() throws Exception {
        when(greetingService.greeting()).thenReturn("Good evening");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("greeting", "Good evening"))
                .andExpect(model().attributeExists("statuses", "app"))
                .andExpect(content().string(containsString("Good evening")))
                .andExpect(content().string(containsString("Interview")));
    }

    @Test
    void anonymousVisitorsSeeSignUpButtons() throws Exception {
        when(greetingService.greeting()).thenReturn("Good morning");

        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("Create free account")))
                .andExpect(content().string(not(containsString("Log out"))));
    }

    @Test
    @WithMockUser(username = "tracey@example.com")
    void loggedInUsersSeeAppLinksAndLogout() throws Exception {
        when(greetingService.greeting()).thenReturn("Good morning");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My applications")))
                .andExpect(content().string(containsString("Log out")))
                .andExpect(content().string(not(containsString("Create free account"))));
    }
}