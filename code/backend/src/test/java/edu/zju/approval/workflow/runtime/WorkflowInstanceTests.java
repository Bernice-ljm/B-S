package edu.zju.approval.workflow.runtime;

import edu.zju.approval.application.domain.ApplicationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowInstanceTests {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Test
    void applicationAndWorkflowStatusesRemainIndependent() {
        assertThat(ApplicationStatus.IN_APPROVAL.name()).isNotEqualTo(WorkflowInstanceStatus.PENDING.name());
    }

    @Test
    void sequentialApprovalActivatesOnlyTheNextNodeAndThenCompletes() {
        WorkflowInstance instance = instance(101L, 202L);
        assertThat(instance.nodes()).extracting(WorkflowNode::status)
                .containsExactly(WorkflowNodeStatus.PENDING, WorkflowNodeStatus.WAITING);

        instance.approveCurrent(101L, 999L, "ok", CREATED.plusMinutes(1));
        assertThat(instance.nodes()).extracting(WorkflowNode::status)
                .containsExactly(WorkflowNodeStatus.APPROVED, WorkflowNodeStatus.PENDING);
        assertThat(instance.currentNodeId()).isEqualTo(2L);

        instance.approveCurrent(202L, 999L, "ok", CREATED.plusMinutes(2));
        assertThat(instance.status()).isEqualTo(WorkflowInstanceStatus.APPROVED);
        assertThat(instance.currentNodeId()).isNull();
        assertThat(instance.completedAt()).isEqualTo(CREATED.plusMinutes(2));
    }

    @Test
    void waitingAndCompletedNodesCannotBeHandled() {
        WorkflowInstance instance = instance(101L, 202L);
        assertThatThrownBy(() -> instance.nodes().get(1)
                .approve(202L, 999L, "early", CREATED.plusMinutes(1)))
                .isInstanceOf(InvalidWorkflowTransitionException.class);

        instance.approveCurrent(101L, 999L, "ok", CREATED.plusMinutes(1));
        assertThatThrownBy(() -> instance.nodes().get(0)
                .approve(101L, 999L, "again", CREATED.plusMinutes(2)))
                .isInstanceOf(InvalidWorkflowTransitionException.class);
    }

    @Test
    void onlySnapshotAssigneeMayApproveAndApplicantCannotSelfApprove() {
        WorkflowInstance instance = instance(101L, 202L);
        assertThatThrownBy(() -> instance.approveCurrent(777L, 999L, "no", CREATED.plusMinutes(1)))
                .isInstanceOf(InvalidWorkflowTransitionException.class);
        assertThatThrownBy(() -> instance.approveCurrent(101L, 101L, "self", CREATED.plusMinutes(1)))
                .isInstanceOf(InvalidWorkflowTransitionException.class);
        assertThat(instance.nodes().get(0).task()).isPresent().get()
                .extracting(WorkflowTask::assigneeId).isEqualTo(101L);
    }

    @Test
    void rejectionAndWithdrawalCancelAllUnfinishedNodes() {
        WorkflowInstance rejected = instance(101L, 202L);
        rejected.rejectCurrent(101L, 999L, "reject", CREATED.plusMinutes(1));
        assertThat(rejected.status()).isEqualTo(WorkflowInstanceStatus.REJECTED);
        assertThat(rejected.currentNodeId()).isNull();
        assertThat(rejected.nodes()).extracting(WorkflowNode::status)
                .containsExactly(WorkflowNodeStatus.REJECTED, WorkflowNodeStatus.CANCELLED);

        WorkflowInstance withdrawn = instance(101L, 202L);
        withdrawn.withdraw(CREATED.plusMinutes(1));
        assertThat(withdrawn.status()).isEqualTo(WorkflowInstanceStatus.WITHDRAWN);
        assertThat(withdrawn.currentNodeId()).isNull();
        assertThat(withdrawn.nodes()).extracting(WorkflowNode::status)
                .containsExactly(WorkflowNodeStatus.CANCELLED, WorkflowNodeStatus.CANCELLED);
    }

    @Test
    void rejectsMultiplePendingNodesAndKeepsAssigneeSnapshot() {
        WorkflowTask firstTask = task(11L, 101L, WorkflowTaskStatus.PENDING, null);
        WorkflowTask secondTask = task(12L, 202L, WorkflowTaskStatus.PENDING, null);
        List<WorkflowNode> invalid = List.of(
                node(1L, 1, WorkflowNodeStatus.PENDING, firstTask, null),
                node(2L, 2, WorkflowNodeStatus.PENDING, secondTask, null));

        assertThatThrownBy(() -> new WorkflowInstance(1L, 50L, null, "PROCUREMENT_V1",
                WorkflowInstanceStatus.PENDING, 1L, 0, CREATED, CREATED, null, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most one pending");

        WorkflowInstance valid = instance(101L, 202L);
        long frozenAssignee = valid.nodes().get(0).task().orElseThrow().assigneeId();
        long laterPositionAssignment = 303L;
        assertThat(frozenAssignee).isEqualTo(101L).isNotEqualTo(laterPositionAssignment);
    }

    @Test
    void waitingNodeWithoutCreatedTaskCanBeCancelled() {
        WorkflowTask firstTask = task(11L, 101L, WorkflowTaskStatus.PENDING, null);
        WorkflowNode waitingWithoutTask = node(2L, 2, WorkflowNodeStatus.WAITING, null, null);
        WorkflowInstance instance = new WorkflowInstance(1L, 50L, null, "PROCUREMENT_V1",
                WorkflowInstanceStatus.PENDING, 1L, 0, CREATED, CREATED, null,
                List.of(node(1L, 1, WorkflowNodeStatus.PENDING, firstTask, null), waitingWithoutTask));

        instance.withdraw(CREATED.plusMinutes(1));

        assertThat(waitingWithoutTask.status()).isEqualTo(WorkflowNodeStatus.CANCELLED);
        assertThat(waitingWithoutTask.task()).isEmpty();
    }

    @Test
    void rejectsNodeTaskStateMismatchAndTerminalInstancesWithUnfinishedNodes() {
        WorkflowTask pendingTask = task(11L, 101L, WorkflowTaskStatus.PENDING, null);
        assertThatThrownBy(() -> node(1L, 1, WorkflowNodeStatus.APPROVED, pendingTask, CREATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("states must agree");

        WorkflowTask waitingTask = task(12L, 202L, WorkflowTaskStatus.PENDING, null);
        WorkflowNode waiting = node(2L, 2, WorkflowNodeStatus.WAITING, waitingTask, null);
        assertThatThrownBy(() -> new WorkflowInstance(1L, 50L, null, "PROCUREMENT_V1",
                WorkflowInstanceStatus.APPROVED, null, 0, CREATED, CREATED, CREATED, List.of(waiting)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unfinished node");
    }

    @Test
    void immutableApprovalLogUsesApplicationStatusAndResolvesInstanceThroughNode() {
        WorkflowNode node = instance(101L, 202L).nodes().get(0);
        ApprovalLog log = new ApprovalLog(1L, 50L, 1L, 11L, 90L, 101L,
                ApprovalAction.SUBMIT, null, null, null, ApplicationStatus.IN_APPROVAL,
                "request-1", CREATED);
        assertThat(log.resultStatus()).isEqualTo(ApplicationStatus.IN_APPROVAL);
        assertThat(log.workflowInstanceId(node)).isEqualTo(1L);
    }

    private WorkflowInstance instance(long firstAssignee, long secondAssignee) {
        WorkflowTask firstTask = task(11L, firstAssignee, WorkflowTaskStatus.PENDING, null);
        WorkflowTask secondTask = task(12L, secondAssignee, WorkflowTaskStatus.PENDING, null);
        return new WorkflowInstance(1L, 50L, null, "PROCUREMENT_V1",
                WorkflowInstanceStatus.PENDING, 1L, 0, CREATED, CREATED, null,
                List.of(
                        node(1L, 1, WorkflowNodeStatus.PENDING, firstTask, null),
                        node(2L, 2, WorkflowNodeStatus.WAITING, secondTask, null)));
    }

    private WorkflowNode node(Long id, int sequence, WorkflowNodeStatus status,
                              WorkflowTask task, LocalDateTime actedAt) {
        return new WorkflowNode(id, 1L, sequence, "Node " + sequence, "{\"duty\":\"FINANCE\"}",
                status, task, actedAt, CREATED);
    }

    private WorkflowTask task(Long id, long assignee, WorkflowTaskStatus status, LocalDateTime actedAt) {
        return new WorkflowTask(id, id - 10L, assignee, status, null, actedAt, CREATED);
    }
}
