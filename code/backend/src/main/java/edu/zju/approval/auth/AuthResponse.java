package edu.zju.approval.auth;

import edu.zju.approval.organization.DepartmentRecord;

import java.util.Set;

public record AuthResponse(
        long userId,
        String username,
        String fullName,
        String email,
        String status,
        DepartmentRecord department,
        Set<String> roleKeys) {
}
