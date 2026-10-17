/*
 * 教学课表查询实现模块。
 * 主要协作组件：TeachingScheduleReadMapper。
 */
package org.example.backend.service;

import org.example.backend.mapper.TeachingScheduleReadMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class TeachingScheduleReadService {
    private final TeachingScheduleReadMapper mapper;

    /**
     * 构造 TeachingScheduleReadService，初始化当前实例。
     * @param mapper 数据映射器参数。
     */
    public TeachingScheduleReadService(TeachingScheduleReadMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查找活动Calendar。
     * @return ScheduleCalendarData 类型的处理结果。
     */
    public ScheduleCalendarData findActiveCalendar() {
        return mapper.findActiveCalendar();
    }

    /**
     * 查找OccupancyCandidates。
     * @param criteria criteria参数。
     * @return 当前操作得到的记录集合。
     */
    public List<ScheduleOccupancyRow> findOccupancyCandidates(ScheduleOccupancyCriteria criteria) {
        return mapper.findOccupancyCandidates(criteria);
    }

    /**
     * 搜索当前操作。
     * @param criteria criteria参数。
     * @return 当前操作得到的记录集合。
     */
    public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) {
        return mapper.search(criteria);
    }

    /**
     * 摘要的处理逻辑。
     * @return ScheduleSummaryData 类型的处理结果。
     */
    public ScheduleSummaryData summary() {
        return mapper.summary();
    }

    /**
     * 列出学期。
     * @return 当前操作得到的记录集合。
     */
    public List<String> listSemesters() {
        return mapper.listSemesters();
    }
}
