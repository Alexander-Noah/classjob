/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import org.example.backend.entity.ReservationStatus;

import java.time.LocalDate;
import java.util.Objects;

public record NewReservationRecord(
        Long applicantUserId,
        String applicantName,
        String department,
        String contact,
        Long labId,
        LocalDate reservationDate,
        String timeRange,
        String scene,
        Integer attendeeCount,
        String reason,
        ReservationStatus status,
        boolean conflictFlag,
        String reviewerName,
        String reviewRemark
) {
    public NewReservationRecord {
        Objects.requireNonNull(status, "Reservation status is required");
    }
}
