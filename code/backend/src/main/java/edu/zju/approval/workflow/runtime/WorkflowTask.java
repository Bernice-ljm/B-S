package edu.zju.approval.workflow.runtime;

import java.time.LocalDateTime;
import java.util.Objects;

public final class WorkflowTask {

    private final Long id;
    private final Long nodeId;
    private final long assigneeId;
    private final LocalDateTime createdAt;
    private WorkflowTaskStatus status;
    private String opinion;
    private LocalDateTime actedAt;

    public WorkflowTask(Long id, Long nodeId, long assigneeId, WorkflowTaskStatus status,
                        String opinion, LocalDateTime actedAt, LocalDateTime createdAt) {
        this.id = id;
        this.nodeId = nodeId;
        this.assigneeId = assigneeId;
        this.status = Objects.requireNonNull(status, "status");
        this.opinion = opinion;
        this.actedAt = actedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if ((status == WorkflowTaskStatus.PENDING) != (actedAt == null)) {
            throw new IllegalArgumentException("Pending task action time must be null and terminal task action time must be set");
        }
    }

    void approve(long actorId, long applicantId, String opinion, LocalDateTime actedAt) {
        verifyActor(actorId, applicantId);
        complete(WorkflowTaskStatus.APPROVED, opinion, actedAt);
    }

    void reject(long actorId, long applicantId, String opinion, LocalDateTime actedAt) {
        verifyActor(actorId, applicantId);
        complete(WorkflowTaskStatus.REJECTED, opinion, actedAt);
    }

    void cancel(LocalDateTime actedAt) {
        if (status == WorkflowTaskStatus.PENDING) {
            complete(WorkflowTaskStatus.CANCELLED, null, actedAt);
        }
    }

    private void verifyActor(long actorId, long applicantId) {
        if (status != WorkflowTaskStatus.PENDING) {
            throw new InvalidWorkflowTransitionException("Only a pending task can be handled");
        }
        if (actorId != assigneeId) {
            throw new InvalidWorkflowTransitionException("Only the task assignee can handle this task");
        }
        if (actorId == applicantId) {
            throw new InvalidWorkflowTransitionException("An applicant cannot approve their own application");
        }
    }

    private void complete(WorkflowTaskStatus target, String opinion, LocalDateTime occurredAt) {
        this.status = target;
        this.opinion = opinion;
        this.actedAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public Long id() { return id; }
    public Long nodeId() { return nodeId; }
    public long assigneeId() { return assigneeId; }
    public WorkflowTaskStatus status() { return status; }
    public String opinion() { return opinion; }
    public LocalDateTime actedAt() { return actedAt; }
    public LocalDateTime createdAt() { return createdAt; }
}
