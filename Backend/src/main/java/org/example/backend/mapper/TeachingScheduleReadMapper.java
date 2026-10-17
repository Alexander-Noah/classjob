/*
 * 教学课表查询的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/TeachingScheduleReadMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.example.backend.service.ScheduleEntryView;
import org.example.backend.service.ScheduleSearchCriteria;
import org.example.backend.service.ScheduleSummaryData;
import org.example.backend.service.ScheduleCalendarData;
import org.example.backend.service.ScheduleOccupancyCriteria;
import org.example.backend.service.ScheduleOccupancyRow;

import java.util.List;

public interface TeachingScheduleReadMapper {
    /**
     * 查找活动Calendar。
     * @return ScheduleCalendarData 类型的处理结果。
     */
    ScheduleCalendarData findActiveCalendar();

    /**
     * 查找OccupancyCandidates。
     * @param criteria criteria参数。
     * @return 当前操作得到的记录集合。
     */
    List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria criteria);

    /**
     * 搜索当前操作。
     * @param criteria criteria参数。
     * @return 当前操作得到的记录集合。
     */
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    /**
     * 摘要的处理逻辑。
     * @return ScheduleSummaryData 类型的处理结果。
     */
    ScheduleSummaryData summary();

    /**
     * 列出学期。
     * @return 当前操作得到的记录集合。
     */
    List<String> listSemesters();
}
