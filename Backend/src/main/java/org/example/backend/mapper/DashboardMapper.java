/*
 * 工作台统计的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/DashboardMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {
    /**
     * 统计实验室。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countLabs(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计Open实验室。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countOpenLabs(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计Using实验室。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countUsingLabs(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计设备。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countDevices(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计Online设备。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countOnlineDevices(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计Abnormal设备。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countAbnormalDevices(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计待处理预约。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countPendingReservations(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计Today预约。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countTodayReservations(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计ApprovedToday预约。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countApprovedTodayReservations(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计活动Repairs。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countActiveRepairs(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计HighPriorityRepairs。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countHighPriorityRepairs(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计Today课程。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countTodayCourses(@Param("managerUserId") Integer managerUserId);

    /**
     * 统计StartedToday课程。
     * @param managerUserId manager用户标识参数。
     * @return long 类型的处理结果。
     */
    long countStartedTodayCourses(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出活动实验室标识集合。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Long> listActiveLabIds(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出实验室Usage。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listLabUsage(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出设备状态。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listDeviceStatus(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出通知。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listNotices();

    /**
     * 列出DoorAlerts。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listDoorAlerts(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出Today课表。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listTodaySchedules(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出Todo条目集合。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listTodoItems(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出记录Reminders。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listRecordReminders(@Param("managerUserId") Integer managerUserId);

    /**
     * 列出RecentActivities。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listRecentActivities(@Param("managerUserId") Integer managerUserId);
}
