/*
 * 业务闭环的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/BusinessLoopReportMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BusinessLoopReportMapper {
    /**
     * 统计预约。
     * @return long 类型的处理结果。
     */
    long countReservations();

    /**
     * 统计Approved预约。
     * @return long 类型的处理结果。
     */
    long countApprovedReservations();

    /**
     * 统计待处理预约。
     * @return long 类型的处理结果。
     */
    long countPendingReservations();

    /**
     * 统计Usage记录集合。
     * @return long 类型的处理结果。
     */
    long countUsageRecords();

    /**
     * 统计Usage记录集合来源预约。
     * @return long 类型的处理结果。
     */
    long countUsageRecordsFromReservations();

    /**
     * 统计AbnormalUsage记录集合。
     * @return long 类型的处理结果。
     */
    long countAbnormalUsageRecords();

    /**
     * 统计RepairOrders。
     * @return long 类型的处理结果。
     */
    long countRepairOrders();

    /**
     * 统计RepairOrders来源Usage记录集合。
     * @return long 类型的处理结果。
     */
    long countRepairOrdersFromUsageRecords();

    /**
     * 统计活动RepairOrders。
     * @return long 类型的处理结果。
     */
    long countActiveRepairOrders();

    /**
     * 统计ClosedRepairOrders。
     * @return long 类型的处理结果。
     */
    long countClosedRepairOrders();

    /**
     * 统计设备。
     * @return long 类型的处理结果。
     */
    long countDevices();

    /**
     * 统计Normal设备。
     * @return long 类型的处理结果。
     */
    long countNormalDevices();

    /**
     * 统计维护设备。
     * @return long 类型的处理结果。
     */
    long countMaintenanceDevices();

    /**
     * 统计Fault设备。
     * @return long 类型的处理结果。
     */
    long countFaultDevices();

    /**
     * 统计课程环境请求。
     * @return long 类型的处理结果。
     */
    long countCourseEnvironmentRequests();

    /**
     * 统计Confirmed课程环境请求。
     * @return long 类型的处理结果。
     */
    long countConfirmedCourseEnvironmentRequests();

    /**
     * 统计环境模板。
     * @return long 类型的处理结果。
     */
    long countEnvironmentTemplates();

    /**
     * 统计Consumables。
     * @return long 类型的处理结果。
     */
    long countConsumables();

    /**
     * 统计Low库存Consumables。
     * @return long 类型的处理结果。
     */
    long countLowStockConsumables();

    /**
     * 统计通知。
     * @return long 类型的处理结果。
     */
    long countNotices();

    /**
     * 统计Published通知。
     * @return long 类型的处理结果。
     */
    long countPublishedNotices();

    /**
     * 统计操作事件。
     * @return long 类型的处理结果。
     */
    long countOperationEvents();

    /**
     * 列出UsageTrend。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listUsageTrend();

    /**
     * 列出实验室UsageRanking。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listLabUsageRanking();

    /**
     * 列出RepairFaultRanking。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listRepairFaultRanking();

    /**
     * 列出Latest事件JSON。
     * @param limit 上限参数。
     * @return 当前操作得到的记录集合。
     */
    List<String> listLatestEventJson(@Param("limit") int limit);
}