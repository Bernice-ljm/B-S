package edu.zju.approval.role;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoleMapper {

    Optional<RoleRecord> findById(@Param("id") long id);

    Optional<RoleRecord> findByRoleKey(@Param("roleKey") String roleKey);

    List<RoleRecord> findByUserId(@Param("userId") long userId);
}
