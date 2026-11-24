package edu.zju.approval.application.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Application(
        Long id,
        String applicationNo,
        ApplicationType applicationType,
        long applicantId,
        long departmentId,
        String title,
        Long totalAmountMinor,
        ApplicationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int rowVersion) {

    public Application {
        Objects.requireNonNull(applicationNo, "applicationNo");
        Objects.requireNonNull(applicationType, "applicationType");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (totalAmountMinor != null && totalAmountMinor < 0) {
            throw new IllegalArgumentException("totalAmountMinor must be non-negative");
        }
        if (rowVersion < 0) {
            throw new IllegalArgumentException("rowVersion must be non-negative");
        }
        if ((status == ApplicationStatus.DRAFT) != (submittedAt == null)) {
            throw new IllegalArgumentException("Only drafts may have no submittedAt timestamp");
        }
    }

    public Application submit(LocalDateTime occurredAt) {
        return transitionTo(ApplicationStatus.IN_APPROVAL, occurredAt, occurredAt);
    }

    public Application approve(LocalDateTime occurredAt) {
        return transitionTo(ApplicationStatus.APPROVED, occurredAt, submittedAt);
    }

    public Application reject(LocalDateTime occurredAt) {
        return transitionTo(ApplicationStatus.REJECTED, occurredAt, submittedAt);
    }

    public Application withdraw(LocalDateTime occurredAt) {
        return transitionTo(ApplicationStatus.WITHDRAWN, occurredAt, submittedAt);
    }

    private Application transitionTo(
            ApplicationStatus target,
            LocalDateTime occurredAt,
            LocalDateTime nextSubmittedAt) {
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (!status.canTransitionTo(target)) {
            throw new InvalidApplicationStateTransitionException(status, target);
        }
        return new Application(id, applicationNo, applicationType, applicantId, departmentId, title,
                totalAmountMinor, target, nextSubmittedAt, createdAt, occurredAt, rowVersion);
    }
}
