package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.MaintenanceRecord;

import java.util.List;

@Mapper
public interface MaintenanceRecordMapper {
    List<MaintenanceRecord> listMaintenanceRecords();

    MaintenanceRecord getMaintenanceRecord(@Param("id") String id);

    Long findLabId(@Param("reference") String reference);

    Long findDeviceId(@Param("reference") String reference);

    int insertMaintenanceRecord(
            @Param("record") MaintenanceRecord record,
            @Param("labId") Long labId,
            @Param("deviceId") Long deviceId
    );

    int updateMaintenanceRecord(
            @Param("id") String id,
            @Param("record") MaintenanceRecord record,
            @Param("labId") Long labId,
            @Param("deviceId") Long deviceId
    );

    int deleteMaintenanceRecord(@Param("id") String id);
}
