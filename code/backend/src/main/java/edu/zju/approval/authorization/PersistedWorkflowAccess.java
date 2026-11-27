package edu.zju.approval.authorization;

import edu.zju.approval.workflow.runtime.WorkflowInstanceStatus;
import edu.zju.approval.workflow.runtime.WorkflowNodeStatus;

import java.time.LocalDateTime;

public record PersistedWorkflowAccess(
        long instanceId,
        long applicationId,
        Long templateVersionId,
        String fixedWorkflowVersionKey,
        WorkflowInstanceStatus instanceStatus,
        Long currentNodeId,
        int instanceRowVersion,
        LocalDateTime instanceCreatedAt,
        LocalDateTime instanceUpdatedAt,
        LocalDateTime instanceCompletedAt,
        Long nodeId,
        Integer sequenceNo,
        String nodeName,
        String approvalMode,
        String ruleSnapshot,
        WorkflowNodeStatus nodeStatus,
        LocalDateTime nodeActedAt,
        LocalDateTime nodeCreatedAt) {
}
