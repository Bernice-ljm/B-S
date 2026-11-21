package edu.zju.approval.user;

import edu.zju.approval.organization.DepartmentMapper;
import edu.zju.approval.role.RoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "spring.autoconfigure.exclude=")
@EnabledIfEnvironmentVariable(named = "STAGE2A_TEST_DB_URL", matches = ".+")
@TestExecutionListeners(
        listeners = {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@Transactional
class UserQueryIntegrationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserQueryService userQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnvironment("STAGE2A_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> requiredEnvironment("STAGE2A_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("STAGE2A_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("mybatis.mapper-locations", () -> "classpath*:mapper/*/*.xml");
        registry.add("app.persistence.enabled", () -> true);
    }

    @Test
    void findsExistingUserByUsernameAndPreservesAuthenticationFields() {
        var user = userMapper.findByUsername("employee.rd").orElseThrow();

        assertThat(user.id()).isEqualTo(2L);
        assertThat(user.status()).isEqualTo("ACTIVE");
        assertThat(user.passwordHash()).startsWith("$2a$12$");
    }

    @Test
    void returnsEmptyForUnknownUserDepartmentAndRole() {
        assertThat(userMapper.findByUsername("missing-user")).isEmpty();
        assertThat(userMapper.findById(999_999L)).isEmpty();
        assertThat(departmentMapper.findById(999_999L)).isEmpty();
        assertThat(departmentMapper.existsById(999_999L)).isFalse();
        assertThat(roleMapper.findById(999_999L)).isEmpty();
        assertThat(roleMapper.findByRoleKey("MISSING_ROLE")).isEmpty();
    }

    @Test
    void loadsUsersDepartmentAndSingleRole() {
        var profile = userQueryService.findProfileByUsername("employee.rd").orElseThrow();

        assertThat(profile.department()).isNotNull();
        assertThat(profile.department().id()).isEqualTo(1L);
        assertThat(profile.roles()).extracting("roleKey").containsExactly("EMPLOYEE");
    }

    @Test
    void loadsMultipleRolesWithoutDuplicatingUser() {
        var profile = userQueryService.findProfileByUsername("admin.demo").orElseThrow();

        assertThat(profile.id()).isEqualTo(1L);
        assertThat(profile.roles()).extracting("roleKey")
                .containsExactly("EMPLOYEE", "SYSTEM_ADMIN");
    }

    @Test
    void keepsRoleAssociationsIsolatedBetweenUsers() {
        var employee = userQueryService.findProfileByUsername("employee.rd").orElseThrow();
        var manager = userQueryService.findProfileByUsername("manager.rd").orElseThrow();

        assertThat(employee.roles()).extracting("roleKey").containsExactly("EMPLOYEE");
        assertThat(manager.roles()).extracting("roleKey")
                .containsExactly("EMPLOYEE", "DEPARTMENT_MANAGER");
    }

    @Test
    void preservesDisabledStatus() {
        jdbcTemplate.update("""
                INSERT INTO users (department_id, username, full_name, email, password_hash, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, 1L, "disabled.test", "Disabled Test", "disabled.test@example.test",
                "$2a$12$rx8roAi/WDCTSkqS/mg3xuKpNS9As9x.WkFjFeByUFdqGUZad.6om", "DISABLED");

        var user = userQueryService.findProfileByUsername("disabled.test").orElseThrow();
        assertThat(user.status()).isEqualTo("DISABLED");
    }

    @Test
    void publicProfileTypeCannotExposePasswordHash() {
        assertThat(Arrays.stream(UserProfile.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("passwordHash");
    }

    private static String requiredEnvironment(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for MySQL integration tests");
        }
        return value;
    }
}
