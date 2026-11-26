package edu.zju.approval.authorization;

import edu.zju.approval.application.domain.Application;
import edu.zju.approval.workflow.runtime.WorkflowInstance;

import java.util.Objects;

public record ApplicationAccessContext(
        Application application,
        WorkflowInstance workflowInstance,
        ApplicationAccessRelationship relationship) {

    public ApplicationAccessContext {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(relationship, "relationship");
    }

    public static ApplicationAccessContext withoutExplicitGrant(
            Application application, WorkflowInstance workflowInstance) {
        return new ApplicationAccessContext(application, workflowInstance, ApplicationAccessRelationship.NONE);
    }
}
