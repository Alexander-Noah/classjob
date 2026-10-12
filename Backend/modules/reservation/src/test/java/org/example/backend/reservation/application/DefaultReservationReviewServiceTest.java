package org.example.backend.reservation.application;

import org.example.backend.reservation.api.ReservationAccessScope;
import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.reservation.application.ReservationOverlapCheck;
import org.example.backend.reservation.application.ReservationPersistencePort;
import org.example.backend.reservation.application.ReservationRecord;
import org.example.backend.reservation.application.ReservationRecordScope;
import org.example.backend.reservation.application.ReservationReviewUpdate;
import org.example.backend.reservation.application.NewReservationRecord;
import org.example.backend.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReservationReviewServiceTest {
    @Test
    void requiresTrustedNonblankReviewerDisplayNameBeforeAnyReadOrWrite() {
        RecordingReservations reservations = new RecordingReservations(true);
        RecordingCatalog catalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null));
        DefaultReservationReviewService service = service(reservations, catalog);

        assertThrows(IllegalStateException.class, () -> service.approve(command(null), ReservationActor.systemAdministrator(1L)));
        assertThrows(IllegalStateException.class, () -> service.reject(command(""), ReservationActor.systemAdministrator(1L, "  ")));

        assertNull(catalog.firstScope);
        assertNull(reservations.update);
    }

    @Test
    void teacherCanNeverReviewAndDoesNotTouchPersistence() {
        RecordingReservations reservations = new RecordingReservations(true);
        RecordingCatalog catalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null));
        DefaultReservationReviewService service = service(reservations, catalog);
        ReservationActor teacher = ReservationActor.teacher(7L, "王老师");

        assertThrows(IllegalStateException.class, () -> service.approve(command(null), teacher));
        assertThrows(IllegalStateException.class, () -> service.reject(command(null), teacher));

        assertNull(catalog.firstScope);
        assertNull(reservations.update);
    }

    @Test
    void systemAdministratorApprovesWithPendingOnlyConditionalUpdateAndDefaultRemark() {
        RecordingReservations reservations = new RecordingReservations(true);
        ReservationView reviewed = view(ReservationStatus.APPROVED, false, "审核员", "通过");
        RecordingCatalog catalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null), reviewed);
        DefaultReservationReviewService service = service(reservations, catalog);
        ReservationActor actor = ReservationActor.systemAdministrator(1L, " 审核员 ");

        ReservationView actual = service.approve(command(null), actor);

        assertEquals(reviewed, actual);
        assertEquals(new ReservationReviewUpdate(41L, ReservationStatus.APPROVED, false, "审核员", "通过", java.util.Set.of(ReservationStatus.PENDING)), reservations.update);
        assertEquals(ReservationAccessScope.forActor(actor), catalog.firstScope);
        assertEquals(catalog.firstScope, catalog.secondScope);
    }

    @Test
    void systemAdministratorRejectsPendingOrConflictUsingTrimmedRemark() {
        for (ReservationStatus status : List.of(ReservationStatus.PENDING, ReservationStatus.CONFLICT)) {
            RecordingReservations reservations = new RecordingReservations(true);
            ReservationView reviewed = view(ReservationStatus.REJECTED, false, "管理员", "不符合要求");
            RecordingCatalog catalog = new RecordingCatalog(view(status, status == ReservationStatus.CONFLICT, "待审核", null), reviewed);
            DefaultReservationReviewService service = service(reservations, catalog);

            assertEquals(reviewed, service.reject(command(" 不符合要求 "), ReservationActor.systemAdministrator(1L, "管理员")));
            assertEquals(java.util.Set.of(ReservationStatus.PENDING, ReservationStatus.CONFLICT), reservations.update.allowedCurrentStatuses());
            assertEquals("不符合要求", reservations.update.reviewRemark());
            assertFalse(reservations.update.conflictFlag());
        }
    }

    @Test
    void laboratoryAdministratorReliesOnScopedCatalogForManagedAndDeniedReservations() {
        ReservationActor actor = ReservationActor.laboratoryAdministrator(2L, "实验室管理员");
        RecordingReservations allowedReservations = new RecordingReservations(true);
        RecordingCatalog allowedCatalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null),
                view(ReservationStatus.APPROVED, false, "实验室管理员", "通过"));
        assertEquals(ReservationStatus.APPROVED, service(allowedReservations, allowedCatalog).approve(command(null), actor).status());
        assertEquals(ReservationAccessScope.forActor(actor), allowedCatalog.firstScope);

        RecordingReservations deniedReservations = new RecordingReservations(true);
        RecordingCatalog deniedCatalog = new RecordingCatalog((ReservationView) null);
        assertThrows(IllegalArgumentException.class, () -> service(deniedReservations, deniedCatalog).approve(command(null), actor));
        assertNull(deniedReservations.update);
    }

    @Test
    void unsupportedCurrentStatesFailBeforeConditionalUpdate() {
        for (ReservationStatus status : List.of(ReservationStatus.CONFLICT, ReservationStatus.APPROVED,
                ReservationStatus.REJECTED, ReservationStatus.EXPIRED, ReservationStatus.COUNTERSIGNING,
                ReservationStatus.COMPLETED, ReservationStatus.CANCELLED)) {
            RecordingReservations reservations = new RecordingReservations(true);
            RecordingCatalog catalog = new RecordingCatalog(view(status, status == ReservationStatus.CONFLICT, "待审核", null));
            DefaultReservationReviewService service = service(reservations, catalog);
            if (status == ReservationStatus.CONFLICT) {
                assertThrows(IllegalStateException.class, () -> service.approve(command(null), ReservationActor.systemAdministrator(1L, "管理员")));
            } else {
                assertThrows(IllegalStateException.class, () -> service.reject(command(null), ReservationActor.systemAdministrator(1L, "管理员")));
            }
            assertNull(reservations.update);
        }
    }

    @Test
    void failedConditionalUpdateReportsConcurrentOrStaleReviewWithoutReread() {
        RecordingReservations reservations = new RecordingReservations(false);
        RecordingCatalog catalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null));

        assertThrows(IllegalStateException.class, () -> service(reservations, catalog)
                .approve(command(null), ReservationActor.systemAdministrator(1L, "管理员")));

        assertEquals(1, catalog.calls);
    }

    @Test
    void missingOrMismatchedAuthoritativePostUpdateViewFailsExplicitly() {
        List<ReservationView> invalid = List.of(
                view(ReservationStatus.PENDING, false, "管理员", "通过"),
                view(ReservationStatus.APPROVED, true, "管理员", "通过"),
                view(ReservationStatus.APPROVED, false, "另一个管理员", "通过"),
                view(ReservationStatus.APPROVED, false, "管理员", "其他备注")
        );
        for (ReservationView postUpdate : invalid) {
            RecordingReservations reservations = new RecordingReservations(true);
            RecordingCatalog catalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null), postUpdate);
            assertThrows(IllegalStateException.class, () -> service(reservations, catalog)
                    .approve(command(null), ReservationActor.systemAdministrator(1L, "管理员")));
        }
        RecordingReservations reservations = new RecordingReservations(true);
        RecordingCatalog catalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null), null);
        assertThrows(IllegalStateException.class, () -> service(reservations, catalog)
                .approve(command(null), ReservationActor.systemAdministrator(1L, "管理员")));
    }

    @Test
    void approvalRefusesCurrentAvailabilityConflictsWithoutUpdatingAndExcludesTheCurrentReservation() {
        RecordingReservations reservations = new RecordingReservations(true);
        RecordingCatalog catalog = new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null));
        CapturingAvailability availability = new CapturingAvailability(true);

        assertThrows(IllegalStateException.class, () -> service(reservations, catalog, availability)
                .approve(command(null), ReservationActor.systemAdministrator(1L, "管理员")));

        assertEquals(1, availability.calls.get());
        assertEquals(41L, availability.request.excludingReservationId());
        assertNull(reservations.update);
    }

    @Test
    void approvalFailsClosedForEveryCurrentAvailabilityFailureWithoutUpdating() {
        List<AvailabilityCase> cases = List.of(
                new AvailabilityCase((lab, count, range) -> CapabilityResult.unavailable("down"), clearSchedule(), 0),
                new AvailabilityCase((lab, count, range) -> CapabilityResult.available(null), clearSchedule(), 0),
                new AvailabilityCase((lab, count, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(9L, 40, true)), clearSchedule(), 0),
                new AvailabilityCase((lab, count, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(lab, 40, false)), clearSchedule(), 0),
                new AvailabilityCase((lab, count, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(lab, 20, true)), clearSchedule(), 0),
                new AvailabilityCase(availableLab(), (lab, date, range) -> CapabilityResult.unavailable("down"), 0),
                new AvailabilityCase(availableLab(), (lab, date, range) -> CapabilityResult.available(null), 0),
                new AvailabilityCase(availableLab(), (lab, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(9L, date, range, false)), 0),
                new AvailabilityCase(availableLab(), (lab, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(lab, date.plusDays(1), range, false)), 0),
                new AvailabilityCase(availableLab(), (lab, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(lab, date, new org.example.backend.reservation.domain.TimeRange(range.startsAt(), range.endsAt().plusSeconds(1)), false)), 0),
                new AvailabilityCase(availableLab(), (lab, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(lab, date, range, true)), 0),
                new AvailabilityCase(availableLab(), clearSchedule(), 1),
                new AvailabilityCase(availableLab(), clearSchedule(), -1)
        );
        for (AvailabilityCase availabilityCase : cases) {
            RecordingReservations reservations = new RecordingReservations(true, availabilityCase.overlaps());
            ReservationAvailabilityEvaluator evaluator = new DefaultReservationAvailabilityEvaluator(reservations,
                    availabilityCase.labs(), availabilityCase.schedules());

            assertThrows(IllegalStateException.class, () -> service(reservations,
                    new RecordingCatalog(view(ReservationStatus.PENDING, false, "待审核", null)), evaluator)
                    .approve(command(null), ReservationActor.systemAdministrator(1L, "管理员")));
            assertNull(reservations.update);
        }
    }

    @Test
    void approvalFailsClosedForMalformedStoredTimeDataWithoutUpdating() {
        RecordingReservations reservations = new RecordingReservations(true);
        ReservationView malformed = new ReservationView(41L, 7L, "王老师", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "9:00-11:00", "课程实验", 30, "课程使用", ReservationStatus.PENDING, false,
                "待审核", null, LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 1, 9, 0), "实验室", "LAB-7");
        CapturingAvailability availability = new CapturingAvailability(false);

        assertThrows(IllegalStateException.class, () -> service(reservations, new RecordingCatalog(malformed), availability)
                .approve(command(null), ReservationActor.systemAdministrator(1L, "管理员")));

        assertEquals(0, availability.calls.get());
        assertNull(reservations.update);
    }

    @Test
    void rejectionDoesNotParseOrInvokeAvailability() {
        RecordingReservations reservations = new RecordingReservations(true);
        ReservationView malformed = new ReservationView(41L, 7L, "王老师", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "invalid", "课程实验", 30, "课程使用", ReservationStatus.PENDING, false,
                "待审核", null, LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 1, 9, 0), "实验室", "LAB-7");
        CapturingAvailability availability = new CapturingAvailability(false);

        assertEquals(ReservationStatus.REJECTED, service(reservations,
                new RecordingCatalog(malformed, view(ReservationStatus.REJECTED, false, "管理员", "驳回")), availability)
                .reject(command(null), ReservationActor.systemAdministrator(1L, "管理员")).status());

        assertEquals(0, availability.calls.get());
        assertTrue(reservations.update != null);
    }

    @Test
    void reviewUseCaseIsTransactionalForCglibProxying() {
        assertTrue(DefaultReservationReviewService.class.isAnnotationPresent(Transactional.class));
    }

    private DefaultReservationReviewService service(RecordingReservations reservations, RecordingCatalog catalog) {
        return service(reservations, catalog, request -> false);
    }

    private DefaultReservationReviewService service(RecordingReservations reservations, RecordingCatalog catalog,
                                                    ReservationAvailabilityEvaluator availability) {
        return new DefaultReservationReviewService(reservations, catalog, availability);
    }

    private ReviewReservationCommand command(String remark) {
        return new ReviewReservationCommand(41L, remark);
    }

    private ReservationView view(ReservationStatus status, boolean conflict, String reviewer, String remark) {
        return new ReservationView(41L, 7L, "王老师", "计算机学院", "13800000000", 7L,
                LocalDate.of(2026, 10, 5), "09:00-11:00", "课程实验", 30, "课程使用", status, conflict,
                reviewer, remark, LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 1, 9, 0), "实验室", "LAB-7");
    }

    private static LabAvailabilityPort availableLab() {
        return (labId, attendees, timeRange) -> CapabilityResult.available(new LabAvailabilitySnapshot(labId, 40, true));
    }

    private static ScheduleConflictPort clearSchedule() {
        return (labId, date, timeRange) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, timeRange, false));
    }

    private static final class RecordingCatalog implements ReservationCatalogQuery {
        private final ReservationView[] results;
        private int calls;
        private ReservationAccessScope firstScope;
        private ReservationAccessScope secondScope;

        private RecordingCatalog(ReservationView... results) { this.results = results; }
        @Override public List<ReservationView> list(ReservationAccessScope scope) { throw new UnsupportedOperationException(); }
        @Override public ReservationView findById(long id, ReservationAccessScope scope) {
            calls++;
            if (calls == 1) firstScope = scope; else secondScope = scope;
            return calls <= results.length ? results[calls - 1] : null;
        }
    }

    private static final class RecordingReservations implements ReservationPersistencePort {
        private final boolean updateResult;
        private final long overlaps;
        private ReservationReviewUpdate update;
        private RecordingReservations(boolean updateResult) { this(updateResult, 0); }
        private RecordingReservations(boolean updateResult, long overlaps) { this.updateResult = updateResult; this.overlaps = overlaps; }
        @Override public List<ReservationRecord> findAll(ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
        @Override public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
        @Override public long insert(NewReservationRecord record) { throw new UnsupportedOperationException(); }
        @Override public long countActiveOverlaps(ReservationOverlapCheck check) { return overlaps; }
        @Override public boolean updateReview(ReservationReviewUpdate value) { update = value; return updateResult; }
    }

    private record AvailabilityCase(LabAvailabilityPort labs, ScheduleConflictPort schedules, long overlaps) { }

    private static final class CapturingAvailability implements ReservationAvailabilityEvaluator {
        private final boolean conflict;
        private final AtomicInteger calls = new AtomicInteger();
        private ReservationAvailabilityRequest request;

        private CapturingAvailability(boolean conflict) { this.conflict = conflict; }
        @Override public boolean hasConflict(ReservationAvailabilityRequest value) {
            request = value;
            calls.incrementAndGet();
            return conflict;
        }
    }
}
