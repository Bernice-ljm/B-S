package edu.zju.approval.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {

    Optional<UserRecord> findById(@Param("id") long id);

    Optional<UserRecord> findByUsername(@Param("username") String username);
}
