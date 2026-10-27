package com.careerpilot.exception;

public class ApplicationNotFoundException extends CareerPilotException {

    private final long applicationId;

    public ApplicationNotFoundException(long applicationId) {
        super("No application with id " + applicationId);
        this.applicationId = applicationId;
    }

    public long getApplicationId() {
        return applicationId;
    }
}