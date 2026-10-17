/*
 * 课表与时间占用数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.time.Instant;

public record ScheduleSnapshot(Long teachingTaskId, String courseName, String teacherName, Instant startsAt, Instant endsAt) {
}
