/*
 * 教师主机的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/TeacherHostMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.TeacherHostEntity;

@Mapper
public interface TeacherHostMapper {
    int upsertHeartbeat(TeacherHostEntity host);

    TeacherHostEntity selectCurrentByLabId(@Param("labId") Long labId);

    int markOffline(@Param("teacherDeviceId") String teacherDeviceId, @Param("labId") Long labId);
}
