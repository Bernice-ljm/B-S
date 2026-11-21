package edu.zju.approval.organization;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface DepartmentMapper {

    Optional<DepartmentRecord> findById(@Param("id") long id);

    boolean existsById(@Param("id") long id);
}
