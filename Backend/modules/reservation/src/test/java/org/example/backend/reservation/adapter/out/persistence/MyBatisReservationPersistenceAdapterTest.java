package org.example.backend.reservation.adapter.out.persistence;

import org.example.backend.reservation.application.port.out.NewReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationPersistencePort;
import org.example.backend.reservation.application.port.out.ReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationRecordScope;
import org.example.backend.reservation.application.port.out.ReservationReviewUpdate;
import org.example.backend.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBatisReservationPersistenceAdapterTest {
    @Test
    void normalizesNullListsAndDelegatesScopeUnchanged() {
        RecordingMapper mapper = new RecordingMapper();
        ReservationPersistencePort port = new MyBatisReservationPersistenceAdapter(mapper);
        ReservationRecordScope scope = ReservationRecordScope.forLaboratoryIds(Set.of(7L, 9L));

        assertEquals(List.of(), port.findAll(scope));
        assertSame(scope, mapper.listScope);
    }

    @Test
    void returnsGeneratedIdFromMapperOwnedInsertRow() {
        RecordingMapper mapper = new RecordingMapper();
        mapper.generatedId = 42L;
        ReservationPersistencePort port = new MyBatisReservationPersistenceAdapter(mapper);

        assertEquals(42L, port.insert(newReservation()));
    }

    @Test
    void returnsFalseWhenConditionalReviewChangesNoRows() {
        RecordingMapper mapper = new RecordingMapper();
        mapper.reviewUpdateCount = 0;

        assertFalse(new MyBatisReservationPersistenceAdapter(mapper).updateReview(reviewUpdate()));
    }

    @Test
    void returnsTrueWhenConditionalReviewChangesOneRow() {
        RecordingMapper mapper = new RecordingMapper();
        mapper.reviewUpdateCount = 1;

        assertTrue(new MyBatisReservationPersistenceAdapter(mapper).updateReview(reviewUpdate()));
    }

    @Test
    void rejectsImpossibleConditionalReviewCount() {
        RecordingMapper mapper = new RecordingMapper();
        mapper.reviewUpdateCount = 2;

        assertThrows(IllegalStateException.class,
                () -> new MyBatisReservationPersistenceAdapter(mapper).updateReview(reviewUpdate()));
    }

    private NewReservationRecord newReservation() {
        return new NewReservationRecord(
                5L, "张三", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "09:00-11:00", "课程实验", 30,
                "课程使用", ReservationStatus.PENDING, false, "待审核", null
        );
    }

    private ReservationReviewUpdate reviewUpdate() {
        return new ReservationReviewUpdate(3L, ReservationStatus.APPROVED, "李老师", "通过",
                Set.of(ReservationStatus.PENDING, ReservationStatus.CONFLICT));
    }

    private static final class RecordingMapper implements ReservationPersistenceMapper {
        private ReservationRecordScope listScope;
        private Long generatedId;
        private int reviewUpdateCount;

        @Override
        public List<ReservationRecord> findAll(ReservationRecordScope scope) {
            listScope = scope;
            return null;
        }

        @Override
        public ReservationRecord findOne(long id, ReservationRecordScope scope) {
            return null;
        }

        @Override
        public int insert(ReservationInsertRow row) {
            row.setId(generatedId);
            return 1;
        }

        @Override
        public ReservationOverlapCount countActiveOverlaps(org.example.backend.reservation.application.port.out.ReservationOverlapCheck check) {
            return new ReservationOverlapCount(0);
        }

        @Override
        public int updateReview(ReservationReviewUpdate update) {
            return reviewUpdateCount;
        }
    }
}
