package edu.zju.approval.user;

import edu.zju.approval.organization.DepartmentRecord;
import edu.zju.approval.role.RoleRecord;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfile(
        Long id,
        String username,
        String fullName,
        String email,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        DepartmentRecord department,
        List<RoleRecord> roles) {

    public UserProfile {
        roles = List.copyOf(roles);
    }
}
