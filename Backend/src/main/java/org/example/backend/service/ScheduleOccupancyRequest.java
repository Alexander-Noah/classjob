/*
 * 课表与时间占用数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record ScheduleOccupancyRequest(
        LocalDate reservationDate,
        LocalTime startsAt,
        LocalTime endsAt,
        Set<String> laboratoryAliases
) {
    public ScheduleOccupancyRequest {
        laboratoryAliases = laboratoryAliases == null ? Set.of() : Set.copyOf(laboratoryAliases);
    }
}
