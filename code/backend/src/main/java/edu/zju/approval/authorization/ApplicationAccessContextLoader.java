package edu.zju.approval.authorization;

import edu.zju.approval.workflow.runtime.WorkflowInstanceStatus;
import edu.zju.approval.workflow.runtime.WorkflowNodeStatus;
import edu.zju.approval.workflow.runtime.WorkflowTaskStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class ApplicationAccessContextLoader {

    private final ApplicationAccessMapper mapper;

    public ApplicationAccessContextLoader(ApplicationAccessMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<ApplicationAccessContext> loadApplication(long applicationId) {
        return mapper.findApplicationById(applicationId).map(application -> {
            var workflow = mapper.findWorkflowByApplicationId(applicationId)
                    .map(this::toWorkflowAccess)
                    .orElse(null);
            return ApplicationAccessContext.withoutExplicitGrant(application, workflow);
        });
    }

    public Optional<AttachmentAccessContext> loadAttachment(long attachmentId) {
        List<Long> owners = mapper.findBoundApplicationIdsByAttachmentId(attachmentId);
        if (owners.size() != 1) {
            return Optional.empty();
        }
        return loadApplication(owners.getFirst()).map(AttachmentAccessContext::new);
    }

    private WorkflowAccessSnapshot toWorkflowAccess(PersistedWorkflowAccess row) {
        if (row.instanceStatus() != WorkflowInstanceStatus.PENDING) {
            if (row.currentNodeId() != null || row.nodeId() != null) {
                throw new IllegalStateException("Terminal workflow contains an actionable current node");
            }
            return snapshot(row, List.of());
        }

        if (row.currentNodeId() == null
                || !row.currentNodeId().equals(row.nodeId())
                || row.sequenceNo() == null
                || row.nodeName() == null
                || row.approvalMode() == null
                || row.ruleSnapshot() == null
                || row.nodeStatus() != WorkflowNodeStatus.PENDING
                || row.nodeCreatedAt() == null
                || !(row.approvalMode().equals("SINGLE")
                     || row.approvalMode().equals("ALL")
                     || row.approvalMode().equals("ANY_ONE"))) {
            throw new IllegalStateException("Pending workflow access data is incomplete");
        }

        var tasks = mapper.findCurrentPendingTasks(row.applicationId(), row.instanceId(), row.nodeId());
        if (tasks.isEmpty() || tasks.stream().anyMatch(task ->
                task.nodeId() != row.nodeId()
                        || task.taskStatus() != WorkflowTaskStatus.PENDING
                        || task.taskCreatedAt() == null)) {
            throw new IllegalStateException("Pending workflow access tasks are incomplete");
        }
        return snapshot(row, tasks);
    }

    private WorkflowAccessSnapshot snapshot(
            PersistedWorkflowAccess row, List<PersistedWorkflowTaskAccess> tasks) {
        return new WorkflowAccessSnapshot(
                row.instanceId(), row.applicationId(), row.instanceStatus(), row.currentNodeId(),
                row.nodeId(), row.nodeStatus(), tasks);
    }
}
