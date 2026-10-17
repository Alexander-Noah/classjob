/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.time.LocalDate;

public record CreateReservationCommand(
        String applicantName,
        String department,
        String contact,
        Long labId,
        LocalDate reservationDate,
        String timeRange,
        String scene,
        Integer attendeeCount,
        String reason
) {
}
