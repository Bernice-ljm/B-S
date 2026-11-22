package edu.zju.approval.auth;

import java.io.Serializable;
import java.util.Set;

public record AuthenticatedUser(
        long userId,
        String username,
        String fullName,
        Long departmentId,
        Set<String> roleKeys) implements Serializable {

    public AuthenticatedUser {
        roleKeys = Set.copyOf(roleKeys);
    }
}
