/*
 * 教学课表查询实现模块。
 * 主要协作组件：TeachingScheduleReadService。
 */
package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class DefaultTeachingScheduleQuery implements TeachingScheduleQuery {
    static final int DEFAULT_LIMIT = 500;
    static final int MAX_LIMIT = 5000;

    private final TeachingScheduleReadService readService;

    /**
     * 构造 DefaultTeachingScheduleQuery，初始化当前实例。
     * @param readService read服务参数。
     */
    public DefaultTeachingScheduleQuery(TeachingScheduleReadService readService) {
        this.readService = readService;
    }

    /**
     * 搜索当前操作。
     * @param criteria criteria参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<ScheduleEntryView> search(ScheduleSearchCriteria criteria) {
        ScheduleSearchCriteria normalized = new ScheduleSearchCriteria(
                trimToNull(criteria.semester()),
                trimToNull(criteria.teacher()),
                trimToNull(criteria.className()),
                trimToNull(criteria.classroom()),
                trimToNull(criteria.courseName()),
                trimToNull(criteria.keyword()),
                criteria.week(),
                normalizeLimit(criteria.limit())
        );
        return immutableList(readService.search(normalized));
    }

    /**
     * 摘要的处理逻辑。
     * @return ScheduleSummaryView 类型的处理结果。
     */
    @Override
    public ScheduleSummaryView summary() {
        ScheduleSummaryData row = readService.summary();
        List<String> semesters = immutableList(readService.listSemesters());
        if (row == null) {
            return new ScheduleSummaryView(0, 0, 0, 0, null, semesters);
        }
        return new ScheduleSummaryView(
                row.total(),
                row.classCount(),
                row.teacherCount(),
                row.classroomCount(),
                row.latestSyncAt(),
                semesters
        );
    }

    /**
     * 列出学期。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<String> listSemesters() {
        return immutableList(readService.listSemesters());
    }

    /**
     * 归一化上限。
     * @param limit 上限参数。
     * @return int 类型的处理结果。
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 裁剪目标Null。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * immutable列表的处理逻辑。
     * @param values 值集合参数。
     * @return 当前操作得到的记录集合。
     */
    private <T> List<T> immutableList(List<T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }
}
