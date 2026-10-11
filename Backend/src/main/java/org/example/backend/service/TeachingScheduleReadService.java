package org.example.backend.service;

import org.example.backend.mapper.TeachingScheduleReadMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class TeachingScheduleReadService {
    private final TeachingScheduleReadMapper mapper;

    public TeachingScheduleReadService(TeachingScheduleReadMapper mapper) {
        this.mapper = mapper;
    }

    public ScheduleCalendarData findActiveCalendar() {
        return mapper.findActiveCalendar();
    }

    public List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria criteria) {
        return mapper.findOccupancyCandidates(criteria);
    }

    public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) {
        return mapper.search(criteria);
    }

    public ScheduleSummaryData summary() {
        return mapper.summary();
    }

    public List<String> listSemesters() {
        return mapper.listSemesters();
    }
}
