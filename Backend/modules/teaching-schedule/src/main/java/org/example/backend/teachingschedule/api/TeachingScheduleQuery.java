package org.example.backend.teachingschedule.api;

import java.util.List;

public interface TeachingScheduleQuery {
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryView summary();

    List<String> listSemesters();
}
