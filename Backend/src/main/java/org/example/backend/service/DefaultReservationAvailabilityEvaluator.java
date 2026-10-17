/*
 * 实验室预约实现模块。
 * 主要协作组件：ReservationPersistenceService、ReservationLabService、ReservationScheduleService。
 */
package org.example.backend.service;

import org.example.backend.entity.ReservationStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Service
public final class DefaultReservationAvailabilityEvaluator implements ReservationAvailabilityEvaluator {
    private static final Set<ReservationStatus> TERMINAL_OVERLAP_EXCLUSIONS = EnumSet.of(
            ReservationStatus.REJECTED, ReservationStatus.CANCELLED,
            ReservationStatus.EXPIRED, ReservationStatus.COMPLETED
    );

    private final ReservationPersistenceService reservations;
    private final ReservationLabService laboratories;
    private final ReservationScheduleService schedules;

    public DefaultReservationAvailabilityEvaluator(ReservationPersistenceService reservations,
                                                   ReservationLabService laboratories,
                                                   ReservationScheduleService schedules) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.laboratories = Objects.requireNonNull(laboratories, "laboratories");
        this.schedules = Objects.requireNonNull(schedules, "schedules");
    }

    /**
     * 汇总实验室开放/容量、教学占用与已有预约重叠三类检查，不执行预约写入。
     *
     * @param request 已规范化的预约范围；excludingReservationId 用于审核时排除自身
     * @return true 表示至少一项业务条件冲突，false 表示当前查询均通过
     * @throws IllegalStateException 能力返回空结果、返回范围错配或重叠计数异常
     * @throws NullPointerException request 为 null
     */
    @Override
    public boolean hasConflict(ReservationAvailabilityRequest request) {
        ReservationAvailabilityRequest validated = Objects.requireNonNull(request, "request");
        LabAvailabilitySnapshot laboratory = requireLaboratoryCapability(validated);
        ScheduleConflictSnapshot schedule = requireScheduleCapability(validated);
        long overlapCount = reservations.countActiveOverlaps(new ReservationOverlapCheck(
                validated.labId(), validated.reservationDate(), validated.normalizedTimeRange(),
                TERMINAL_OVERLAP_EXCLUSIONS, validated.excludingReservationId()));
        if (overlapCount < 0) {
            throw new IllegalStateException("Reservation overlap count cannot be negative");
        }
        return !laboratory.available() || laboratory.capacity() < validated.attendeeCount()
                || schedule.hasConflict() || overlapCount > 0;
    }

    private LabAvailabilitySnapshot requireLaboratoryCapability(ReservationAvailabilityRequest request) {
        CapabilityResult<LabAvailabilitySnapshot> result = laboratories.checkAvailability(
                request.labId(), request.attendeeCount(), request.timeRange());
        if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
            throw new IllegalStateException("Laboratory availability capability is unavailable");
        }
        LabAvailabilitySnapshot snapshot = result.value();
        if (!request.labId().equals(snapshot.labId())) {
            throw new IllegalStateException("Laboratory availability capability returned a mismatched laboratory");
        }
        return snapshot;
    }

    private ScheduleConflictSnapshot requireScheduleCapability(ReservationAvailabilityRequest request) {
        CapabilityResult<ScheduleConflictSnapshot> result = schedules.checkConflict(
                request.labId(), request.reservationDate(), request.timeRange());
        if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
            throw new IllegalStateException("Schedule conflict capability is unavailable");
        }
        ScheduleConflictSnapshot snapshot = result.value();
        // 不只检查 AVAILABLE，还核对结果对应原请求，防止消费缓存或适配层的错配数据。
        if (!request.labId().equals(snapshot.labId()) || !request.reservationDate().equals(snapshot.reservationDate())
                || !request.timeRange().equals(snapshot.timeRange())) {
            throw new IllegalStateException("Schedule conflict capability returned a mismatched request");
        }
        return snapshot;
    }
}
