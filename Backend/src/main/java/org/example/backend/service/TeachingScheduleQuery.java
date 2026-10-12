package org.example.backend.service;

import java.util.List;

public interface TeachingScheduleQuery {
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryView summary();

    List<String> listSemesters();
}
