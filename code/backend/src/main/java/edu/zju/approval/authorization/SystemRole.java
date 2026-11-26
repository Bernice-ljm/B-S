package edu.zju.approval.authorization;

import java.util.Arrays;
import java.util.Optional;

public enum SystemRole {
    EMPLOYEE,
    DEPARTMENT_MANAGER,
    FINANCE_APPROVER,
    ENTERPRISE_APPROVER,
    SYSTEM_ADMIN;

    public String roleKey() { return name(); }
    public String authority() { return "ROLE_" + roleKey(); }

    public static Optional<SystemRole> fromRoleKey(String roleKey) {
        if (roleKey == null) return Optional.empty();
        return Arrays.stream(values()).filter(role -> role.roleKey().equals(roleKey)).findFirst();
    }
}
