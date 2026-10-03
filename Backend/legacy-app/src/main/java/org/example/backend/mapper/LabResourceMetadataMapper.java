package org.example.backend.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.backend.adapter.labmetadata.DeviceCountRow;
import org.example.backend.adapter.labmetadata.ManagerNameRow;

import java.util.List;
import java.util.Set;

public interface LabResourceMetadataMapper {
    List<ManagerNameRow> findManagerNames(@Param("managerUserIds") Set<Long> managerUserIds);

    List<DeviceCountRow> countDevicesByLabIds(@Param("labIds") Set<Long> labIds);
}
