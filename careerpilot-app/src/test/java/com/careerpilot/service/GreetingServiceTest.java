package com.careerpilot.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A plain unit test: no Spring at all, just a fixed Clock. Runs in milliseconds. */
class GreetingServiceTest {

    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");

    private GreetingService at(int hour) {
        Instant instant = LocalDate.of(2026, 11, 9).atTime(hour, 0).atZone(NAIROBI).toInstant();
        return new GreetingService(Clock.fixed(instant, NAIROBI));
    }

    @ParameterizedTest(name = "{0}:00 -> {1}")
    @CsvSource({
            "0,  Good morning",
            "11, Good morning",
            "12, Good afternoon",
            "16, Good afternoon",
            "17, Good evening",
            "23, Good evening"
    })
    void greetsByTimeOfDay(int hour, String expected) {
        assertEquals(expected, at(hour).greeting());
    }
}