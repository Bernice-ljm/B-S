package edu.zju.approval.authorization;

import edu.zju.approval.application.domain.Application;
import edu.zju.approval.workflow.runtime.WorkflowInstance;

import java.util.Objects;

public record ApplicationAccessContext(
        Application application,
        WorkflowAccessSnapshot workflowAccess,
        ApplicationAccessRelationship relationship) {

    public ApplicationAccessContext {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(relationship, "relationship");
    }

    public ApplicationAccessContext(
            Application application, WorkflowInstance workflowInstance,
            ApplicationAccessRelationship relationship) {
        this(application, WorkflowAccessSnapshot.fromDomain(workflowInstance), relationship);
    }

    public static ApplicationAccessContext withoutExplicitGrant(
            Application application, WorkflowInstance workflowInstance) {
        return new ApplicationAccessContext(application, workflowInstance, ApplicationAccessRelationship.NONE);
    }

    public static ApplicationAccessContext withoutExplicitGrant(
            Application application, WorkflowAccessSnapshot workflowAccess) {
        return new ApplicationAccessContext(application, workflowAccess, ApplicationAccessRelationship.NONE);
    }
}
