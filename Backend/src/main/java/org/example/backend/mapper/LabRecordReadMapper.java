/*
 * 实验室的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/LabRecordReadMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.backend.service.LabRecord;

import java.util.List;

public interface LabRecordReadMapper {
    /**
     * 查找All。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<LabRecord> findAll(@Param("managerUserId") Long managerUserId);

    /**
     * 查找By标识。
     * @param id 标识参数。
     * @param managerUserId manager用户标识参数。
     * @return LabRecord 类型的处理结果。
     */
    LabRecord findById(@Param("id") long id, @Param("managerUserId") Long managerUserId);
}
