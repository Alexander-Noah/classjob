package org.example.backend.service;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
public class DefaultReservationCatalogQuery implements ReservationCatalogQuery {
    private final ReservationPersistenceService reservations;
    private final ReservationLabService laboratories;

    public DefaultReservationCatalogQuery(
            ReservationPersistenceService reservations,
            ReservationLabService laboratories
    ) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.laboratories = Objects.requireNonNull(laboratories, "laboratories");
    }

    @Override
    public List<ReservationView> list(ReservationAccessScope scope) {
        List<ReservationRecord> found = Objects.requireNonNull(reservations.findAll(resolveScope(scope)), "reservation records");
        return enrich(found);
    }

    @Override
    public ReservationView findById(long id, ReservationAccessScope scope) {
        Optional<ReservationRecord> found = Objects.requireNonNull(
                reservations.findOne(id, resolveScope(scope)), "reservation detail");
        if (found.isEmpty()) {
            return null;
        }
        return enrich(List.of(found.get())).get(0);
    }

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
        return found.stream().map(record -> toView(record, summaries.get(record.labId()))).toList();
    }

    private void validateSummaries(Set<Long> requestedIds, Map<Long, LaboratorySummary> summaries) {
        for (Map.Entry<Long, LaboratorySummary> entry : summaries.entrySet()) {
            if (!requestedIds.contains(entry.getKey()) || entry.getValue() == null) {
                throw new IllegalStateException("Unexpected laboratory summary for id: " + entry.getKey());
            }
        }
    }

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
