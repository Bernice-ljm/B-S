package edu.zju.approval.authorization;

import edu.zju.approval.application.domain.Application;
import edu.zju.approval.application.domain.ApplicationStatus;
import edu.zju.approval.application.domain.ApplicationType;
import edu.zju.approval.auth.AuthenticatedUser;
import edu.zju.approval.workflow.runtime.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServiceTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final AuthorizationService service = new AuthorizationService();
    private final AuthenticatedUser applicant = user(10, "EMPLOYEE");
    private final AuthenticatedUser assignee = user(20, "EMPLOYEE", "FINANCE_APPROVER");
    private final AuthenticatedUser futureAssignee = user(21, "ENTERPRISE_APPROVER");
    private final AuthenticatedUser administrator = user(30, "SYSTEM_ADMIN");

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void roleChecksUseCurrentAuthenticatedUserAndRejectUnknownRoles() {
        authenticate(assignee);
        assertThat(service.hasRole(SystemRole.FINANCE_APPROVER)).isTrue();
        assertThat(service.hasRole(SystemRole.SYSTEM_ADMIN)).isFalse();
        assertThat(service.hasRole(null)).isFalse();
        assertThat(SystemRole.fromRoleKey("NOT_A_ROLE")).isEmpty();
        assertThat(SystemRole.FINANCE_APPROVER.authority()).isEqualTo("ROLE_FINANCE_APPROVER");
    }

    @Test
    void unauthenticatedUserReceivesNoAuthorization() {
        var context = context(application(1, ApplicationStatus.DRAFT, 10), null);
        assertThat(service.hasRole(SystemRole.EMPLOYEE)).isFalse();
        assertThat(service.canViewApplication(context)).isFalse();
        assertThat(service.canEditDraft(context)).isFalse();
        assertThat(service.canApproveTask(context)).isFalse();
        assertThat(service.canDownloadAttachment(new AttachmentAccessContext(context))).isFalse();
    }

    @Test
    void viewRequiresApplicantCurrentAssigneeOrExplicitBusinessGrant() {
        var application = application(1, ApplicationStatus.IN_APPROVAL, 10);
        var workflow = pendingWorkflow(1, 20, 21);
        var context = context(application, workflow);
        assertThat(service.canViewApplication(applicant, context)).isTrue();
        assertThat(service.canViewApplication(assignee, context)).isTrue();
        assertThat(service.canViewApplication(futureAssignee, context)).isFalse();
        assertThat(service.canViewApplication(administrator, context)).isFalse();
        assertThat(service.canViewApplication(administrator, new ApplicationAccessContext(
                application, workflow, ApplicationAccessRelationship.EXPLICIT_READ_GRANT))).isTrue();
        assertThat(service.canViewApplication(administrator, context(application, null))).isFalse();
    }

    @Test
    void mismatchedWorkflowCannotGrantViewOrApproval() {
        var application = application(1, ApplicationStatus.IN_APPROVAL, 10);
        var mismatch = context(application, pendingWorkflow(2, 20, 21));
        assertThat(service.canViewApplication(assignee, mismatch)).isFalse();
        assertThat(service.canApproveTask(assignee, mismatch)).isFalse();
    }

    @Test
    void onlyApplicantCanEditOwnDraftAndAdminDoesNotBypass() {
        assertThat(service.canEditDraft(applicant, context(application(1, ApplicationStatus.DRAFT, 10), null))).isTrue();
        assertThat(service.canEditDraft(assignee, context(application(1, ApplicationStatus.DRAFT, 10), null))).isFalse();
        assertThat(service.canEditDraft(applicant, context(application(1, ApplicationStatus.IN_APPROVAL, 10), null))).isFalse();
        assertThat(service.canEditDraft(administrator, context(application(1, ApplicationStatus.DRAFT, 10), null))).isFalse();
    }

    @Test
    void onlyCurrentPendingTaskAssigneeCanApprove() {
        var context = context(application(1, ApplicationStatus.IN_APPROVAL, 10), pendingWorkflow(1, 20, 21));
        assertThat(service.canApproveTask(assignee, context)).isTrue();
        assertThat(service.canApproveTask(futureAssignee, context)).isFalse();
        assertThat(service.canApproveTask(administrator, context)).isFalse();
    }

    @Test
    void applicantCannotApproveOwnApplicationEvenWhenAssigned() {
        var context = context(application(1, ApplicationStatus.IN_APPROVAL, 10), pendingWorkflow(1, 10, 21));
        assertThat(service.canApproveTask(applicant, context)).isFalse();
    }

    @Test
    void terminalWorkflowNodeAndTaskCannotBeApproved() {
        var application = application(1, ApplicationStatus.IN_APPROVAL, 10);
        assertThat(service.canApproveTask(assignee, context(application,
                terminalWorkflow(WorkflowInstanceStatus.APPROVED, WorkflowNodeStatus.APPROVED,
                        WorkflowTaskStatus.APPROVED)))).isFalse();
        assertThat(service.canApproveTask(assignee, context(application,
                terminalWorkflow(WorkflowInstanceStatus.REJECTED, WorkflowNodeStatus.REJECTED,
                        WorkflowTaskStatus.REJECTED)))).isFalse();
    }

    @Test
    void attachmentRequiresVerifiedOwningApplicationAndSameAccessRule() {
        var access = context(application(1, ApplicationStatus.IN_APPROVAL, 10), pendingWorkflow(1, 20, 21));
        assertThat(service.canDownloadAttachment(assignee, new AttachmentAccessContext(access))).isTrue();
        assertThat(service.canDownloadAttachment(administrator, new AttachmentAccessContext(access))).isFalse();
        assertThat(service.canDownloadAttachment(administrator, null)).isFalse();
    }

    private Application application(long id, ApplicationStatus status, long applicantId) {
        return new Application(id, "APP-" + id, ApplicationType.PROCUREMENT, applicantId, 1,
                "Test", 100L, status, status == ApplicationStatus.DRAFT ? null : NOW, NOW, NOW, 0);
    }

    private ApplicationAccessContext context(Application application, WorkflowInstance workflow) {
        return ApplicationAccessContext.withoutExplicitGrant(application, workflow);
    }

    private WorkflowInstance pendingWorkflow(long applicationId, long currentAssigneeId, long nextAssigneeId) {
        var current = node(1, 1, WorkflowNodeStatus.PENDING,
                task(1, 1, currentAssigneeId, WorkflowTaskStatus.PENDING));
        var next = node(2, 2, WorkflowNodeStatus.WAITING,
                task(2, 2, nextAssigneeId, WorkflowTaskStatus.PENDING));
        return new WorkflowInstance(1L, applicationId, null, "PROCUREMENT_V1",
                WorkflowInstanceStatus.PENDING, 1L, 0, NOW, NOW, null, List.of(current, next));
    }

    private WorkflowInstance terminalWorkflow(WorkflowInstanceStatus instanceStatus,
                                               WorkflowNodeStatus nodeStatus,
                                               WorkflowTaskStatus taskStatus) {
        var node = node(1, 1, nodeStatus, task(1, 1, 20, taskStatus));
        return new WorkflowInstance(1L, 1L, null, "PROCUREMENT_V1", instanceStatus,
                null, 0, NOW, NOW, NOW, List.of(node));
    }

    private WorkflowTask task(long id, long nodeId, long assigneeId, WorkflowTaskStatus status) {
        return new WorkflowTask(id, nodeId, assigneeId, status, null,
                status == WorkflowTaskStatus.PENDING ? null : NOW, NOW);
    }

    private WorkflowNode node(long id, int sequence, WorkflowNodeStatus status, WorkflowTask task) {
        return new WorkflowNode(id, 1L, sequence, "Approval " + sequence, "{}", status, task,
                status == WorkflowNodeStatus.PENDING || status == WorkflowNodeStatus.WAITING ? null : NOW, NOW);
    }

    private AuthenticatedUser user(long id, String... roles) {
        return new AuthenticatedUser(id, "user" + id, "User", 1L, Set.of(roles));
    }

    private void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
