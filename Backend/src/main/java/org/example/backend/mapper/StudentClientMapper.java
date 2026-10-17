/*
 * 学生客户端的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/StudentClientMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.StudentClientEntity;

import java.util.List;

@Mapper
public interface StudentClientMapper {
    int upsertOnline(StudentClientEntity client);

    int markOffline(@Param("studentDeviceId") String studentDeviceId, @Param("labId") Long labId);

    List<StudentClientEntity> selectByLabId(@Param("labId") Long labId);
}
