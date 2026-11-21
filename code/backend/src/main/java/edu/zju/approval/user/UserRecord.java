package edu.zju.approval.user;

import java.time.LocalDateTime;

public record UserRecord(
        Long id,
        Long departmentId,
        String username,
        String fullName,
        String email,
        String passwordHash,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
