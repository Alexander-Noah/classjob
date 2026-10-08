package org.example.backend.teachingschedule.adapter.out.persistence;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.application.port.out.ScheduleSummaryData;
import org.example.backend.teachingschedule.application.port.out.ScheduleCalendarData;
import org.example.backend.teachingschedule.application.port.out.ScheduleOccupancyCriteria;
import org.example.backend.teachingschedule.application.port.out.ScheduleOccupancyRow;

import java.util.List;

public interface TeachingScheduleReadMapper {
    ScheduleCalendarData findActiveCalendar();

    List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria criteria);

    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryData summary();

    List<String> listSemesters();
}
