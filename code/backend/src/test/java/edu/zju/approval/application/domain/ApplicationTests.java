package edu.zju.approval.application.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTests {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 1, 9, 0);
    private static final LocalDateTime ACTION_TIME = CREATED.plusHours(1);

    @Test
    void supportsEveryAllowedApplicationTransition() {
        Application submitted = draft().submit(ACTION_TIME);
        assertThat(submitted.status()).isEqualTo(ApplicationStatus.IN_APPROVAL);
        assertThat(submitted.submittedAt()).isEqualTo(ACTION_TIME);
        assertThat(submitted.approve(ACTION_TIME.plusMinutes(1)).status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(submitted.reject(ACTION_TIME.plusMinutes(1)).status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(submitted.withdraw(ACTION_TIME.plusMinutes(1)).status()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void terminalStatesCannotBeReopenedOrHandledAgain() {
        Application submitted = draft().submit(ACTION_TIME);
        assertThatThrownBy(() -> submitted.approve(ACTION_TIME).approve(ACTION_TIME))
                .isInstanceOf(InvalidApplicationStateTransitionException.class);
        assertThatThrownBy(() -> submitted.reject(ACTION_TIME).submit(ACTION_TIME))
                .isInstanceOf(InvalidApplicationStateTransitionException.class);
        assertThatThrownBy(() -> submitted.withdraw(ACTION_TIME).reject(ACTION_TIME))
                .isInstanceOf(InvalidApplicationStateTransitionException.class);
        assertThatThrownBy(() -> draft().approve(ACTION_TIME))
                .isInstanceOf(InvalidApplicationStateTransitionException.class);
    }

    private Application draft() {
        return new Application(1L, "APP-1", ApplicationType.PROCUREMENT, 10L, 20L,
                "Purchase", 100L, ApplicationStatus.DRAFT, null, CREATED, CREATED, 0);
    }
}
