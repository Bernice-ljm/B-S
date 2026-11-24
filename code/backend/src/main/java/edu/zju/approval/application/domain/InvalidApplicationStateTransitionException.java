package edu.zju.approval.application.domain;

public class InvalidApplicationStateTransitionException extends IllegalStateException {

    public InvalidApplicationStateTransitionException(ApplicationStatus source, ApplicationStatus target) {
        super("Application cannot transition from " + source + " to " + target);
    }
}
