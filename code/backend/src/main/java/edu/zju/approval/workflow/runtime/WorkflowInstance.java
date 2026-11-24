package edu.zju.approval.workflow.runtime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class WorkflowInstance {

    private final Long id;
    private final long applicationId;
    private final Long templateVersionId;
    private final String fixedWorkflowVersionKey;
    private final List<WorkflowNode> nodes;
    private final LocalDateTime createdAt;
    private int rowVersion;
    private WorkflowInstanceStatus status;
    private Long currentNodeId;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public WorkflowInstance(Long id, long applicationId, Long templateVersionId,
                            String fixedWorkflowVersionKey, WorkflowInstanceStatus status,
                            Long currentNodeId, int rowVersion, LocalDateTime createdAt,
                            LocalDateTime updatedAt, LocalDateTime completedAt,
                            List<WorkflowNode> nodes) {
        if ((templateVersionId == null) == (fixedWorkflowVersionKey == null)) {
            throw new IllegalArgumentException("Exactly one workflow version reference must be present");
        }
        if (rowVersion < 0) {
            throw new IllegalArgumentException("rowVersion must be non-negative");
        }
        this.id = id;
        this.applicationId = applicationId;
        this.templateVersionId = templateVersionId;
        this.fixedWorkflowVersionKey = fixedWorkflowVersionKey;
        this.status = Objects.requireNonNull(status, "status");
        this.currentNodeId = currentNodeId;
        this.rowVersion = rowVersion;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.completedAt = completedAt;
        this.nodes = new ArrayList<>(Objects.requireNonNull(nodes, "nodes"));
        this.nodes.sort(Comparator.comparingInt(WorkflowNode::sequenceNo));
        validateInvariants();
    }

    public void approveCurrent(long actorId, long applicantId, String opinion, LocalDateTime occurredAt) {
        WorkflowNode current = currentNode();
        current.approve(actorId, applicantId, opinion, occurredAt);
        WorkflowNode next = nodes.stream()
                .filter(node -> node.sequenceNo() > current.sequenceNo())
                .filter(node -> node.status() == WorkflowNodeStatus.WAITING)
                .findFirst()
                .orElse(null);
        if (next == null) {
            finish(WorkflowInstanceStatus.APPROVED, occurredAt);
        } else {
            next.activate();
            currentNodeId = next.id();
            touch(occurredAt);
        }
        validateInvariants();
    }

    public void rejectCurrent(long actorId, long applicantId, String opinion, LocalDateTime occurredAt) {
        currentNode().reject(actorId, applicantId, opinion, occurredAt);
        cancelUnfinished(occurredAt);
        finish(WorkflowInstanceStatus.REJECTED, occurredAt);
        validateInvariants();
    }

    public void withdraw(LocalDateTime occurredAt) {
        requirePendingInstance();
        cancelUnfinished(occurredAt);
        finish(WorkflowInstanceStatus.WITHDRAWN, occurredAt);
        validateInvariants();
    }

    private WorkflowNode currentNode() {
        requirePendingInstance();
        return nodes.stream()
                .filter(node -> Objects.equals(node.id(), currentNodeId))
                .findFirst()
                .orElseThrow(() -> new InvalidWorkflowTransitionException("Current node is missing"));
    }

    private void requirePendingInstance() {
        if (status != WorkflowInstanceStatus.PENDING) {
            throw new InvalidWorkflowTransitionException("Workflow instance is already terminal");
        }
    }

    private void cancelUnfinished(LocalDateTime occurredAt) {
        nodes.forEach(node -> node.cancel(occurredAt));
    }

    private void finish(WorkflowInstanceStatus terminalStatus, LocalDateTime occurredAt) {
        status = terminalStatus;
        currentNodeId = null;
        completedAt = occurredAt;
        touch(occurredAt);
    }

    private void touch(LocalDateTime occurredAt) {
        updatedAt = Objects.requireNonNull(occurredAt, "occurredAt");
        rowVersion++;
    }

    private void validateInvariants() {
        var sequenceNumbers = new HashSet<Integer>();
        if (nodes.stream().anyMatch(node -> !sequenceNumbers.add(node.sequenceNo()))) {
            throw new IllegalArgumentException("Workflow node sequence numbers must be unique");
        }
        long pendingNodes = nodes.stream().filter(node -> node.status() == WorkflowNodeStatus.PENDING).count();
        if (pendingNodes > 1) {
            throw new IllegalArgumentException("A workflow instance may have at most one pending node");
        }
        if (status == WorkflowInstanceStatus.PENDING) {
            if (pendingNodes != 1 || currentNodeId == null) {
                throw new IllegalArgumentException("A pending instance must point to exactly one pending node");
            }
            WorkflowNode current = nodes.stream()
                    .filter(node -> Objects.equals(node.id(), currentNodeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Current node must belong to this instance"));
            if (current.status() != WorkflowNodeStatus.PENDING) {
                throw new IllegalArgumentException("Current node must be pending");
            }
            if (completedAt != null) {
                throw new IllegalArgumentException("A pending instance cannot have completedAt");
            }
        } else {
            boolean hasUnfinishedNode = nodes.stream().anyMatch(node ->
                    node.status() == WorkflowNodeStatus.PENDING
                            || node.status() == WorkflowNodeStatus.WAITING);
            if (currentNodeId != null || pendingNodes != 0 || hasUnfinishedNode) {
                throw new IllegalArgumentException("A terminal instance cannot retain an unfinished node");
            }
        }
    }

    public Long id() { return id; }
    public long applicationId() { return applicationId; }
    public Long templateVersionId() { return templateVersionId; }
    public String fixedWorkflowVersionKey() { return fixedWorkflowVersionKey; }
    public WorkflowInstanceStatus status() { return status; }
    public Long currentNodeId() { return currentNodeId; }
    public int rowVersion() { return rowVersion; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public LocalDateTime completedAt() { return completedAt; }
    public List<WorkflowNode> nodes() { return List.copyOf(nodes); }
}
