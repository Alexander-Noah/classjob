package org.example.backend.reservation.application;

import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.api.ReservationAccessScope;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.application.ManagedLaboratoryIdsPort;
import org.example.backend.reservation.application.NewReservationRecord;
import org.example.backend.reservation.application.ReservationOverlapCheck;
import org.example.backend.reservation.application.ReservationPersistencePort;
import org.example.backend.reservation.application.ReservationRecord;
import org.example.backend.reservation.application.ReservationRecordScope;
import org.example.backend.reservation.application.ReservationReviewUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultReservationCommandHandlerTest {
    @Test
    void delegatesCreationApprovalAndRejectionToTheirDedicatedServices() {
        ReservationView created = view(1L);
        ReservationView approved = view(2L);
        ReservationView rejected = view(3L);
        RecordingCreation creation = new RecordingCreation(created);
        RecordingReview review = new RecordingReview(approved, rejected);
        DefaultReservationCommandHandler handler = new DefaultReservationCommandHandler(creation, review);
        ReservationActor actor = ReservationActor.systemAdministrator(1L, "管理员");
        CreateReservationCommand create = new CreateReservationCommand("王老师", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "09:00-11:00", "课程", 30, "使用");
        ReviewReservationCommand command = new ReviewReservationCommand(41L, "备注");

        assertEquals(created, handler.create(create, actor));
        assertEquals(approved, handler.approve(command, actor));
        assertEquals(rejected, handler.reject(command, actor));
        assertEquals(create, creation.command);
        assertEquals(actor, creation.actor);
        assertEquals(command, review.approveCommand);
        assertEquals(command, review.rejectCommand);
        assertEquals(actor, review.actor);
    }

    @Test
    void retriesCreationInANewTransactionAfterTransientSerializationFailure() {
        ReservationView created = view(4L);
        RetryableCreation creation = new RetryableCreation(created, 1);
        DefaultReservationCommandHandler handler = new DefaultReservationCommandHandler(
                creation, new RecordingReview(view(2L), view(3L)));
        ReservationActor actor = ReservationActor.systemAdministrator(1L, "管理员");
        CreateReservationCommand command = new CreateReservationCommand("王老师", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "09:00-11:00", "课程", 30, "使用");

        assertEquals(created, handler.create(command, actor));
        assertEquals(2, creation.attempts);
    }

    private ReservationView view(long id) {
        return new ReservationView(id, 7L, "王老师", "学院", "1", 7L, LocalDate.of(2026, 10, 5), "09:00-11:00",
                "课程", 30, "使用", org.example.backend.reservation.domain.ReservationStatus.PENDING, false, null, null,
                LocalDateTime.now(), LocalDateTime.now(), "实验室", "LAB-7");
    }

    private static final class RecordingCreation extends DefaultReservationCreationService {
        private final ReservationView result;
        private CreateReservationCommand command;
        private ReservationActor actor;
        private RecordingCreation(ReservationView result) {
            super(noOpReservations(), noOpCatalog(), userId -> Set.of(),
                    request -> { throw new UnsupportedOperationException(); });
            this.result = result;
        }
        @Override public ReservationView create(CreateReservationCommand value, ReservationActor authenticatedActor) { command = value; actor = authenticatedActor; return result; }
    }

    private static final class RecordingReview extends DefaultReservationReviewService {
        private final ReservationView approved;
        private final ReservationView rejected;
        private ReviewReservationCommand approveCommand;
        private ReviewReservationCommand rejectCommand;
        private ReservationActor actor;
        private RecordingReview(ReservationView approved, ReservationView rejected) {
            super(noOpReservations(), noOpCatalog(), request -> { throw new UnsupportedOperationException(); });
            this.approved = approved;
            this.rejected = rejected;
        }
        @Override public ReservationView approve(ReviewReservationCommand command, ReservationActor authenticatedActor) { approveCommand = command; actor = authenticatedActor; return approved; }
        @Override public ReservationView reject(ReviewReservationCommand command, ReservationActor authenticatedActor) { rejectCommand = command; actor = authenticatedActor; return rejected; }
    }

    private static final class RetryableCreation extends DefaultReservationCreationService {
        private final ReservationView result;
        private int failuresRemaining;
        private int attempts;

        private RetryableCreation(ReservationView result, int failuresRemaining) {
            super(noOpReservations(), noOpCatalog(), userId -> Set.of(), request -> { throw new UnsupportedOperationException(); });
            this.result = result;
            this.failuresRemaining = failuresRemaining;
        }

        @Override
        public ReservationView create(CreateReservationCommand command, ReservationActor actor) {
            attempts++;
            if (failuresRemaining-- > 0) {
                throw new CannotAcquireLockException("deadlock victim");
            }
            return result;
        }
    }

    private static ReservationCatalogQuery noOpCatalog() {
        return new ReservationCatalogQuery() {
            @Override public List<ReservationView> list(ReservationAccessScope scope) { throw new UnsupportedOperationException(); }
            @Override public ReservationView findById(long id, ReservationAccessScope scope) { throw new UnsupportedOperationException(); }
        };
    }

    private static ReservationPersistencePort noOpReservations() {
        return new ReservationPersistencePort() {
            @Override public List<ReservationRecord> findAll(ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
            @Override public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
            @Override public long insert(NewReservationRecord record) { throw new UnsupportedOperationException(); }
            @Override public long countActiveOverlaps(ReservationOverlapCheck check) { throw new UnsupportedOperationException(); }
            @Override public boolean updateReview(ReservationReviewUpdate update) { throw new UnsupportedOperationException(); }
        };
    }
}
