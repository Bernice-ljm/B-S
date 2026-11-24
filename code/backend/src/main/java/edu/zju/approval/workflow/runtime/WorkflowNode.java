package edu.zju.approval.workflow.runtime;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public final class WorkflowNode {

    private final Long id;
    private final Long instanceId;
    private final int sequenceNo;
    private final String nodeName;
    private final String ruleSnapshot;
    private final WorkflowTask task;
    private final LocalDateTime createdAt;
    private WorkflowNodeStatus status;
    private LocalDateTime actedAt;

    public WorkflowNode(Long id, Long instanceId, int sequenceNo, String nodeName,
                        String ruleSnapshot, WorkflowNodeStatus status, WorkflowTask task,
                        LocalDateTime actedAt, LocalDateTime createdAt) {
        if (sequenceNo <= 0) {
            throw new IllegalArgumentException("sequenceNo must be positive");
        }
        this.id = id;
        this.instanceId = instanceId;
        this.sequenceNo = sequenceNo;
        this.nodeName = Objects.requireNonNull(nodeName, "nodeName");
        this.ruleSnapshot = Objects.requireNonNull(ruleSnapshot, "ruleSnapshot");
        this.status = Objects.requireNonNull(status, "status");
        this.task = task;
        this.actedAt = actedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        validateTaskState();
    }

    void activate() {
        if (status != WorkflowNodeStatus.WAITING) {
            throw new InvalidWorkflowTransitionException("Only a waiting node can be activated");
        }
        if (task == null || task.status() != WorkflowTaskStatus.PENDING) {
            throw new InvalidWorkflowTransitionException("An actionable node must have a pending task snapshot");
        }
        status = WorkflowNodeStatus.PENDING;
    }

    void approve(long actorId, long applicantId, String opinion, LocalDateTime occurredAt) {
        requirePending();
        task.approve(actorId, applicantId, opinion, occurredAt);
        status = WorkflowNodeStatus.APPROVED;
        actedAt = occurredAt;
    }

    void reject(long actorId, long applicantId, String opinion, LocalDateTime occurredAt) {
        requirePending();
        task.reject(actorId, applicantId, opinion, occurredAt);
        status = WorkflowNodeStatus.REJECTED;
        actedAt = occurredAt;
    }

    void cancel(LocalDateTime occurredAt) {
        if (status == WorkflowNodeStatus.WAITING || status == WorkflowNodeStatus.PENDING) {
            if (task != null) {
                task.cancel(occurredAt);
            }
            status = WorkflowNodeStatus.CANCELLED;
            actedAt = occurredAt;
        }
    }

    private void requirePending() {
        if (status != WorkflowNodeStatus.PENDING) {
            throw new InvalidWorkflowTransitionException("Only the pending workflow node can be handled");
        }
    }

    private void validateTaskState() {
        switch (status) {
            case PENDING -> requireTaskState(WorkflowTaskStatus.PENDING);
            case APPROVED -> requireTaskState(WorkflowTaskStatus.APPROVED);
            case REJECTED -> requireTaskState(WorkflowTaskStatus.REJECTED);
            case SKIPPED -> {
                if (task != null) {
                    throw new IllegalArgumentException("A skipped node cannot have an approval task");
                }
            }
            case CANCELLED -> {
                if (task != null && task.status() != WorkflowTaskStatus.CANCELLED) {
                    throw new IllegalArgumentException("A cancelled node task must also be cancelled");
                }
            }
            case WAITING -> {
                if (task != null && task.status() != WorkflowTaskStatus.PENDING) {
                    throw new IllegalArgumentException("A waiting node may only hold a pending task snapshot");
                }
            }
        }
    }

    private void requireTaskState(WorkflowTaskStatus expected) {
        if (task == null || task.status() != expected) {
            throw new IllegalArgumentException("Node and task states must agree: " + status + "/" + expected);
        }
    }

    public Long id() { return id; }
    public Long instanceId() { return instanceId; }
    public int sequenceNo() { return sequenceNo; }
    public String nodeName() { return nodeName; }
    public String ruleSnapshot() { return ruleSnapshot; }
    public WorkflowNodeStatus status() { return status; }
    public Optional<WorkflowTask> task() { return Optional.ofNullable(task); }
    public LocalDateTime actedAt() { return actedAt; }
    public LocalDateTime createdAt() { return createdAt; }
}
