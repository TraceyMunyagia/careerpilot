package com.careerpilot.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalTime;

@Service
public class GreetingService {

    private final Clock clock;

    // One constructor, so Spring injects the Clock bean automatically (no @Autowired needed)
    public GreetingService(Clock clock) {
        this.clock = clock;
    }

    public String greeting() {
        int hour = LocalTime.now(clock).getHour();
        if (hour < 12) {
            return "Good morning";
        }
        if (hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }
}