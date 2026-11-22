package edu.zju.approval.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import jakarta.servlet.http.Cookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.autoconfigure.exclude=")
@EnabledIfEnvironmentVariable(named = "STAGE2A_TEST_DB_URL", matches = ".+")
@TestExecutionListeners(
        listeners = {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@Transactional
class AuthenticationIntegrationTests {

    private static final String PASSWORD = "Stage2A2-Test-Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnvironment("STAGE2A_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> requiredEnvironment("STAGE2A_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("STAGE2A_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("mybatis.mapper-locations", () -> "classpath*:mapper/*/*.xml");
        registry.add("app.persistence.enabled", () -> true);
    }

    @BeforeEach
    void createAuthenticationUsers() {
        insertUser("auth.employee", "Auth Employee", "auth.employee@example.test", "ACTIVE", 1L);
        insertUser("auth.admin", "Auth Admin", "auth.admin@example.test", "ACTIVE", 1L);
        insertUser("auth.disabled", "Auth Disabled", "auth.disabled@example.test", "DISABLED", 1L);
        insertUser("auth.pending", "Auth Pending", "auth.pending@example.test", "PENDING_CONFIRMATION", null);
        grantRole("auth.employee", 1L);
        grantRole("auth.admin", 1L);
        grantRole("auth.admin", 5L);
    }

    @Test
    void loginLoadsDatabaseUserRolesAndCreatesSession() throws Exception {
        var originalSession = new MockHttpSession();
        String oldSessionId = originalSession.getId();
        CsrfExchange csrf = obtainCsrf(originalSession);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(originalSession)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.cookie().getValue())
                        .contentType("application/json")
                        .content(loginJson("auth.admin", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("auth.admin"))
                .andExpect(jsonPath("$.roleKeys[0]").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
        assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(oldSessionId);
        assertThat(result.getResponse().getContentAsString()).contains("EMPLOYEE", "SYSTEM_ADMIN");
    }

    @Test
    void rejectsWrongPasswordUnknownDisabledAndPendingUsersUniformly() throws Exception {
        assertLoginRejected("auth.employee", "wrong-password");
        assertLoginRejected("auth.missing", PASSWORD);
        assertLoginRejected("auth.disabled", PASSWORD);
        assertLoginRejected("auth.pending", PASSWORD);
    }

    @Test
    void currentUserRequiresAuthenticationAndKeepsUsersIsolated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        MockHttpSession employeeSession = login("auth.employee");
        MockHttpSession adminSession = login("auth.admin");

        mockMvc.perform(get("/api/auth/me").session(employeeSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("auth.employee"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("SYSTEM_ADMIN"))));
        mockMvc.perform(get("/api/auth/me").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("auth.admin"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SYSTEM_ADMIN")));
        mockMvc.perform(get("/api/applications").session(employeeSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void logoutInvalidatesSessionAndRepeatedLogoutDoesNotFailWithServerError() throws Exception {
        MockHttpSession session = login("auth.employee");
        CsrfExchange authenticatedCsrf = obtainCsrf(session);

        MvcResult logout = mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(authenticatedCsrf.cookie())
                        .header("X-XSRF-TOKEN", authenticatedCsrf.cookie().getValue()))
                .andExpect(status().isNoContent())
                .andReturn();
        assertDeletedCookie(logout, "JSESSIONID");
        assertDeletedCookie(logout, "XSRF-TOKEN");
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());

        CsrfExchange anonymousCsrf = obtainCsrf(new MockHttpSession());
        mockMvc.perform(post("/api/auth/logout")
                        .session(anonymousCsrf.session())
                        .cookie(anonymousCsrf.cookie())
                        .header("X-XSRF-TOKEN", anonymousCsrf.cookie().getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void csrfProtectsStateChangesButNotSafeReads() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson("auth.employee", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        var session = new MockHttpSession();
        CsrfExchange anonymousCsrf = obtainCsrf(session);
        assertThat(anonymousCsrf.cookie().isHttpOnly()).isFalse();
        assertThat(anonymousCsrf.cookie().getSecure()).isFalse();
        assertThat(anonymousCsrf.cookie().getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(anonymousCsrf.responseToken()).isNotEqualTo(anonymousCsrf.cookie().getValue());

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .cookie(anonymousCsrf.cookie())
                        .contentType("application/json")
                        .content(loginJson("auth.employee", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .header("X-XSRF-TOKEN", anonymousCsrf.cookie().getValue())
                        .contentType("application/json")
                        .content(loginJson("auth.employee", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .cookie(anonymousCsrf.cookie())
                        .header("X-XSRF-TOKEN", "wrong-token")
                        .contentType("application/json")
                        .content(loginJson("auth.employee", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        MvcResult login = performLogin("auth.employee", session, anonymousCsrf.cookie());
        assertDeletedCookie(login, "XSRF-TOKEN");
        CsrfExchange authenticatedCsrf = obtainCsrf(session);
        assertThat(authenticatedCsrf.cookie().getValue())
                .isNotEqualTo(anonymousCsrf.cookie().getValue());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(authenticatedCsrf.cookie())
                        .header("X-XSRF-TOKEN", anonymousCsrf.cookie().getValue()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(authenticatedCsrf.cookie())
                        .header("X-XSRF-TOKEN", authenticatedCsrf.cookie().getValue()))
                .andExpect(status().isNoContent());

        CsrfExchange afterLogout = obtainCsrf(new MockHttpSession());
        performLogin("auth.employee", afterLogout.session(), afterLogout.cookie());
    }

    @Test
    void authenticatedSessionIsRejectedAfterAccountIsDisabled() throws Exception {
        MockHttpSession session = login("auth.employee");
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE username = ?", "auth.employee");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        jdbcTemplate.update("UPDATE users SET status = 'ACTIVE' WHERE username = ?", "auth.employee");
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
        login("auth.employee");
    }

    private MockHttpSession login(String username) throws Exception {
        var session = new MockHttpSession();
        CsrfExchange csrf = obtainCsrf(session);
        MvcResult result = performLogin(username, session, csrf.cookie());
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void assertLoginRejected(String username, String password) throws Exception {
        var session = new MockHttpSession();
        CsrfExchange csrf = obtainCsrf(session);
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.cookie().getValue())
                        .contentType("application/json")
                        .content(loginJson(username, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Authentication failed"));
    }

    private MvcResult performLogin(String username, MockHttpSession session, Cookie csrfCookie)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType("application/json")
                        .content(loginJson(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
    }

    private CsrfExchange obtainCsrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        String responseToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token").asText();
        return new CsrfExchange(session, cookie, responseToken);
    }

    private static void assertDeletedCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
    }

    private record CsrfExchange(MockHttpSession session, Cookie cookie, String responseToken) {
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    private void insertUser(
            String username,
            String fullName,
            String email,
            String status,
            Long departmentId) {
        jdbcTemplate.update("""
                INSERT INTO users (department_id, username, full_name, email, password_hash, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, departmentId, username, fullName, email, passwordEncoder.encode(PASSWORD), status);
    }

    private void grantRole(String username, long roleId) {
        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id, granted_by)
                SELECT id, ?, 1 FROM users WHERE username = ?
                """, roleId, username);
    }

    private static String requiredEnvironment(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for MySQL integration tests");
        }
        return value;
    }
}
