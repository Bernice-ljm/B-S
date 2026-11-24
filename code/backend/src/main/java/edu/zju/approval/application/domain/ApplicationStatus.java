package edu.zju.approval.application.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    DRAFT,
    IN_APPROVAL,
    APPROVED,
    REJECTED,
    WITHDRAWN;

    public boolean canTransitionTo(ApplicationStatus target) {
        Set<ApplicationStatus> allowed = switch (this) {
            case DRAFT -> EnumSet.of(IN_APPROVAL);
            case IN_APPROVAL -> EnumSet.of(APPROVED, REJECTED, WITHDRAWN);
            case APPROVED, REJECTED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
        };
        return allowed.contains(target);
    }
}
