package com.careerpilot.controller;

import com.careerpilot.config.AppProperties;
import com.careerpilot.dto.CurrentUserView;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /** null when nobody is logged in, so templates can check ${currentUser != null}. */
    @ModelAttribute("currentUser")
    public CurrentUserView currentUser() {
        return CurrentUserView.from(SecurityContextHolder.getContext().getAuthentication());
    }
}