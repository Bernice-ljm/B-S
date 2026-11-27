package edu.zju.approval.authorization;

import edu.zju.approval.application.domain.ApplicationStatus;
import edu.zju.approval.auth.AuthenticatedUser;
import edu.zju.approval.auth.CurrentUser;
import edu.zju.approval.workflow.runtime.WorkflowInstanceStatus;
import edu.zju.approval.workflow.runtime.WorkflowNodeStatus;
import org.springframework.stereotype.Service;


@Service
public class AuthorizationService {
    public boolean hasRole(SystemRole role) {
        return CurrentUser.get().map(user -> hasRole(user, role)).orElse(false);
    }

    boolean hasRole(AuthenticatedUser user, SystemRole role) {
        return user != null && role != null && user.roleKeys().contains(role.roleKey());
    }

    public boolean canViewApplication(ApplicationAccessContext context) {
        return CurrentUser.get().map(user -> canViewApplication(user, context)).orElse(false);
    }

    boolean canViewApplication(AuthenticatedUser user, ApplicationAccessContext context) {
        if (user == null || context == null) return false;
        if (user.userId() == context.application().applicantId()
                || context.relationship() == ApplicationAccessRelationship.EXPLICIT_READ_GRANT) return true;
        return context.application().status() == ApplicationStatus.IN_APPROVAL
                && isCurrentAssignee(context, user.userId());
    }

    public boolean canEditDraft(ApplicationAccessContext context) {
        return CurrentUser.get().map(user -> canEditDraft(user, context)).orElse(false);
    }

    boolean canEditDraft(AuthenticatedUser user, ApplicationAccessContext context) {
        return user != null && context != null
                && context.application().status() == ApplicationStatus.DRAFT
                && user.userId() == context.application().applicantId();
    }

    public boolean canApproveTask(ApplicationAccessContext context) {
        return CurrentUser.get().map(user -> canApproveTask(user, context)).orElse(false);
    }

    boolean canApproveTask(AuthenticatedUser user, ApplicationAccessContext context) {
        return user != null && context != null
                && context.application().status() == ApplicationStatus.IN_APPROVAL
                && user.userId() != context.application().applicantId()
                && isCurrentAssignee(context, user.userId());
    }

    public boolean canDownloadAttachment(AttachmentAccessContext context) {
        return CurrentUser.get().map(user -> canDownloadAttachment(user, context)).orElse(false);
    }

    boolean canDownloadAttachment(AuthenticatedUser user, AttachmentAccessContext context) {
        return context != null && canViewApplication(user, context.owningApplication());
    }

    private static boolean isCurrentAssignee(ApplicationAccessContext context, long userId) {
        var instance = context.workflowAccess();
        if (instance == null || instance.applicationId() != context.application().id()
                || instance.instanceStatus() != WorkflowInstanceStatus.PENDING || instance.currentNodeId() == null) {
            return false;
        }
        if (!instance.currentNodeId().equals(instance.nodeId())
                || instance.nodeStatus() != WorkflowNodeStatus.PENDING) {
            return false;
        }
        return instance.currentTasks().stream()
                .anyMatch(task -> task.assigneeId() == userId);
    }
}
