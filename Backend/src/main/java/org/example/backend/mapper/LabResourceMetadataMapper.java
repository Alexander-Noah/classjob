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
