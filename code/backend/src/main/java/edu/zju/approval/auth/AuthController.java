package edu.zju.approval.auth;

import edu.zju.approval.user.UserProfile;
import edu.zju.approval.user.UserQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.time.Instant;
import edu.zju.approval.web.ApiError;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserQueryService userQueryService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserQueryService userQueryService,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.userQueryService = userQueryService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @PostMapping("/login")
    AuthResponse login(
            @Valid @RequestBody LoginRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        var verified = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        requestBody.username(), requestBody.password()));
        UserProfile profile = userQueryService.findProfileByUsername(verified.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user is unavailable"));
        var roleKeys = new LinkedHashSet<String>();
        profile.roles().stream()
                .filter(role -> "ACTIVE".equals(role.status()))
                .forEach(role -> roleKeys.add(role.roleKey()));
        var principal = new AuthenticatedUser(
                profile.id(), profile.username(), profile.fullName(),
                profile.department() == null ? null : profile.department().id(), roleKeys);
        var authorities = roleKeys.stream()
                .map(DatabaseUserDetailsService::authorityName)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var established = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);

        sessionAuthenticationStrategy.onAuthentication(established, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(established);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return toResponse(profile, roleKeys);
    }

    @GetMapping("/me")
    ResponseEntity<?> me() {
        var currentUser = CurrentUser.get();
        if (currentUser.isEmpty()) {
            return authenticationRequired();
        }
        return userQueryService.findProfileById(currentUser.orElseThrow().userId())
                .<ResponseEntity<?>>map(profile -> ResponseEntity.ok(
                        toResponse(profile, currentUser.orElseThrow().roleKeys())))
                .orElseGet(this::authenticationRequired);
    }

    private ResponseEntity<ApiError> authenticationRequired() {
        return ResponseEntity.status(401).body(new ApiError(
                "AUTHENTICATION_REQUIRED", "Authentication required", "/api/auth/me", Instant.now()));
    }

    private static AuthResponse toResponse(UserProfile profile, java.util.Set<String> roleKeys) {
        return new AuthResponse(
                profile.id(), profile.username(), profile.fullName(), profile.email(),
                profile.status(), profile.department(), roleKeys);
    }
}
