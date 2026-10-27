package com.careerpilot.dto;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;

import java.time.LocalDate;

/**
 * What a list row needs, and nothing more. Views get plain data, so they can never
 * trigger lazy loading or change an entity by accident.
 */
public record ApplicationSummary(Long id,
                                 String companyName,
                                 String jobTitle,
                                 String location,
                                 Status status,
                                 LocalDate applicationDate,
                                 LocalDate interviewDate,
                                 LocalDate followUpDate) {

    public static ApplicationSummary from(JobApplication app) {
        return new ApplicationSummary(
                app.getId(),
                app.getCompanyName(),
                app.getJobTitle(),
                app.getLocation(),
                app.getStatus(),
                app.getApplicationDate(),
                app.getInterviewDate(),
                app.getFollowUpDate());
    }
}