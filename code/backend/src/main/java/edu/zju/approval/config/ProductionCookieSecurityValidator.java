package edu.zju.approval.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionCookieSecurityValidator implements InitializingBean {

    private final boolean secureSessionCookie;

    public ProductionCookieSecurityValidator(
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureSessionCookie) {
        this.secureSessionCookie = secureSessionCookie;
    }

    @Override
    public void afterPropertiesSet() {
        if (!secureSessionCookie) {
            throw new IllegalStateException(
                    "SESSION_COOKIE_SECURE must be true when the prod profile is active");
        }
    }
}
