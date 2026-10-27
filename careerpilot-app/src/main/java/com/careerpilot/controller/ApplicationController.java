package com.careerpilot.controller;

import com.careerpilot.model.Status;
import com.careerpilot.service.JobApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final JobApplicationService jobApplicationService;

    public ApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

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
}