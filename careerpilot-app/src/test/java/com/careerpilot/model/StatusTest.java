package com.careerpilot.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusTest {

    @Test
    void savedCanMoveToApplied() {
        assertTrue(Status.SAVED.canMoveTo(Status.APPLIED));
    }

    @Test
    void savedCannotJumpStraightToOffer() {
        assertFalse(Status.SAVED.canMoveTo(Status.OFFER));
    }

    @Test
    @DisplayName("Closed statuses allow no further moves")
    void closedStatusesAreTerminal() {
        assertTrue(Status.REJECTED.allowedNext().isEmpty());
        assertTrue(Status.OFFER.allowedNext().isEmpty());
    }

    @Test
    void activeAndClosedFlags() {
        assertTrue(Status.APPLIED.isActive());
        assertTrue(Status.INTERVIEW.isActive());
        assertFalse(Status.SAVED.isActive());
        assertTrue(Status.OFFER.isClosed());
        assertFalse(Status.INTERVIEW.isClosed());
    }

    @Test
    void labelsAreHumanReadable() {
        assertEquals("Interview", Status.INTERVIEW.getLabel());
    }

    @ParameterizedTest(name = "{0} -> {1} allowed? {2}")
    @CsvSource({
            "SAVED,     APPLIED,   true",
            "SAVED,     REJECTED,  true",
            "SAVED,     INTERVIEW, false",
            "SAVED,     OFFER,     false",
            "APPLIED,   INTERVIEW, true",
            "APPLIED,   OFFER,     true",
            "APPLIED,   REJECTED,  true",
            "APPLIED,   SAVED,     false",
            "INTERVIEW, OFFER,     true",
            "INTERVIEW, REJECTED,  true",
            "INTERVIEW, APPLIED,   false"
    })
    void transitionRules(Status from, Status to, boolean allowed) {
        assertEquals(allowed, from.canMoveTo(to));
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"REJECTED", "OFFER"})
    void closedStatusesCannotMoveAnywhere(Status closed) {
        for (Status target : Status.values()) {
            assertFalse(closed.canMoveTo(target), closed + " should not move to " + target);
        }
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    void noStatusCanMoveToItself(Status status) {
        assertFalse(status.canMoveTo(status));
    }
}