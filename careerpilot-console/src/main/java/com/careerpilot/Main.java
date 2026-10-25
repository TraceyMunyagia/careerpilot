package com.careerpilot;

import com.careerpilot.data.SeedData;
import com.careerpilot.repository.JobApplicationRepository;
import com.careerpilot.service.JobApplicationService;
import com.careerpilot.ui.ConsoleMenu;
import com.careerpilot.ui.InputHelper;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // Manual "dependency injection". Spring Boot will do this wiring for you from Week 3.
        JobApplicationRepository repository = new JobApplicationRepository();
        JobApplicationService service = new JobApplicationService(repository);
        SeedData.load(service);

        try (Scanner scanner = new Scanner(System.in)) {
            new ConsoleMenu(service, new InputHelper(scanner)).start();
        }
    }
}