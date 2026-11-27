package edu.zju.approval.authorization;

import edu.zju.approval.workflow.runtime.WorkflowInstance;
import edu.zju.approval.workflow.runtime.WorkflowInstanceStatus;
import edu.zju.approval.workflow.runtime.WorkflowNodeStatus;
import edu.zju.approval.workflow.runtime.WorkflowTaskStatus;

import java.util.List;
import java.util.Objects;

public record WorkflowAccessSnapshot(
        long instanceId,
        long applicationId,
        WorkflowInstanceStatus instanceStatus,
        Long currentNodeId,
        Long nodeId,
        WorkflowNodeStatus nodeStatus,
        List<PersistedWorkflowTaskAccess> currentTasks) {

    public WorkflowAccessSnapshot {
        Objects.requireNonNull(instanceStatus, "instanceStatus");
        currentTasks = List.copyOf(Objects.requireNonNull(currentTasks, "currentTasks"));
    }

    static WorkflowAccessSnapshot fromDomain(WorkflowInstance instance) {
        if (instance == null) {
            return null;
        }
        var currentNode = instance.nodes().stream()
                .filter(node -> Objects.equals(node.id(), instance.currentNodeId()))
                .findFirst()
                .orElse(null);
        var tasks = currentNode == null ? List.<PersistedWorkflowTaskAccess>of()
                : currentNode.task()
                .filter(task -> task.status() == WorkflowTaskStatus.PENDING)
                .map(task -> List.of(new PersistedWorkflowTaskAccess(
                        task.id(), task.nodeId(), task.assigneeId(), task.status(),
                        task.opinion(), task.actedAt(), task.createdAt())))
                .orElseGet(List::of);
        return new WorkflowAccessSnapshot(
                Objects.requireNonNull(instance.id(), "instance.id"), instance.applicationId(),
                instance.status(), instance.currentNodeId(),
                currentNode == null ? null : currentNode.id(),
                currentNode == null ? null : currentNode.status(), tasks);
    }
}
