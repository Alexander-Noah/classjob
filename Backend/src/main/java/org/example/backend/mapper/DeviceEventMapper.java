/*
 * 设备事件的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/DeviceEventMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.DeviceInventoryRecordEntity;
import org.example.backend.entity.DeviceTransferRecordEntity;

import java.util.List;

@Mapper
public interface DeviceEventMapper {
    /**
     * 插入库存事件。
     * @param record 记录参数。
     * @return int 类型的处理结果。
     */
    int insertInventoryEvent(DeviceInventoryRecordEntity record);

    /**
     * 插入Transfer事件。
     * @param record 记录参数。
     * @return int 类型的处理结果。
     */
    int insertTransferEvent(DeviceTransferRecordEntity record);

    /**
     * 列出库存事件。
     * @param deviceId 设备标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<DeviceInventoryRecordEntity> listInventoryEvents(@Param("deviceId") Long deviceId);

    /**
     * 列出Transfer事件。
     * @param deviceId 设备标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<DeviceTransferRecordEntity> listTransferEvents(@Param("deviceId") Long deviceId);
}
