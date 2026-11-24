package edu.zju.approval.workflow.runtime;

import java.time.LocalDateTime;
import java.util.Objects;

public record IdempotencyRecord(
        Long id,
        long actorId,
        String operation,
        String idempotencyKey,
        long resourceId,
        Integer draftVersion,
        String requestHash,
        IdempotencyStatus status,
        String savedResult,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public IdempotencyRecord {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(requestHash, "requestHash");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        if ((status == IdempotencyStatus.PROCESSING) != (completedAt == null)) {
            throw new IllegalArgumentException("Completion time must match idempotency status");
        }
    }
}
