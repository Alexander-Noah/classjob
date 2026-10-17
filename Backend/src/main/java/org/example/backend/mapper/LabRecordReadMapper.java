/*
 * 实验室的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/LabRecordReadMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.backend.service.LabRecord;

import java.util.List;

public interface LabRecordReadMapper {
    List<LabRecord> findAll(@Param("managerUserId") Long managerUserId);

    LabRecord findById(@Param("id") long id, @Param("managerUserId") Long managerUserId);
}
