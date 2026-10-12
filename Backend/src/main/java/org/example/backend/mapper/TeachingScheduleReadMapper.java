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
