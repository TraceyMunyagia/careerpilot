package com.careerpilot.controller;

import com.careerpilot.dto.RegistrationForm;
import com.careerpilot.exception.EmailAlreadyRegisteredException;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public String form(Model model) {
        if (SecurityUtils.isLoggedIn()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    /**
     * Post/Redirect/Get: on success, redirect, so refreshing the next page can't resubmit the form.
     * BindingResult must come directly after the @Valid parameter.
     */
    @PostMapping
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register"; // same page, with errors and the user's input
        }

        try {
            registrationService.register(form.getFullName(), form.getEmail(), form.getPassword());
        } catch (EmailAlreadyRegisteredException e) {
            bindingResult.rejectValue("email", "email.taken", "An account with this email already exists");
            return "auth/register";
        }

        // Flash attributes survive exactly one redirect, then disappear
        redirectAttributes.addFlashAttribute("registeredEmail", form.getEmail().trim().toLowerCase());
        return "redirect:/login?registered";
    }
}