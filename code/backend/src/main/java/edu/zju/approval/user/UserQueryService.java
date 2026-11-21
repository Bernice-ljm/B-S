package edu.zju.approval.user;

import edu.zju.approval.organization.DepartmentMapper;
import edu.zju.approval.role.RoleMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;

    public UserQueryService(
            UserMapper userMapper,
            DepartmentMapper departmentMapper,
            RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
    }

    public Optional<UserRecord> findAuthenticationRecordByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public Optional<UserProfile> findProfileByUsername(String username) {
        return userMapper.findByUsername(username).map(this::toProfile);
    }

    public Optional<UserProfile> findProfileById(long id) {
        return userMapper.findById(id).map(this::toProfile);
    }

    private UserProfile toProfile(UserRecord user) {
        var department = user.departmentId() == null
                ? null
                : departmentMapper.findById(user.departmentId()).orElse(null);
        return new UserProfile(
                user.id(),
                user.username(),
                user.fullName(),
                user.email(),
                user.status(),
                user.createdAt(),
                user.updatedAt(),
                department,
                roleMapper.findByUserId(user.id()));
    }
}
