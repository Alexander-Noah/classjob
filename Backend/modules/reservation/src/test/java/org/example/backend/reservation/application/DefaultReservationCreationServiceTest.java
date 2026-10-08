package org.example.backend.reservation.application;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.reservation.api.ReservationAccessScope;
import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.application.port.out.ManagedLaboratoryIdsPort;
import org.example.backend.reservation.application.port.out.NewReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationOverlapCheck;
import org.example.backend.reservation.application.port.out.ReservationPersistencePort;
import org.example.backend.reservation.application.port.out.ReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationRecordScope;
import org.example.backend.reservation.application.port.out.ReservationReviewUpdate;
import org.example.backend.reservation.domain.ReservationStatus;
import org.example.backend.reservation.domain.TimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReservationCreationServiceTest {
    @Test
    void persistsAuthenticatedActorIdentityAndReturnsTheScopedAuthoritativeView() {
        RecordingReservations reservations = new RecordingReservations(0);
        ReservationView expected = view(41L, 73L, ReservationStatus.PENDING, false);
        RecordingCatalog catalog = new RecordingCatalog(expected);
        var service = service(reservations, catalog, managed(7L), availableLab(), clearSchedule());

        ReservationView actual = service.create(command(), ReservationActor.teacher(73L));

        assertEquals(expected, actual);
        assertEquals(73L, reservations.inserted.applicantUserId());
        assertEquals("09:00-11:00", reservations.inserted.timeRange());
        assertEquals(ReservationStatus.PENDING, reservations.inserted.status());
        assertFalse(reservations.inserted.conflictFlag());
        assertEquals("待审核", reservations.inserted.reviewerName());
        assertNull(reservations.inserted.reviewRemark());
        assertEquals(41L, catalog.id);
        assertEquals(ReservationAccessScope.forActor(ReservationActor.teacher(73L)), catalog.scope);
    }

    @Test
    void allowsSystemAdministratorToCreateForAnyLaboratory() {
        RecordingReservations reservations = new RecordingReservations(0);
        var service = service(reservations, new RecordingCatalog(view(41L, 99L, ReservationStatus.PENDING, false)), userId -> Set.of(), availableLab(), clearSchedule());

        service.create(command(), ReservationActor.systemAdministrator(99L));

        assertEquals(99L, reservations.inserted.applicantUserId());
    }

    @Test
    void laboratoryAdministratorCannotCreateOutsideManagedLaboratoriesOrWhenManagedSetIsMissing() {
        for (ManagedLaboratoryIdsPort managed : List.of(managed(8L), userId -> Set.<Long>of())) {
            RecordingReservations reservations = new RecordingReservations(0);
            var service = service(reservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), managed, availableLab(), clearSchedule());

            assertThrows(IllegalArgumentException.class, () -> service.create(command(), ReservationActor.laboratoryAdministrator(7L)));
            assertNull(reservations.inserted);
        }
        RecordingReservations reservations = new RecordingReservations(0);
        var service = service(reservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), userId -> null, availableLab(), clearSchedule());
        assertThrows(IllegalStateException.class, () -> service.create(command(), ReservationActor.laboratoryAdministrator(7L)));
        assertNull(reservations.inserted);
    }

    @Test
    void rejectsMalformedInputAndTimeRangesBeforeWrites() {
        List<CreateReservationCommand> invalid = List.of(
                new CreateReservationCommand(" ", "学院", "1", 7L, DATE, "09:00-11:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", " ", "1", 7L, DATE, "09:00-11:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", " ", 7L, DATE, "09:00-11:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 0L, DATE, "09:00-11:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 7L, null, "09:00-11:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 7L, DATE, "9:00-11:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 7L, DATE, "11:00-11:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 7L, DATE, "11:00-09:00", "课程", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 7L, DATE, "09:00-11:00", " ", 1, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 7L, DATE, "09:00-11:00", "课程", 0, "原因"),
                new CreateReservationCommand("王老师", "学院", "1", 7L, DATE, "09:00-11:00", "课程", 1, " ")
        );
        for (CreateReservationCommand invalidCommand : invalid) {
            RecordingReservations reservations = new RecordingReservations(0);
            var service = service(reservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), managed(7L), availableLab(), clearSchedule());

            assertThrows(IllegalArgumentException.class, () -> service.create(invalidCommand, ReservationActor.teacher(7L)));
            assertNull(reservations.inserted);
            assertNull(reservations.overlapCheck);
        }
    }

    @Test
    void convertsRequestedLocalDateAndTimeUsingAsiaShanghaiForAvailability() {
        RecordingReservations reservations = new RecordingReservations(0);
        RecordingLabAvailability labs = availableLab();
        var service = service(reservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), managed(7L), labs, clearSchedule());

        service.create(command(), ReservationActor.teacher(7L));

        assertEquals(new TimeRange(Instant.parse("2026-10-05T01:00:00Z"), Instant.parse("2026-10-05T03:00:00Z")), labs.timeRange);
    }

    @Test
    void failsClosedForUnavailableMissingOrMismatchedLaboratoryCapability() {
        List<LabAvailabilityPort> ports = List.of(
                (labId, attendees, range) -> CapabilityResult.unavailable("down"),
                (labId, attendees, range) -> CapabilityResult.available(null),
                (labId, attendees, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(9L, 30, true))
        );
        for (LabAvailabilityPort labs : ports) {
            RecordingReservations reservations = new RecordingReservations(0);
            var service = service(reservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), managed(7L), labs, clearSchedule());
            assertThrows(IllegalStateException.class, () -> service.create(command(), ReservationActor.teacher(7L)));
            assertNull(reservations.inserted);
        }
    }

    @Test
    void failsClosedForUnavailableMissingOrMismatchedScheduleCapability() {
        List<ScheduleConflictPort> ports = List.of(
                (labId, date, range) -> CapabilityResult.unavailable("down"),
                (labId, date, range) -> CapabilityResult.available(null),
                (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(9L, DATE, range, false))
        );
        for (ScheduleConflictPort schedules : ports) {
            RecordingReservations reservations = new RecordingReservations(0);
            var service = service(reservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), managed(7L), availableLab(), schedules);
            assertThrows(IllegalStateException.class, () -> service.create(command(), ReservationActor.teacher(7L)));
            assertNull(reservations.inserted);
        }
    }

    @Test
    void derivesConflictForIneligibleLaboratoryUnderCapacityScheduleConflictAndActiveOverlap() {
        List<CreationCase> cases = List.of(
                new CreationCase(new RecordingLabAvailability(new LabAvailabilitySnapshot(7L, 40, false)), clearSchedule(), 0),
                new CreationCase(new RecordingLabAvailability(new LabAvailabilitySnapshot(7L, 20, true)), clearSchedule(), 0),
                new CreationCase(availableLab(), conflictingSchedule(), 0),
                new CreationCase(availableLab(), clearSchedule(), 1)
        );
        for (CreationCase creationCase : cases) {
            RecordingReservations reservations = new RecordingReservations(creationCase.overlaps());
            var service = service(reservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.CONFLICT, true)), managed(7L), creationCase.labs(), creationCase.schedules());

            service.create(command(), ReservationActor.teacher(7L));

            assertEquals(ReservationStatus.CONFLICT, reservations.inserted.status());
            assertTrue(reservations.inserted.conflictFlag());
        }
    }

    @Test
    void derivesInitialPendingOrConflictStatusFromTheCentralAvailabilityEvaluator() {
        for (boolean conflict : List.of(false, true)) {
            RecordingReservations reservations = new RecordingReservations(0);
            CapturingAvailability availability = new CapturingAvailability(conflict);
            DefaultReservationCreationService service = service(reservations,
                    new RecordingCatalog(view(41L, 7L, conflict ? ReservationStatus.CONFLICT : ReservationStatus.PENDING, conflict)),
                    managed(7L), availability);

            service.create(command(), ReservationActor.teacher(7L));

            assertEquals(conflict, reservations.inserted.conflictFlag());
            assertEquals(conflict ? ReservationStatus.CONFLICT : ReservationStatus.PENDING, reservations.inserted.status());
            assertEquals(7L, availability.request.labId());
            assertNull(availability.request.excludingReservationId());
        }
    }

    @Test
    void usesExactlyTheSpecifiedTerminalExclusionsAndRejectsNegativeOverlapCounts() {
        RecordingReservations normalReservations = new RecordingReservations(0);
        var normal = service(normalReservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), managed(7L), availableLab(), clearSchedule());
        normal.create(command(), ReservationActor.teacher(7L));
        assertEquals(Set.of(ReservationStatus.REJECTED, ReservationStatus.CANCELLED, ReservationStatus.EXPIRED, ReservationStatus.COMPLETED), normalReservations.overlapCheck.terminalStatuses());

        RecordingReservations invalidReservations = new RecordingReservations(-1);
        var invalid = service(invalidReservations, new RecordingCatalog(view(41L, 7L, ReservationStatus.PENDING, false)), managed(7L), availableLab(), clearSchedule());
        assertThrows(IllegalStateException.class, () -> invalid.create(command(), ReservationActor.teacher(7L)));
        assertNull(invalidReservations.inserted);
    }

    @Test
    void failsExplicitlyWhenInsertedRecordCannotBeRereadWithinActorScope() {
        RecordingReservations reservations = new RecordingReservations(0);
        var service = service(reservations, new RecordingCatalog(null), managed(7L), availableLab(), clearSchedule());

        assertThrows(IllegalStateException.class, () -> service.create(command(), ReservationActor.teacher(7L)));
        assertEquals(41L, reservations.insertedId);
    }

    @Test
    void runsCreationUseCaseInsideSerializableTransaction() {
        Transactional transaction = DefaultReservationCreationService.class.getAnnotation(Transactional.class);

        assertEquals(Isolation.SERIALIZABLE, transaction.isolation());
    }

    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);

    private DefaultReservationCreationService service(RecordingReservations reservations, ReservationCatalogQuery catalog,
                                                      ManagedLaboratoryIdsPort managed, LabAvailabilityPort labs,
                                                      ScheduleConflictPort schedules) {
        return new DefaultReservationCreationService(reservations, catalog, managed,
                new DefaultReservationAvailabilityEvaluator(reservations, labs, schedules));
    }

    private DefaultReservationCreationService service(RecordingReservations reservations, ReservationCatalogQuery catalog,
                                                      ManagedLaboratoryIdsPort managed, ReservationAvailabilityEvaluator availability) {
        return new DefaultReservationCreationService(reservations, catalog, managed, availability);
    }

    private CreateReservationCommand command() {
        return new CreateReservationCommand("王老师", "计算机学院", "13800000000", 7L, DATE,
                "09:00-11:00", "课程实验", 30, "课程使用");
    }

    private ManagedLaboratoryIdsPort managed(long... ids) {
        Set<Long> managed = java.util.Arrays.stream(ids).boxed().collect(java.util.stream.Collectors.toSet());
        return userId -> managed;
    }

    private RecordingLabAvailability availableLab() {
        return new RecordingLabAvailability(new LabAvailabilitySnapshot(7L, 40, true));
    }

    private ScheduleConflictPort clearSchedule() {
        return (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, false));
    }

    private ScheduleConflictPort conflictingSchedule() {
        return (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, true));
    }

    private ReservationView view(long id, long applicant, ReservationStatus status, boolean conflict) {
        return new ReservationView(id, applicant, "王老师", "计算机学院", "13800000000", 7L, DATE, "09:00-11:00",
                "课程实验", 30, "课程使用", status, conflict, "待审核", null,
                LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 1, 9, 0), "实验室", "LAB-7");
    }

    private record CreationCase(RecordingLabAvailability labs, ScheduleConflictPort schedules, long overlaps) {
    }

    private static final class RecordingLabAvailability implements LabAvailabilityPort {
        private final LabAvailabilitySnapshot snapshot;
        private TimeRange timeRange;

        private RecordingLabAvailability(LabAvailabilitySnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public CapabilityResult<LabAvailabilitySnapshot> check(Long labId, int attendees, TimeRange requestedTimeRange) {
            timeRange = requestedTimeRange;
            return CapabilityResult.available(snapshot);
        }
    }

    private static final class CapturingAvailability implements ReservationAvailabilityEvaluator {
        private final boolean conflict;
        private ReservationAvailabilityRequest request;

        private CapturingAvailability(boolean conflict) { this.conflict = conflict; }
        @Override public boolean hasConflict(ReservationAvailabilityRequest value) { request = value; return conflict; }
    }

    private static final class RecordingCatalog implements ReservationCatalogQuery {
        private final ReservationView result;
        private long id;
        private ReservationAccessScope scope;

        private RecordingCatalog(ReservationView result) {
            this.result = result;
        }

        @Override
        public List<ReservationView> list(ReservationAccessScope scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReservationView findById(long requestedId, ReservationAccessScope requestedScope) {
            id = requestedId;
            scope = requestedScope;
            return result;
        }
    }

    private static final class RecordingReservations implements ReservationPersistencePort {
        private final long overlaps;
        private NewReservationRecord inserted;
        private ReservationOverlapCheck overlapCheck;
        private long insertedId = 41L;

        private RecordingReservations(long overlaps) {
            this.overlaps = overlaps;
        }

        @Override public List<ReservationRecord> findAll(ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
        @Override public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) { throw new UnsupportedOperationException(); }
        @Override public long insert(NewReservationRecord record) { inserted = record; return insertedId; }
        @Override public long countActiveOverlaps(ReservationOverlapCheck check) { overlapCheck = check; return overlaps; }
        @Override public boolean updateReview(ReservationReviewUpdate update) { throw new UnsupportedOperationException(); }
    }
}
