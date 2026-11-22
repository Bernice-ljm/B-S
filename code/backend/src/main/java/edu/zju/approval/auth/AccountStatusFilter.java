package edu.zju.approval.auth;

import edu.zju.approval.user.UserQueryService;
import edu.zju.approval.web.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserQueryService userQueryService;
    private final ApiErrorWriter errorWriter;

    public AccountStatusFilter(UserQueryService userQueryService, ApiErrorWriter errorWriter) {
        this.userQueryService = userQueryService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            final boolean active;
            try {
                active = userQueryService.findAuthenticationRecordById(user.userId())
                        .filter(record -> "ACTIVE".equals(record.status()))
                        .isPresent();
            } catch (DataAccessException | TransactionException exception) {
                errorWriter.write(response, request, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "AUTHENTICATION_SERVICE_UNAVAILABLE", "Authentication service unavailable");
                return;
            }
            if (!active) {
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
                errorWriter.write(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED", "Authentication required");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
