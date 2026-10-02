package org.example.backend.teachingschedule.application.port.out;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;

import java.util.List;

public interface TeachingScheduleReadPort {
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryData summary();

    List<String> listSemesters();
}
