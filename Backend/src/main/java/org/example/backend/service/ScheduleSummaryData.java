/*
 * 课表与时间占用数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.time.LocalDateTime;

public record ScheduleSummaryData(
        long total,
        long classCount,
        long teacherCount,
        long classroomCount,
        LocalDateTime latestSyncAt
) {
}
