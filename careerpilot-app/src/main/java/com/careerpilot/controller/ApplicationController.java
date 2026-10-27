package com.careerpilot.controller;

import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.model.Status;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    /** GET /applications or /applications?status=INTERVIEW (Spring converts the text to the enum). */
    @GetMapping
    public String list(@RequestParam(required = false) Status status, Model model) {
        model.addAttribute("statuses", Status.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("applications", List.of()); // real data arrives in Week 6
        return "applications/list";
    }

    /** GET /applications/5. There's no data until Week 4, so every id is "not found" for now. */
    @GetMapping("/{id}")
    public String detail(@PathVariable long id) {
        throw new ApplicationNotFoundException(id);
    }
}