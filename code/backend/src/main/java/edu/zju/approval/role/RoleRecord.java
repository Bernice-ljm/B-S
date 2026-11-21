package edu.zju.approval.role;

import java.time.LocalDateTime;

public record RoleRecord(
        Long id,
        String roleKey,
        String displayName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
