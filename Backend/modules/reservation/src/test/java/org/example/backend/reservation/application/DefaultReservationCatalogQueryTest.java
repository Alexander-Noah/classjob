package org.example.backend.reservation.application;

import org.example.backend.reservation.api.ReservationAccessScope;
import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.ReservationPersistenceRuntimeConfiguration;
import org.example.backend.reservation.application.LaboratorySummary;
import org.example.backend.reservation.application.LaboratorySummaryPort;
import org.example.backend.reservation.application.ManagedLaboratoryIdsPort;
import org.example.backend.reservation.application.ReservationPersistencePort;
import org.example.backend.reservation.application.ReservationRecord;
import org.example.backend.reservation.application.ReservationRecordScope;
import org.example.backend.reservation.application.ReservationReviewUpdate;
import org.example.backend.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReservationCatalogQueryTest {
    @Test
    void systemAdministratorReadsAllReservationRecords() {
        RecordingReservations reservations = new RecordingReservations(List.of(record(1L, 11L, 101L)), Optional.empty());
        var query = query(reservations, userId -> Set.of(), Map.of(101L, metadata("创新实验室", "LAB-101")));

        query.list(scopeFor(ReservationActor.systemAdministrator(99L)));

        assertEquals(ReservationRecordScope.all(), reservations.listScope);
    }

    @Test
    void teacherReadsOnlyOwnReservationRecords() {
        RecordingReservations reservations = new RecordingReservations(List.of(record(1L, 23L, 101L)), Optional.empty());
        var query = query(reservations, userId -> Set.of(), Map.of(101L, metadata("创新实验室", "LAB-101")));

        query.list(scopeFor(ReservationActor.teacher(23L)));

        assertEquals(ReservationRecordScope.forApplicant(23L), reservations.listScope);
    }

    @Test
    void laboratoryAdministratorReadsOnlyManagedLaboratoryRecords() {
        RecordingReservations reservations = new RecordingReservations(List.of(record(1L, 23L, 101L)), Optional.empty());
        var query = query(reservations, userId -> Set.of(101L, 103L), Map.of(101L, metadata("创新实验室", "LAB-101")));

        query.list(scopeFor(ReservationActor.laboratoryAdministrator(7L)));

        assertEquals(ReservationRecordScope.forLaboratoryIds(Set.of(101L, 103L)), reservations.listScope);
    }

    @Test
    void laboratoryAdministratorWithoutManagedLaboratoriesRemainsEmptyScoped() {
        RecordingReservations reservations = new RecordingReservations(List.of(), Optional.empty());
        RecordingMetadata metadata = new RecordingMetadata(Map.of());
        var query = query(reservations, userId -> Set.of(), metadata);

        assertEquals(List.of(), query.list(scopeFor(ReservationActor.laboratoryAdministrator(7L))));

        assertEquals(ReservationRecordScope.forLaboratoryIds(Set.of()), reservations.listScope);
        assertFalse(metadata.called);
    }

    @Test
    void mergesShuffledLaboratoryMetadataByLaboratoryId() {
        RecordingReservations reservations = new RecordingReservations(
                List.of(record(1L, 11L, 101L), record(2L, 12L, 102L)), Optional.empty());
        Map<Long, LaboratorySummary> shuffled = new LinkedHashMap<>();
        shuffled.put(102L, metadata("化学实验室", "LAB-102"));
        shuffled.put(101L, metadata("创新实验室", "LAB-101"));
        var query = query(reservations, userId -> Set.of(), shuffled);

        List<ReservationView> views = query.list(scopeFor(ReservationActor.systemAdministrator(99L)));

        assertEquals("创新实验室", views.get(0).labName());
        assertEquals("LAB-101", views.get(0).labCode());
        assertEquals("化学实验室", views.get(1).labName());
        assertEquals("LAB-102", views.get(1).labCode());
    }

    @Test
    void rejectsLaboratoryMetadataForAnUnrequestedId() {
        RecordingReservations reservations = new RecordingReservations(List.of(record(1L, 11L, 101L)), Optional.empty());
        var query = query(reservations, userId -> Set.of(), Map.of(102L, metadata("化学实验室", "LAB-102")));

        assertThrows(IllegalStateException.class, () -> query.list(scopeFor(ReservationActor.systemAdministrator(99L))));
    }

    @Test
    void preservesReservationWhenLaboratoryMetadataIsMissing() {
        RecordingReservations reservations = new RecordingReservations(List.of(record(1L, 11L, 101L)), Optional.empty());
        var query = query(reservations, userId -> Set.of(), Map.of());

        ReservationView view = query.list(scopeFor(ReservationActor.systemAdministrator(99L))).get(0);

        assertEquals(1L, view.id());
        assertNull(view.labName());
        assertNull(view.labCode());
    }

    @Test
    void missingDetailDoesNotRequestLaboratoryMetadata() {
        RecordingReservations reservations = new RecordingReservations(List.of(), Optional.empty());
        RecordingMetadata metadata = new RecordingMetadata(Map.of());
        var query = query(reservations, userId -> Set.of(), metadata);

        assertNull(query.findById(44L, scopeFor(ReservationActor.teacher(23L))));

        assertEquals(ReservationRecordScope.forApplicant(23L), reservations.detailScope);
        assertFalse(metadata.called);
    }

    @Test
    void listAndDetailUseTheSameCompleteViewMapping() {
        ReservationRecord record = record(1L, 11L, 101L);
        RecordingReservations reservations = new RecordingReservations(List.of(record), Optional.of(record));
        var query = query(reservations, userId -> Set.of(), Map.of(101L, metadata("创新实验室", "LAB-101")));
        ReservationAccessScope scope = scopeFor(ReservationActor.systemAdministrator(99L));

        ReservationView listed = query.list(scope).get(0);
        ReservationView detailed = query.findById(1L, scope);

        assertEquals(listed, detailed);
        assertEquals(ReservationStatus.CONFLICT, detailed.status());
        assertTrue(detailed.conflictFlag());
        assertEquals("王老师", detailed.applicantName());
        assertEquals("创新实验室", detailed.labName());
    }

    @Test
    void runsCatalogReadUseCaseInsideReadOnlyTransaction() {
        Transactional transaction = DefaultReservationCatalogQuery.class.getAnnotation(Transactional.class);

        assertTrue(transaction.readOnly());
    }

    @Test
    void runtimeConfigurationWiresTheCatalogQuery() {
        RecordingReservations reservations = new RecordingReservations(List.of(), Optional.empty());
        ReservationCatalogQuery catalog = new ReservationPersistenceRuntimeConfiguration().reservationCatalogQuery(
                reservations, userId -> Set.of(), laboratoryIds -> Map.of());

        assertTrue(catalog instanceof DefaultReservationCatalogQuery);
    }

    private DefaultReservationCatalogQuery query(
            ReservationPersistencePort reservations,
            ManagedLaboratoryIdsPort managedLaboratories,
            Map<Long, LaboratorySummary> metadata
    ) {
        return query(reservations, managedLaboratories, new RecordingMetadata(metadata));
    }

    private DefaultReservationCatalogQuery query(
            ReservationPersistencePort reservations,
            ManagedLaboratoryIdsPort managedLaboratories,
            LaboratorySummaryPort metadata
    ) {
        return new DefaultReservationCatalogQuery(reservations, managedLaboratories, metadata);
    }

    private ReservationAccessScope scopeFor(ReservationActor actor) {
        return ReservationAccessScope.forActor(actor);
    }

    private ReservationRecord record(long id, long applicantUserId, long labId) {
        return new ReservationRecord(
                id, applicantUserId, "王老师", "计算机学院", "13800000000", labId,
                LocalDate.of(2026, 10, 5), "09:00-11:00", "课程实验", 30, "课程使用",
                ReservationStatus.CONFLICT, true, "李老师", "存在冲突",
                LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 1, 10, 0)
        );
    }

    private LaboratorySummary metadata(String name, String code) {
        return new LaboratorySummary(name, code);
    }

    private static final class RecordingReservations implements ReservationPersistencePort {
        private final List<ReservationRecord> records;
        private final Optional<ReservationRecord> detail;
        private ReservationRecordScope listScope;
        private ReservationRecordScope detailScope;

        private RecordingReservations(List<ReservationRecord> records, Optional<ReservationRecord> detail) {
            this.records = records;
            this.detail = detail;
        }

        @Override
        public List<ReservationRecord> findAll(ReservationRecordScope scope) {
            listScope = scope;
            return records;
        }

        @Override
        public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) {
            detailScope = scope;
            return detail;
        }

        @Override
        public long insert(org.example.backend.reservation.application.NewReservationRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countActiveOverlaps(org.example.backend.reservation.application.ReservationOverlapCheck check) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateReview(ReservationReviewUpdate update) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingMetadata implements LaboratorySummaryPort {
        private final Map<Long, LaboratorySummary> metadata;
        private boolean called;

        private RecordingMetadata(Map<Long, LaboratorySummary> metadata) {
            this.metadata = metadata;
        }

        @Override
        public Map<Long, LaboratorySummary> findByIds(Set<Long> laboratoryIds) {
            called = true;
            return metadata;
        }
    }
}
