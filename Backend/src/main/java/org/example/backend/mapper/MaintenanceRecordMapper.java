/*
 * 维护记录的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/MaintenanceRecordMapper.xml，修改参数或字段需同步接口与 XML。
 */
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

    Long findDeviceId(@Param("reference") String reference, @Param("labId") Long labId);

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
