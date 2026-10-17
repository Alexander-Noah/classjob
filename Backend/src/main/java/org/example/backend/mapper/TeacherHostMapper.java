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
    /**
     * upsertHeartbeat的处理逻辑。
     * @param host 主机参数。
     * @return int 类型的处理结果。
     */
    int upsertHeartbeat(TeacherHostEntity host);

    /**
     * 选择当前By实验室标识。
     * @param labId 实验室标识参数。
     * @return TeacherHostEntity 类型的处理结果。
     */
    TeacherHostEntity selectCurrentByLabId(@Param("labId") Long labId);

    /**
     * 标记Offline。
     * @param teacherDeviceId 教师设备标识参数。
     * @param labId 实验室标识参数。
     * @return int 类型的处理结果。
     */
    int markOffline(@Param("teacherDeviceId") String teacherDeviceId, @Param("labId") Long labId);
}
