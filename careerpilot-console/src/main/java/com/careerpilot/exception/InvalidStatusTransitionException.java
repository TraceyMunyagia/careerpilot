package com.careerpilot.exception;

import com.careerpilot.model.Status;

public class InvalidStatusTransitionException extends CareerPilotException {

    private final Status from;
    private final Status to;

    public InvalidStatusTransitionException(Status from, Status to) {
        super(buildMessage(from, to));
        this.from = from;
        this.to = to;
    }

    private static String buildMessage(Status from, Status to) {
        if (from == to) {
            return "Application is already " + from.getLabel();
        }
        return "Cannot move from " + from.getLabel() + " to " + to.getLabel()
                + ". Allowed: " + from.allowedNext();
    }

    public Status getFrom() {
        return from;
    }

    public Status getTo() {
        return to;
    }
}