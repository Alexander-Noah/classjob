/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.util.Objects;
import java.util.Set;

public record ReservationRecordScope(Long applicantUserId, Set<Long> laboratoryIds) {
    /**
     * 构造 ReservationRecordScope，初始化当前实例。
     */
    public ReservationRecordScope {
        if (applicantUserId != null && laboratoryIds != null) {
            throw new IllegalArgumentException("A reservation scope may target an applicant or laboratories, not both");
        }
        if (laboratoryIds != null) {
            laboratoryIds = Set.copyOf(laboratoryIds);
        }
    }

    /**
     * all的处理逻辑。
     * @return ReservationRecordScope 类型的处理结果。
     */
    public static ReservationRecordScope all() {
        return new ReservationRecordScope(null, null);
    }

    /**
     * forApplicant的处理逻辑。
     * @param applicantUserId applicant用户标识参数。
     * @return ReservationRecordScope 类型的处理结果。
     */
    public static ReservationRecordScope forApplicant(long applicantUserId) {
        return new ReservationRecordScope(applicantUserId, null);
    }

    /**
     * forLaboratory标识集合的处理逻辑。
     * @param laboratoryIds laboratory标识集合参数。
     * @return ReservationRecordScope 类型的处理结果。
     */
    public static ReservationRecordScope forLaboratoryIds(Set<Long> laboratoryIds) {
        return new ReservationRecordScope(null, Objects.requireNonNull(laboratoryIds, "Laboratory IDs are required"));
    }
}
