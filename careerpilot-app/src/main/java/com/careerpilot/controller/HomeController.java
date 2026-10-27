package com.careerpilot.controller;

import com.careerpilot.model.Status;
import com.careerpilot.service.GreetingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final GreetingService greetingService;

    public HomeController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("greeting", greetingService.greeting());
        model.addAttribute("statuses", Status.values());
        return "home";
    }
}