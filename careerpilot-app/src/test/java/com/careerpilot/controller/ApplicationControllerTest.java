package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
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

    @Test
    void listShowsFilterChipsAndEmptyState() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/list"))
                .andExpect(content().string(containsString("Interview")))
                .andExpect(content().string(containsString("No applications yet")));
    }

    @Test
    void statusQueryParameterBecomesTheEnum() throws Exception {
        mockMvc.perform(get("/applications").param("status", "OFFER"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStatus", Status.OFFER))
                .andExpect(content().string(containsString("Nothing with status Offer yet.")));
    }

    @Test
    void unknownApplicationShowsThe404Page() throws Exception {
        mockMvc.perform(get("/applications/42"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("message", "No application with id 42"));
    }

    @Test
    void invalidStatusShowsThe400Page() throws Exception {
        mockMvc.perform(get("/applications").param("status", "BANANA"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/4xx"))
                .andExpect(content().string(containsString("BANANA")));
    }

    @Test
    void nonNumericIdShowsThe400Page() throws Exception {
        mockMvc.perform(get("/applications/abc"))
                .andExpect(status().isBadRequest());
    }
}