package edu.zju.approval.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ProductionCookieSecurityValidatorTests {

    @Test
    void productionProfileRejectsNonSecureSessionCookie() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new ProductionCookieSecurityValidator(false).afterPropertiesSet())
                .withMessageContaining("SESSION_COOKIE_SECURE");
    }

    @Test
    void productionProfileAcceptsSecureSessionCookie() {
        assertThatCode(() -> new ProductionCookieSecurityValidator(true).afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}
