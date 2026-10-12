package org.example.backend.teachingschedule.infrastructure;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.application.ScheduleSummaryData;
import org.example.backend.teachingschedule.application.ScheduleCalendarData;
import org.example.backend.teachingschedule.application.ScheduleOccupancyCriteria;
import org.example.backend.teachingschedule.application.ScheduleOccupancyRow;

import java.util.List;

public interface TeachingScheduleReadMapper {
    ScheduleCalendarData findActiveCalendar();

    List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria criteria);

    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryData summary();

    List<String> listSemesters();
}
