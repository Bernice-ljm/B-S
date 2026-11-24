package edu.zju.approval.workflow.runtime;

import edu.zju.approval.application.domain.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public record ApprovalLog(
        Long id,
        long applicationId,
        Long nodeId,
        Long taskId,
        long idempotencyRecordId,
        long actorId,
        ApprovalAction action,
        String opinion,
        Long fromAssigneeId,
        Long toAssigneeId,
        ApplicationStatus resultStatus,
        String requestKey,
        LocalDateTime createdAt) {

    public ApprovalLog {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(resultStatus, "resultStatus");
        Objects.requireNonNull(requestKey, "requestKey");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public Long workflowInstanceId(WorkflowNode node) {
        if (nodeId == null || !Objects.equals(nodeId, node.id())) {
            throw new IllegalArgumentException("The supplied node does not belong to this log");
        }
        return node.instanceId();
    }
}
