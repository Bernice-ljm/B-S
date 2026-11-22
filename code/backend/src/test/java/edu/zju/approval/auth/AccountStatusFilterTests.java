package edu.zju.approval.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.zju.approval.user.UserQueryService;
import edu.zju.approval.user.UserRecord;
import edu.zju.approval.user.UserMapper;
import edu.zju.approval.web.ApiErrorWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccountStatusFilterTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void databaseFailureReturnsControlled503WithoutInvalidatingSession() throws Exception {
        var unavailableUsers = new UserQueryService(null, null, null) {
            @Override
            public Optional<UserRecord> findAuthenticationRecordById(long id) {
                throw new DataAccessResourceFailureException("synthetic database outage");
            }
        };
        var filter = new AccountStatusFilter(
                unavailableUsers, new ApiErrorWriter(new ObjectMapper().findAndRegisterModules()));
        var principal = new AuthenticatedUser(42L, "synthetic", "Synthetic User", 1L, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, Set.of()));

        var request = new MockHttpServletRequest("GET", "/api/auth/me");
        var session = (MockHttpSession) request.getSession(true);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains(
                "AUTHENTICATION_SERVICE_UNAVAILABLE", "Authentication service unavailable");
        assertThat(chain.getRequest()).isNull();
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void transactionBeginFailureReturns503AndSameSessionRecovers() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(TransactionFailureTestConfig.class)) {
            var userQueryService = context.getBean(UserQueryService.class);
            var transactionManager = context.getBean(FailingTransactionManager.class);
            assertThat(AopUtils.isAopProxy(userQueryService)).isTrue();

            var filter = new AccountStatusFilter(
                    userQueryService, new ApiErrorWriter(new ObjectMapper().findAndRegisterModules()));
            var principal = new AuthenticatedUser(42L, "synthetic", "Synthetic User", 1L, Set.of());
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, Set.of());
            SecurityContext originalSecurityContext = SecurityContextHolder.createEmptyContext();
            originalSecurityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(originalSecurityContext);
            var session = new MockHttpSession();
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    originalSecurityContext);
            assertThat(session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                    .isSameAs(originalSecurityContext);

            var failedRequest = new MockHttpServletRequest("GET", "/api/auth/me");
            failedRequest.setSession(session);
            var failedResponse = new MockHttpServletResponse();
            var failedChain = new MockFilterChain();
            filter.doFilter(failedRequest, failedResponse, failedChain);

            assertThat(failedResponse.getStatus()).isEqualTo(503);
            assertThat(failedResponse.getContentAsString()).contains(
                    "AUTHENTICATION_SERVICE_UNAVAILABLE", "Authentication service unavailable");
            assertThat(failedChain.getRequest()).isNull();
            assertThat(session.isInvalid()).isFalse();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
            SecurityContext storedAfterFailure = (SecurityContext) session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertThat(storedAfterFailure).isSameAs(originalSecurityContext);
            assertThat(storedAfterFailure.getAuthentication()).isSameAs(authentication);

            transactionManager.setFailOnBegin(false);
            var recoveredRequest = new MockHttpServletRequest("GET", "/api/auth/me");
            recoveredRequest.setSession(session);
            SecurityContextHolder.clearContext();
            var securityContextRepository = new HttpSessionSecurityContextRepository();
            SecurityContext restoredSecurityContext = securityContextRepository
                    .loadDeferredContext(recoveredRequest)
                    .get();
            SecurityContextHolder.setContext(restoredSecurityContext);
            assertThat(restoredSecurityContext).isSameAs(originalSecurityContext);
            assertThat(restoredSecurityContext.getAuthentication()).isSameAs(authentication);
            var recoveredResponse = new MockHttpServletResponse();
            var recoveredChain = new MockFilterChain();
            filter.doFilter(recoveredRequest, recoveredResponse, recoveredChain);

            assertThat(recoveredChain.getRequest()).isSameAs(recoveredRequest);
            assertThat(recoveredResponse.getStatus()).isEqualTo(200);
            assertThat(session.isInvalid()).isFalse();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
            assertThat(session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                    .isSameAs(originalSecurityContext);
        }
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionFailureTestConfig {

        @Bean
        FailingTransactionManager transactionManager() {
            return new FailingTransactionManager();
        }

        @Bean
        UserMapper userMapper() {
            return new UserMapper() {
                @Override
                public Optional<UserRecord> findById(long id) {
                    var now = LocalDateTime.now();
                    return Optional.of(new UserRecord(
                            id, 1L, "synthetic", "Synthetic User", "synthetic@example.test",
                            "unused", "ACTIVE", now, now));
                }

                @Override
                public Optional<UserRecord> findByUsername(String username) {
                    return Optional.empty();
                }
            };
        }

        @Bean
        UserQueryService userQueryService(UserMapper userMapper) {
            return new UserQueryService(userMapper, null, null);
        }
    }

    static class FailingTransactionManager extends AbstractPlatformTransactionManager {

        private boolean failOnBegin = true;

        void setFailOnBegin(boolean failOnBegin) {
            this.failOnBegin = failOnBegin;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            if (failOnBegin) {
                throw new CannotCreateTransactionException("synthetic transaction begin failure");
            }
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
