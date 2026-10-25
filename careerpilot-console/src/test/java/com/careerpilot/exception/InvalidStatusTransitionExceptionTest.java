package com.careerpilot.exception;

import com.careerpilot.model.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvalidStatusTransitionExceptionTest {

    @Test
    void messageForTheSameStatus() {
        var ex = new InvalidStatusTransitionException(Status.APPLIED, Status.APPLIED);
        assertEquals("Application is already Applied", ex.getMessage());
    }

    @Test
    void messageNamesBothStatuses() {
        var ex = new InvalidStatusTransitionException(Status.SAVED, Status.OFFER);
        assertTrue(ex.getMessage().startsWith("Cannot move from Saved to Offer"));
        assertEquals(Status.SAVED, ex.getFrom());
        assertEquals(Status.OFFER, ex.getTo());
    }

    @Test
    void allCustomExceptionsShareTheBaseClass() {
        assertInstanceOf(CareerPilotException.class, new ApplicationNotFoundException(1));
        assertInstanceOf(CareerPilotException.class, new BusinessRuleException("x"));
    }
}