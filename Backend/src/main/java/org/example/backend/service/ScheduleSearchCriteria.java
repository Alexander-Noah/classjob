/*
 * 课表与时间占用数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

public record ScheduleSearchCriteria(
        String semester,
        String teacher,
        String className,
        String classroom,
        String courseName,
        String keyword,
        Integer week,
        Integer limit
) {
}
