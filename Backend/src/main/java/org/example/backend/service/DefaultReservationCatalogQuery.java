/*
 * 实验室预约实现模块。
 * 主要协作组件：ReservationPersistenceService、ReservationLabService。
 */
package org.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DefaultReservationCatalogQuery implements ReservationCatalogQuery {
    private final ReservationPersistenceService reservations;
    private final ReservationLabService laboratories;

    /**
     * 构造 DefaultReservationCatalogQuery，初始化当前实例。
     * @param reservations 预约参数。
     * @param laboratories laboratories参数。
     */
    public DefaultReservationCatalogQuery(
            ReservationPersistenceService reservations,
            ReservationLabService laboratories
    ) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.laboratories = Objects.requireNonNull(laboratories, "laboratories");
    }

    /**
     * 列出当前操作。
     * @param scope scope参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<ReservationView> list(ReservationAccessScope scope) {
        List<ReservationRecord> found = Objects.requireNonNull(reservations.findAll(resolveScope(scope)), "reservation records");
        return enrich(found);
    }

    /**
     * 查找By标识。
     * @param id 标识参数。
     * @param scope scope参数。
     * @return ReservationView 类型的处理结果。
     */
    @Override
    public ReservationView findById(long id, ReservationAccessScope scope) {
        Optional<ReservationRecord> found = Objects.requireNonNull(
                reservations.findOne(id, resolveScope(scope)), "reservation detail");
        if (found.isEmpty()) {
            return null;
        }
        return enrich(List.of(found.get())).get(0);
    }

    /**
     * 解析Scope。
     * @param scope scope参数。
     * @return ReservationRecordScope 类型的处理结果。
     */
    private ReservationRecordScope resolveScope(ReservationAccessScope scope) {
        ReservationActor actor = Objects.requireNonNull(scope, "scope").actor();
        if (actor.role() == ReservationActorRole.SYSTEM_ADMINISTRATOR) {
            return ReservationRecordScope.all();
        }
        if (actor.role() == ReservationActorRole.TEACHER) {
            return ReservationRecordScope.forApplicant(actor.userId());
        }
        if (actor.role() == ReservationActorRole.LABORATORY_ADMINISTRATOR) {
            Set<Long> ids = Objects.requireNonNull(
                    laboratories.findManagedLaboratoryIds(actor.userId()), "managed laboratory ids");
            return ReservationRecordScope.forLaboratoryIds(Set.copyOf(ids));
        }
        throw new IllegalStateException("Unsupported reservation actor role: " + actor.role());
    }

    /**
     * enrich的处理逻辑。
     * @param found found参数。
     * @return 当前操作得到的记录集合。
     */
    private List<ReservationView> enrich(List<ReservationRecord> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        Set<Long> laboratoryIds = new HashSet<>();
        for (ReservationRecord record : found) {
            if (record == null || record.labId() == null || record.labId() <= 0) {
                throw new IllegalStateException("Reservation record must have a positive laboratory id");
            }
            laboratoryIds.add(record.labId());
        }

        Map<Long, LaboratorySummary> summaries = Objects.requireNonNull(
                laboratories.findLaboratorySummaries(Set.copyOf(laboratoryIds)), "laboratory summaries");
        validateSummaries(laboratoryIds, summaries);
        return found.stream().map(/** 将当前条目转换为目标结果。 入参：record。 */ record -> toView(record, summaries.get(record.labId()))).toList();
    }

    /**
     * 校验摘要。
     * @param requestedIds requested标识集合参数。
     * @param summaries 摘要参数。
     */
    private void validateSummaries(Set<Long> requestedIds, Map<Long, LaboratorySummary> summaries) {
        for (Map.Entry<Long, LaboratorySummary> entry : summaries.entrySet()) {
            if (!requestedIds.contains(entry.getKey()) || entry.getValue() == null) {
                throw new IllegalStateException("Unexpected laboratory summary for id: " + entry.getKey());
            }
        }
    }

    /**
     * 转换为视图。
     * @param record 记录参数。
     * @param laboratory 实验室参数。
     * @return ReservationView 类型的处理结果。
     */
    private ReservationView toView(ReservationRecord record, LaboratorySummary laboratory) {
        return new ReservationView(
                record.id(),
                record.applicantUserId(),
                record.applicantName(),
                record.department(),
                record.contact(),
                record.labId(),
                record.reservationDate(),
                record.timeRange(),
                record.scene(),
                record.attendeeCount(),
                record.reason(),
                record.status(),
                record.conflictFlag(),
                record.reviewerName(),
                record.reviewRemark(),
                record.createdAt(),
                record.updatedAt(),
                laboratory == null ? null : laboratory.name(),
                laboratory == null ? null : laboratory.code()
        );
    }
}
