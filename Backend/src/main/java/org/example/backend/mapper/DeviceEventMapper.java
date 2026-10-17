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
    int insertInventoryEvent(DeviceInventoryRecordEntity record);

    int insertTransferEvent(DeviceTransferRecordEntity record);

    List<DeviceInventoryRecordEntity> listInventoryEvents(@Param("deviceId") Long deviceId);

    List<DeviceTransferRecordEntity> listTransferEvents(@Param("deviceId") Long deviceId);
}
