package com.careerpilot.exception;

public class EmailAlreadyRegisteredException extends CareerPilotException {

    private final String email;

    public EmailAlreadyRegisteredException(String email) {
        super("An account with email " + email + " already exists");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}