package org.example.backend.teachingschedule.adapter.out.persistence;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;

import java.util.List;

public interface TeachingScheduleReadMapper {
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryRow summary();

    List<String> listSemesters();
}
