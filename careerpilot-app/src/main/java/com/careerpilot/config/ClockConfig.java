package com.careerpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Same idea as Week 2: never call LocalDate.now() in business code.
 * Any bean that needs the time asks for a Clock, and tests swap in a fixed one.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(AppProperties appProperties) {
        return Clock.system(ZoneId.of(appProperties.timeZone()));
    }
}