package com.careerpilot.model;


import java.util.EnumSet;
import java.util.Set;

/** Every stage a job application can be in. */
public enum Status {
    SAVED("Saved"),
    APPLIED("Applied"),
    INTERVIEW("Interview"),
    REJECTED("Rejected"),
    OFFER("Offer");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Applied or interviewing, meaning still in play. */
    public boolean isActive() {
        return this == APPLIED || this == INTERVIEW;
    }

    /** A final outcome has been reached. */
    public boolean isClosed() {
        return this == REJECTED || this == OFFER;
    }

    /** Which statuses this one may move to. Closed statuses can't move. */
    public Set<Status> allowedNext() {
        return switch (this) {
            case SAVED -> EnumSet.of(APPLIED, REJECTED);   // REJECTED here means "dropped it"
            case APPLIED -> EnumSet.of(INTERVIEW, REJECTED, OFFER);
            case INTERVIEW -> EnumSet.of(REJECTED, OFFER);
            case REJECTED, OFFER -> EnumSet.noneOf(Status.class);
        };
    }

    public boolean canMoveTo(Status next) {
        return allowedNext().contains(next);
    }
}