package edu.zju.approval.authorization;

import edu.zju.approval.application.domain.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApplicationAccessMapper {

    Optional<Application> findApplicationById(@Param("applicationId") long applicationId);

    Optional<PersistedWorkflowAccess> findWorkflowByApplicationId(
            @Param("applicationId") long applicationId);

    List<PersistedWorkflowTaskAccess> findCurrentPendingTasks(
            @Param("applicationId") long applicationId,
            @Param("instanceId") long instanceId,
            @Param("nodeId") long nodeId);

    List<Long> findBoundApplicationIdsByAttachmentId(@Param("attachmentId") long attachmentId);
}
