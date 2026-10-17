/*
 * 实验室预约数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import org.example.backend.service.TimeRange;

import java.time.LocalDate;
/**
 * 可用性检查的输入快照。构造器检查必填项，但不验证文本区间与 Instant 区间是否一致；
 * 调用方应将同一次 ReservationTimeRangeParser 解析结果同时传给两个字段。
 *
 * @param labId 正数实验室标识，不能以名称代替
 * @param reservationDate 上海本地的预约日期
 * @param normalizedTimeRange 用于持久化重叠查询的 HH:mm-HH:mm 文本
 * @param timeRange 用于实验室/教学能力查询的绝对时间区间
 * @param attendeeCount 正整数人数，用于检查容量
 * @param excludingReservationId 可选的自身记录 ID，创建时为 null，审核时传当前 ID
 */
public record ReservationAvailabilityRequest(
        Long labId,
        LocalDate reservationDate,
        String normalizedTimeRange,
        TimeRange timeRange,
        Integer attendeeCount,
        Long excludingReservationId
) {
    /**
     * 构造 ReservationAvailabilityRequest，初始化当前实例。
     */
    public ReservationAvailabilityRequest {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException("Reservation availability requires a positive laboratory id");
        }
        if (reservationDate == null) {
            throw new IllegalArgumentException("Reservation availability requires a date");
        }
        if (normalizedTimeRange == null || normalizedTimeRange.isBlank()) {
            throw new IllegalArgumentException("Reservation availability requires a time range");
        }
        if (timeRange == null) {
            throw new IllegalArgumentException("Reservation availability requires a time range");
        }
        if (attendeeCount == null || attendeeCount <= 0) {
            throw new IllegalArgumentException("Reservation availability requires a positive attendee count");
        }
    }
}
