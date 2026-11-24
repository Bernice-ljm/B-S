package edu.zju.approval.workflow.runtime;

public class InvalidWorkflowTransitionException extends IllegalStateException {

    public InvalidWorkflowTransitionException(String message) {
        super(message);
    }
}
