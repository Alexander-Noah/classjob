package org.example.backend.teachingschedule.adapter.out.persistence;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.application.port.out.ScheduleSummaryData;

import java.util.List;

public interface TeachingScheduleReadMapper {
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryData summary();

    List<String> listSemesters();
}
