package edu.zju.approval.workflow.runtime;

public enum WorkflowNodeStatus {
    WAITING,
    PENDING,
    APPROVED,
    REJECTED,
    SKIPPED,
    CANCELLED
}
