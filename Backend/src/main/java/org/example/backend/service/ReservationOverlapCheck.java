/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import org.example.backend.entity.ReservationStatus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public record ReservationOverlapCheck(
        Long labId,
        LocalDate reservationDate,
        String timeRange,
        Set<ReservationStatus> terminalStatuses,
        Long excludingReservationId
) {
    /**
     * 构造 ReservationOverlapCheck，初始化当前实例。
     */
    public ReservationOverlapCheck {
        Objects.requireNonNull(labId, "Laboratory ID is required");
        Objects.requireNonNull(reservationDate, "Reservation date is required");
        Objects.requireNonNull(timeRange, "Time range is required");
        terminalStatuses = Set.copyOf(Objects.requireNonNull(terminalStatuses, "Terminal statuses are required"));
        if (terminalStatuses.isEmpty()) {
            throw new IllegalArgumentException("Terminal statuses must not be empty");
        }
    }
}
