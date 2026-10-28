package com.careerpilot.controller;

import com.careerpilot.dto.ApplicationForm;
import com.careerpilot.dto.ApplicationSummary;
import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;
import com.careerpilot.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final JobApplicationService jobApplicationService;

    public ApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    /**
     * For the application form only: trim every text field and turn empty ones into null.
     * Scoped by name, so password fields on other forms are never trimmed.
     */
    @InitBinder("applicationForm")
    void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    // ---------- read ----------

    /** GET /applications or /applications?status=INTERVIEW */
    @GetMapping
    public String list(@RequestParam(required = false) Status status, Model model) {
        model.addAttribute("statuses", Status.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("applications", jobApplicationService.listForCurrentUser(status));
        return "applications/list";
    }

    /** GET /applications/5. Throws ApplicationNotFoundException (→ 404) if missing or not yours. */
    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        // Not "application": Thymeleaf reserves ${application} for servlet-context attributes
        model.addAttribute("jobApplication", jobApplicationService.getForCurrentUser(id));
        return "applications/detail";
    }

    // ---------- create ----------

    /** GET /applications/new. The literal "/new" beats the "/{id}" pattern. */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("applicationForm", new ApplicationForm());
        model.addAttribute("editing", false);
        return "applications/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("applicationForm") ApplicationForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "applications/form";
        }

        JobApplication created = jobApplicationService.create(form);
        redirectAttributes.addFlashAttribute("successMessage",
                "Added " + created.getJobTitle() + " at " + created.getCompanyName() + ".");
        return "redirect:/applications/" + created.getId();
    }

    // ---------- edit ----------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        JobApplication application = jobApplicationService.getForCurrentUser(id);
        model.addAttribute("applicationForm", ApplicationForm.from(application));
        model.addAttribute("applicationId", id);
        model.addAttribute("editing", true);
        return "applications/form";
    }

    /** POST, not PUT: plain HTML forms can only send GET or POST. */
    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("applicationForm") ApplicationForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("applicationId", id);
            model.addAttribute("editing", true);
            return "applications/form";
        }

        jobApplicationService.update(id, form);
        redirectAttributes.addFlashAttribute("successMessage", "Changes saved.");
        return "redirect:/applications/" + id;
    }
        @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        ApplicationSummary deleted = jobApplicationService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Deleted " + deleted.jobTitle() + " at " + deleted.companyName() + ".");
        return "redirect:/applications";
    }
     
}