/*
 * 实验室资源的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/LabResourceMetadataMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.DeviceCountRow;
import org.example.backend.entity.ManagerNameRow;

import java.util.List;
import java.util.Set;

public interface LabResourceMetadataMapper {
    List<ManagerNameRow> findManagerNames(@Param("managerUserIds") Set<Long> managerUserIds);

    List<DeviceCountRow> countDevicesByLabIds(@Param("labIds") Set<Long> labIds);
}
