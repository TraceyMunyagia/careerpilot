package com.careerpilot.exception;

/**
 * Base class for expected, user-facing errors.
 * Unchecked (extends RuntimeException) so callers aren't forced to catch it everywhere;
 * the UI layer catches it once.
 */
public class CareerPilotException extends RuntimeException {

    public CareerPilotException(String message) {
        super(message);
    }
}