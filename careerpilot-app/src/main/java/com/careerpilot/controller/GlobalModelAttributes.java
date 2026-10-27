package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the same attributes to every page, so no controller has to repeat them. */
@ControllerAdvice
public class GlobalModelAttributes {

    private final AppProperties appProperties;

    public GlobalModelAttributes(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @ModelAttribute("app")
    public AppProperties app() {
        return appProperties;
    }
}