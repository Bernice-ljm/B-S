package edu.zju.approval.authorization;

import edu.zju.approval.workflow.runtime.WorkflowTaskStatus;

import java.time.LocalDateTime;

public record PersistedWorkflowTaskAccess(
        long taskId,
        long nodeId,
        long assigneeId,
        WorkflowTaskStatus taskStatus,
        String taskOpinion,
        LocalDateTime taskActedAt,
        LocalDateTime taskCreatedAt) {
}
