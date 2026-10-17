/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import org.example.backend.entity.ReservationStatus;

import java.util.Objects;
import java.util.Set;

public record ReservationReviewUpdate(
        long reservationId,
        ReservationStatus status,
        boolean conflictFlag,
        String reviewerName,
        String reviewRemark,
        Set<ReservationStatus> allowedCurrentStatuses
) {
    /**
     * 构造 ReservationReviewUpdate，初始化当前实例。
     */
    public ReservationReviewUpdate {
        Objects.requireNonNull(status, "Review status is required");
        allowedCurrentStatuses = Set.copyOf(Objects.requireNonNull(allowedCurrentStatuses, "Allowed statuses are required"));
        if (allowedCurrentStatuses.isEmpty()) {
            throw new IllegalArgumentException("Allowed statuses must not be empty");
        }
    }
}
