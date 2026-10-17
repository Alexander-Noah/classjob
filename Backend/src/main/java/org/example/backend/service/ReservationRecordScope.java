/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.util.Objects;
import java.util.Set;

public record ReservationRecordScope(Long applicantUserId, Set<Long> laboratoryIds) {
    public ReservationRecordScope {
        if (applicantUserId != null && laboratoryIds != null) {
            throw new IllegalArgumentException("A reservation scope may target an applicant or laboratories, not both");
        }
        if (laboratoryIds != null) {
            laboratoryIds = Set.copyOf(laboratoryIds);
        }
    }

    public static ReservationRecordScope all() {
        return new ReservationRecordScope(null, null);
    }

    public static ReservationRecordScope forApplicant(long applicantUserId) {
        return new ReservationRecordScope(applicantUserId, null);
    }

    public static ReservationRecordScope forLaboratoryIds(Set<Long> laboratoryIds) {
        return new ReservationRecordScope(null, Objects.requireNonNull(laboratoryIds, "Laboratory IDs are required"));
    }
}
