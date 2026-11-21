package edu.zju.approval.organization;

import java.time.LocalDateTime;

public record DepartmentRecord(
        Long id,
        String departmentCode,
        String name,
        Long parentId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
