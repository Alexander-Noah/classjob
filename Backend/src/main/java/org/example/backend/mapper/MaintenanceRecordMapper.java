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
    /**
     * 列出维护记录集合。
     * @return 当前操作得到的记录集合。
     */
    List<MaintenanceRecord> listMaintenanceRecords();

    /**
     * 获取维护记录。
     * @param id 标识参数。
     * @return MaintenanceRecord 类型的处理结果。
     */
    MaintenanceRecord getMaintenanceRecord(@Param("id") String id);

    /**
     * 查找实验室标识。
     * @param reference 引用参数。
     * @return Long 类型的处理结果。
     */
    Long findLabId(@Param("reference") String reference);

    /**
     * 查找设备标识。
     * @param reference 引用参数。
     * @param labId 实验室标识参数。
     * @return Long 类型的处理结果。
     */
    Long findDeviceId(@Param("reference") String reference, @Param("labId") Long labId);

    /**
     * 插入维护记录。
     * @param record 记录参数。
     * @param labId 实验室标识参数。
     * @param deviceId 设备标识参数。
     * @return int 类型的处理结果。
     */
    int insertMaintenanceRecord(
            @Param("record") MaintenanceRecord record,
            @Param("labId") Long labId,
            @Param("deviceId") Long deviceId
    );

    /**
     * 更新维护记录。
     * @param id 标识参数。
     * @param record 记录参数。
     * @param labId 实验室标识参数。
     * @param deviceId 设备标识参数。
     * @return int 类型的处理结果。
     */
    int updateMaintenanceRecord(
            @Param("id") String id,
            @Param("record") MaintenanceRecord record,
            @Param("labId") Long labId,
            @Param("deviceId") Long deviceId
    );

    /**
     * 删除维护记录。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteMaintenanceRecord(@Param("id") String id);
}
