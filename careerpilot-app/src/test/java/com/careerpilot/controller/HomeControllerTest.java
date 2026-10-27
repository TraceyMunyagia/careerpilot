package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.service.GreetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * A "slice" test: starts only the web layer (controllers, advice, Thymeleaf), with no database.
 * Services are replaced by Mockito mocks.
 */
@WebMvcTest(HomeController.class)
@EnableConfigurationProperties(AppProperties.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GreetingService greetingService;

    @Test
    void homePageShowsGreetingAndPipeline() throws Exception {
        when(greetingService.greeting()).thenReturn("Good evening");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("greeting", "Good evening"))
                .andExpect(model().attributeExists("statuses", "app"))
                .andExpect(content().string(containsString("Good evening")))
                .andExpect(content().string(containsString("Interview")));
    }
}