/*
 * 设备管理的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/DevicesMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.DevicesEntity;

import java.util.List;
import java.util.Map;

@Mapper
public interface DevicesMapper {
    /**
     * 获取设备。
     * @return 当前操作得到的记录集合。
     */
    List<DevicesEntity> getDevices();

    /**
     * 页码设备的处理逻辑。
     * @param offset 偏移参数。
     * @param pageSize 页码大小参数。
     * @param keyword keyword参数。
     * @param labId 实验室标识参数。
     * @param labName 实验室名称参数。
     * @param category 分类参数。
     * @param status 状态参数。
     * @return 当前操作得到的记录集合。
     */
    List<DevicesEntity> pageDevices(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("keyword") String keyword,
            @Param("labId") Long labId,
            @Param("labName") String labName,
            @Param("category") String category,
            @Param("status") String status
    );

    /**
     * 统计设备。
     * @param keyword keyword参数。
     * @param labId 实验室标识参数。
     * @param labName 实验室名称参数。
     * @param category 分类参数。
     * @param status 状态参数。
     * @return long 类型的处理结果。
     */
    long countDevices(
            @Param("keyword") String keyword,
            @Param("labId") Long labId,
            @Param("labName") String labName,
            @Param("category") String category,
            @Param("status") String status
    );

    /**
     * 获取设备Stats。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getDeviceStats();

    /**
     * Inserter设备的处理逻辑。
     * @param devicesEntity 设备Entity参数。
     */
    void InserterDevices(DevicesEntity devicesEntity);

    /**
     * 获取设备By标识。
     * @param id 标识参数。
     * @return DevicesEntity 类型的处理结果。
     */
    DevicesEntity getDevicesById(@Param("id") Long id);

    /**
     * 更新设备。
     * @param id 标识参数。
     * @param devices 设备参数。
     * @return int 类型的处理结果。
     */
    int updateDevices(@Param("id") Long id, @Param("devices") DevicesEntity devices);

    /**
     * 更新设备资产维护状态By名称Or编码。
     * @param device 设备参数。
     * @param status 状态参数。
     * @param maintenance 维护参数。
     * @return int 类型的处理结果。
     */
    int updateDeviceAssetMaintenanceStateByNameOrCode(
            @Param("device") String device,
            @Param("status") String status,
            @Param("maintenance") String maintenance
    );

    /**
     * 更新库存状态。
     * @param id 标识参数。
     * @param inventoryDate 库存日期参数。
     * @return int 类型的处理结果。
     */
    int updateInventoryState(
            @Param("id") Long id,
            @Param("inventoryDate") String inventoryDate
    );

    /**
     * 更新Transfer状态。
     * @param id 标识参数。
     * @param labId 实验室标识参数。
     * @param ownerUserId 所有者用户标识参数。
     * @param location location参数。
     * @return int 类型的处理结果。
     */
    int updateTransferState(
            @Param("id") Long id,
            @Param("labId") Long labId,
            @Param("ownerUserId") Long ownerUserId,
            @Param("location") String location
    );

    /**
     * 删除设备。
     * @param id 标识参数。
     */
    void deleteDevices(@Param("id") Long id);

    /**
     * 获取设备By实验室标识。
     * @param labId 实验室标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<DevicesEntity> getDevicesByLabId(@Param("labId") Integer labId);
}
