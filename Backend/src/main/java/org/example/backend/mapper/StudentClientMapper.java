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
    /**
     * upsertOnline的处理逻辑。
     * @param client 客户端参数。
     * @return int 类型的处理结果。
     */
    int upsertOnline(StudentClientEntity client);

    /**
     * 标记Offline。
     * @param studentDeviceId 学生设备标识参数。
     * @param labId 实验室标识参数。
     * @return int 类型的处理结果。
     */
    int markOffline(@Param("studentDeviceId") String studentDeviceId, @Param("labId") Long labId);

    /**
     * 选择By实验室标识。
     * @param labId 实验室标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<StudentClientEntity> selectByLabId(@Param("labId") Long labId);
}
