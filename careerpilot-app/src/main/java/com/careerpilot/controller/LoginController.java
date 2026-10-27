package com.careerpilot.controller;

import com.careerpilot.config.DataSeeder;
import com.careerpilot.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Only shows the page. Spring Security itself handles POST /login
 * (checking the password, creating the session, redirecting).
 */
@Controller
public class LoginController {

    private final boolean seedEnabled;

    public LoginController(@Value("${careerpilot.seed.enabled:false}") boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }

    @GetMapping("/login")
    public String login(Model model) {
        if (SecurityUtils.isLoggedIn()) {
            return "redirect:/dashboard";
        }
        if (seedEnabled) {
            model.addAttribute("demoEmail", DataSeeder.DEMO_EMAIL);
            model.addAttribute("demoPassword", DataSeeder.DEMO_PASSWORD);
        }
        return "auth/login";
    }
}