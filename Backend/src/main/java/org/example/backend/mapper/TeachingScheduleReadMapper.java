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
    ScheduleCalendarData findActiveCalendar();

    List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria criteria);

    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryData summary();

    List<String> listSemesters();
}
