package org.example.backend.teachingschedule.adapter.out.persistence;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.application.port.out.ScheduleSummaryData;
import org.example.backend.teachingschedule.application.port.out.TeachingScheduleReadPort;

import java.util.List;

public final class MyBatisTeachingScheduleReadAdapter implements TeachingScheduleReadPort {
    private final TeachingScheduleReadMapper mapper;

    public MyBatisTeachingScheduleReadAdapter(TeachingScheduleReadMapper mapper) {
        this.mapper = mapper;
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
