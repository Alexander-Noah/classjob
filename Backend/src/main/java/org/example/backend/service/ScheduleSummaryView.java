/*
 * 课表与时间占用数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleSummaryView(
        long total,
        long classCount,
        long teacherCount,
        long classroomCount,
        LocalDateTime latestSyncAt,
        List<String> semesters
) {
    /**
     * 构造 ScheduleSummaryView，初始化当前实例。
     */
    public ScheduleSummaryView {
        semesters = semesters == null ? List.of() : List.copyOf(semesters);
    }
}
