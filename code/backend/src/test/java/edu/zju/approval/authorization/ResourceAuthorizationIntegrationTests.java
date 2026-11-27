package edu.zju.approval.authorization;

import edu.zju.approval.auth.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "spring.autoconfigure.exclude=")
@EnabledIfEnvironmentVariable(named = "STAGE2A_TEST_DB_URL", matches = ".+")
@TestExecutionListeners(
        listeners = {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@Transactional
class ResourceAuthorizationIntegrationTests {

    private static final long APPLICATION_ID = 20_001L;
    private static final long SECOND_APPLICATION_ID = 20_002L;
    private static final long SELF_ASSIGNED_APPLICATION_ID = 20_003L;
    private static final long TERMINAL_APPLICATION_ID = 20_004L;
    private static final long ALL_APPLICATION_ID = 20_005L;
    private static final long ANY_ONE_APPLICATION_ID = 20_006L;
    private static final long NO_TASK_APPLICATION_ID = 20_007L;
    private static final long OTHER_NODE_APPLICATION_ID = 20_008L;
    private static final long ATTACHMENT_ID = 21_001L;
    private static final long UNBOUND_ATTACHMENT_ID = 21_002L;

    @Autowired
    private ResourceAuthorizationService resourceAuthorizationService;

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

    @BeforeEach
    void createAuthorizationFixtures() {
        insertApplication(APPLICATION_ID, "AUTH-20001", 2L, "IN_APPROVAL");
        insertApplication(SECOND_APPLICATION_ID, "AUTH-20002", 4L, "IN_APPROVAL");
        insertApplication(SELF_ASSIGNED_APPLICATION_ID, "AUTH-20003", 2L, "IN_APPROVAL");
        insertApplication(TERMINAL_APPLICATION_ID, "AUTH-20004", 2L, "APPROVED");
        insertApplication(ALL_APPLICATION_ID, "AUTH-20005", 2L, "IN_APPROVAL");
        insertApplication(ANY_ONE_APPLICATION_ID, "AUTH-20006", 2L, "IN_APPROVAL");
        insertApplication(NO_TASK_APPLICATION_ID, "AUTH-20007", 2L, "IN_APPROVAL");
        insertApplication(OTHER_NODE_APPLICATION_ID, "AUTH-20008", 2L, "IN_APPROVAL");

        insertPendingWorkflow(22_001L, APPLICATION_ID, 23_001L, 24_001L, 3L);
        insertPendingWorkflow(22_002L, SECOND_APPLICATION_ID, 23_002L, 24_002L, 5L);
        insertPendingWorkflow(22_003L, SELF_ASSIGNED_APPLICATION_ID, 23_003L, 24_003L, 2L);
        insertTerminalWorkflow(22_004L, TERMINAL_APPLICATION_ID);
        insertPendingWorkflow(22_005L, ALL_APPLICATION_ID, 23_005L, "ALL");
        insertTask(24_005L, 23_005L, 3L, "PENDING");
        insertTask(24_006L, 23_005L, 4L, "PENDING");
        insertTask(24_007L, 23_005L, 5L, "APPROVED");
        insertPendingWorkflow(22_006L, ANY_ONE_APPLICATION_ID, 23_006L, "ANY_ONE");
        insertTask(24_008L, 23_006L, 3L, "PENDING");
        insertTask(24_009L, 23_006L, 4L, "PENDING");
        insertPendingWorkflow(22_007L, NO_TASK_APPLICATION_ID, 23_007L, "SINGLE");
        insertPendingWorkflow(22_008L, OTHER_NODE_APPLICATION_ID, 23_008L, "SINGLE");
        insertTask(24_010L, 23_008L, 3L, "PENDING");
        insertNonCurrentPendingNode(23_009L, 22_008L, 24_011L, 5L);

        insertAttachment(ATTACHMENT_ID, 2L, "authz/attachment-21001");
        insertAttachment(UNBOUND_ATTACHMENT_ID, 2L, "authz/attachment-21002");
        jdbcTemplate.update("""
                INSERT INTO attachment_binds (application_id, attachment_id, purpose)
                VALUES (?, ?, 'APPLICATION')
                """, APPLICATION_ID, ATTACHMENT_ID);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistedApplicantAndCurrentAssigneeCanViewButUnrelatedAndAdminUsersCannot() {
        authenticate(2L, "employee.rd", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canViewApplication(APPLICATION_ID)).isTrue();

        authenticate(3L, "manager.rd", "EMPLOYEE", "DEPARTMENT_MANAGER");
        assertThat(resourceAuthorizationService.canViewApplication(APPLICATION_ID)).isTrue();

        authenticate(4L, "employee.marketing", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canViewApplication(APPLICATION_ID)).isFalse();

        authenticate(1L, "admin.demo", "EMPLOYEE", "SYSTEM_ADMIN");
        assertThat(resourceAuthorizationService.canViewApplication(APPLICATION_ID)).isFalse();
        assertThat(resourceAuthorizationService.canViewApplication(999_999L)).isFalse();
    }

    @Test
    void applicationIdReplacementDoesNotReuseAuthorizationForAnotherApplication() {
        authenticate(3L, "manager.rd", "EMPLOYEE", "DEPARTMENT_MANAGER");

        assertThat(resourceAuthorizationService.canViewApplication(APPLICATION_ID)).isTrue();
        assertThat(resourceAuthorizationService.canViewApplication(SECOND_APPLICATION_ID)).isFalse();
        assertThat(resourceAuthorizationService.canApproveTask(APPLICATION_ID)).isTrue();
        assertThat(resourceAuthorizationService.canApproveTask(SECOND_APPLICATION_ID)).isFalse();
    }

    @Test
    void onlyPersistedCurrentAssigneeCanApproveAndApplicantCannotSelfApprove() {
        authenticate(3L, "manager.rd", "EMPLOYEE", "DEPARTMENT_MANAGER");
        assertThat(resourceAuthorizationService.canApproveTask(APPLICATION_ID)).isTrue();

        authenticate(4L, "employee.marketing", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canApproveTask(APPLICATION_ID)).isFalse();

        authenticate(2L, "employee.rd", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canApproveTask(SELF_ASSIGNED_APPLICATION_ID)).isFalse();
    }

    @Test
    void terminalApplicationCannotBeApprovedButRemainsVisibleToItsApplicant() {
        authenticate(2L, "employee.rd", "EMPLOYEE");

        assertThat(resourceAuthorizationService.canViewApplication(TERMINAL_APPLICATION_ID)).isTrue();
        assertThat(resourceAuthorizationService.canApproveTask(TERMINAL_APPLICATION_ID)).isFalse();
    }

    @Test
    void attachmentAuthorizationFollowsPersistedBindingAndDoesNotGrantAdminBypass() {
        authenticate(2L, "employee.rd", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canDownloadAttachment(ATTACHMENT_ID)).isTrue();

        authenticate(4L, "employee.marketing", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canDownloadAttachment(ATTACHMENT_ID)).isFalse();

        authenticate(1L, "admin.demo", "EMPLOYEE", "SYSTEM_ADMIN");
        assertThat(resourceAuthorizationService.canDownloadAttachment(ATTACHMENT_ID)).isFalse();
        assertThat(resourceAuthorizationService.canDownloadAttachment(UNBOUND_ATTACHMENT_ID)).isFalse();
        assertThat(resourceAuthorizationService.canDownloadAttachment(999_999L)).isFalse();
    }

    @Test
    void ambiguousAttachmentBindingFailsClosed() {
        jdbcTemplate.update("""
                INSERT INTO attachment_binds (application_id, attachment_id, purpose)
                VALUES (?, ?, 'APPLICATION')
                """, SECOND_APPLICATION_ID, ATTACHMENT_ID);
        authenticate(2L, "employee.rd", "EMPLOYEE");

        assertThat(resourceAuthorizationService.canDownloadAttachment(ATTACHMENT_ID)).isFalse();
    }

    @Test
    void allModeAuthorizesEveryCurrentPendingAssigneeAndIgnoresCompletedTasks() {
        authenticate(3L, "manager.rd", "EMPLOYEE", "DEPARTMENT_MANAGER");
        assertThat(resourceAuthorizationService.canApproveTask(ALL_APPLICATION_ID)).isTrue();

        authenticate(4L, "employee.marketing", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canApproveTask(ALL_APPLICATION_ID)).isTrue();

        authenticate(5L, "finance.demo", "FINANCE_APPROVER");
        assertThat(resourceAuthorizationService.canApproveTask(ALL_APPLICATION_ID)).isFalse();
    }

    @Test
    void anyOneModeAuthorizesAnyCurrentPendingAssigneeButNotAnUnrelatedUser() {
        authenticate(3L, "manager.rd", "EMPLOYEE", "DEPARTMENT_MANAGER");
        assertThat(resourceAuthorizationService.canApproveTask(ANY_ONE_APPLICATION_ID)).isTrue();

        authenticate(4L, "employee.marketing", "EMPLOYEE");
        assertThat(resourceAuthorizationService.canApproveTask(ANY_ONE_APPLICATION_ID)).isTrue();

        authenticate(5L, "finance.demo", "FINANCE_APPROVER");
        assertThat(resourceAuthorizationService.canApproveTask(ANY_ONE_APPLICATION_ID)).isFalse();
    }

    @Test
    void pendingTaskOnAnotherNodeCannotGrantCurrentNodeApproval() {
        authenticate(5L, "finance.demo", "FINANCE_APPROVER");
        assertThat(resourceAuthorizationService.canApproveTask(OTHER_NODE_APPLICATION_ID)).isFalse();

        authenticate(3L, "manager.rd", "EMPLOYEE", "DEPARTMENT_MANAGER");
        assertThat(resourceAuthorizationService.canApproveTask(OTHER_NODE_APPLICATION_ID)).isTrue();
    }

    @Test
    void pendingCurrentNodeWithoutAnEffectiveTaskFailsClosed() {
        authenticate(3L, "manager.rd", "EMPLOYEE", "DEPARTMENT_MANAGER");
        assertThatThrownBy(() -> resourceAuthorizationService.canApproveTask(NO_TASK_APPLICATION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tasks are incomplete");
    }

    private void insertApplication(long id, String number, long applicantId, String status) {
        jdbcTemplate.update("""
                INSERT INTO applications (
                    id, application_no, application_type, applicant_id, department_id,
                    title, status, total_amount_minor, submitted_at)
                VALUES (?, ?, 'PROCUREMENT', ?, 1, ?, ?, 10000, CURRENT_TIMESTAMP(3))
                """, id, number, applicantId, "Authorization fixture " + id, status);
    }

    private void insertPendingWorkflow(
            long instanceId, long applicationId, long nodeId, long taskId, long assigneeId) {
        insertPendingWorkflow(instanceId, applicationId, nodeId, "SINGLE");
        insertTask(taskId, nodeId, assigneeId, "PENDING");
    }

    private void insertPendingWorkflow(
            long instanceId, long applicationId, long nodeId, String approvalMode) {
        jdbcTemplate.update("""
                INSERT INTO workflow_instances (
                    id, application_id, fixed_workflow_version_key, current_node_id, status)
                VALUES (?, ?, 'AUTHORIZATION_TEST_V1', NULL, 'PENDING')
                """, instanceId, applicationId);
        jdbcTemplate.update("""
                INSERT INTO workflow_instance_nodes (
                    id, instance_id, sequence_no, node_name, approval_mode,
                    condition_result_snapshot, rule_snapshot, status)
                VALUES (?, ?, 1, 'Authorization test', ?, TRUE, JSON_OBJECT(), 'PENDING')
                """, nodeId, instanceId, approvalMode);
        jdbcTemplate.update(
                "UPDATE workflow_instances SET current_node_id = ? WHERE id = ?", nodeId, instanceId);
    }

    private void insertTask(long taskId, long nodeId, long assigneeId, String status) {
        jdbcTemplate.update("""
                INSERT INTO workflow_node_tasks (id, node_id, assignee_id, status, acted_at)
                VALUES (?, ?, ?, ?, CASE WHEN ? = 'PENDING' THEN NULL ELSE CURRENT_TIMESTAMP(3) END)
                """, taskId, nodeId, assigneeId, status, status);
    }

    private void insertNonCurrentPendingNode(
            long nodeId, long instanceId, long taskId, long assigneeId) {
        jdbcTemplate.update("""
                INSERT INTO workflow_instance_nodes (
                    id, instance_id, sequence_no, node_name, approval_mode,
                    condition_result_snapshot, rule_snapshot, status)
                VALUES (?, ?, 2, 'Non-current test node', 'SINGLE', TRUE, JSON_OBJECT(), 'PENDING')
                """, nodeId, instanceId);
        insertTask(taskId, nodeId, assigneeId, "PENDING");
    }

    private void insertTerminalWorkflow(long instanceId, long applicationId) {
        jdbcTemplate.update("""
                INSERT INTO workflow_instances (
                    id, application_id, fixed_workflow_version_key, current_node_id,
                    status, completed_at)
                VALUES (?, ?, 'AUTHORIZATION_TEST_V1', NULL, 'APPROVED', CURRENT_TIMESTAMP(3))
                """, instanceId, applicationId);
    }

    private void insertAttachment(long id, long uploaderId, String storageKey) {
        jdbcTemplate.update("""
                INSERT INTO attachments (
                    id, uploader_id, storage_key, original_name, mime_type, size_bytes, sha256)
                VALUES (?, ?, ?, 'receipt.pdf', 'application/pdf', 10, ?)
                """, id, uploaderId, storageKey, String.format("%064d", id));
    }

    private void authenticate(long userId, String username, String... roles) {
        var principal = new AuthenticatedUser(userId, username, username, 1L, Set.of(roles));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }

    private static String requiredEnvironment(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for MySQL integration tests");
        }
        return value;
    }
}
