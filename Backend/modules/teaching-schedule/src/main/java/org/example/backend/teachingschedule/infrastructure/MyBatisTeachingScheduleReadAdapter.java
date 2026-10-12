package org.example.backend.teachingschedule.infrastructure;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.application.ScheduleSummaryData;
import org.example.backend.teachingschedule.application.ScheduleCalendarData;
import org.example.backend.teachingschedule.application.ScheduleOccupancyCriteria;
import org.example.backend.teachingschedule.application.ScheduleOccupancyRow;
import org.example.backend.teachingschedule.application.TeachingScheduleReadPort;

import java.util.List;

public final class MyBatisTeachingScheduleReadAdapter implements TeachingScheduleReadPort {
    private final TeachingScheduleReadMapper mapper;

    public MyBatisTeachingScheduleReadAdapter(TeachingScheduleReadMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ScheduleCalendarData findActiveCalendar() {
        return mapper.findActiveCalendar();
    }

    @Override
    public List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria criteria) {
        return mapper.findOccupancyCandidates(criteria);
    }

    @Override
    public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) {
        return mapper.search(criteria);
    }

    @Override
    public ScheduleSummaryData summary() {
        return mapper.summary();
    }

    @Override
    public List<String> listSemesters() {
        return mapper.listSemesters();
    }
}
