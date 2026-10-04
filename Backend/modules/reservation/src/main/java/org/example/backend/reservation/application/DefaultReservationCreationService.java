package org.example.backend.reservation.application;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.reservation.api.ReservationAccessScope;
import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationActorRole;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.application.port.out.ManagedLaboratoryIdsPort;
import org.example.backend.reservation.application.port.out.NewReservationRecord;
import org.example.backend.reservation.application.port.out.ReservationOverlapCheck;
import org.example.backend.reservation.application.port.out.ReservationPersistencePort;
import org.example.backend.reservation.domain.ReservationStateMachine;
import org.example.backend.reservation.domain.ReservationStatus;
import org.example.backend.reservation.domain.TimeRange;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Transactional
public class DefaultReservationCreationService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Pattern TIME_RANGE = Pattern.compile("^(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2})$");
    private static final Set<ReservationStatus> TERMINAL_OVERLAP_EXCLUSIONS = EnumSet.of(
            ReservationStatus.REJECTED, ReservationStatus.CANCELLED,
            ReservationStatus.EXPIRED, ReservationStatus.COMPLETED
    );

    private final ReservationPersistencePort reservations;
    private final ReservationCatalogQuery catalog;
    private final ManagedLaboratoryIdsPort managedLaboratories;
    private final LabAvailabilityPort laboratoryAvailability;
    private final ScheduleConflictPort scheduleConflicts;

    public DefaultReservationCreationService(ReservationPersistencePort reservations, ReservationCatalogQuery catalog,
                                             ManagedLaboratoryIdsPort managedLaboratories, LabAvailabilityPort laboratoryAvailability,
                                             ScheduleConflictPort scheduleConflicts) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.managedLaboratories = Objects.requireNonNull(managedLaboratories, "managedLaboratories");
        this.laboratoryAvailability = Objects.requireNonNull(laboratoryAvailability, "laboratoryAvailability");
        this.scheduleConflicts = Objects.requireNonNull(scheduleConflicts, "scheduleConflicts");
    }

    public ReservationView create(CreateReservationCommand command, ReservationActor actor) {
        ValidatedCommand validated = validate(command);
        ReservationActor authenticatedActor = Objects.requireNonNull(actor, "actor");
        assertCanCreateForLaboratory(authenticatedActor, validated.labId());
        LabAvailabilitySnapshot laboratory = requireLaboratoryCapability(validated);
        ScheduleConflictSnapshot schedule = requireScheduleCapability(validated);
        long overlapCount = reservations.countActiveOverlaps(new ReservationOverlapCheck(validated.labId(), validated.reservationDate(),
                validated.timeRange(), TERMINAL_OVERLAP_EXCLUSIONS, null));
        if (overlapCount < 0) {
            throw new IllegalStateException("Reservation overlap count cannot be negative");
        }
        boolean conflict = !laboratory.available() || laboratory.capacity() < validated.attendeeCount()
                || schedule.hasConflict() || overlapCount > 0;
        ReservationStatus status = ReservationStateMachine.initialStatus(conflict);
        long reservationId = reservations.insert(new NewReservationRecord(authenticatedActor.userId(), validated.applicantName(),
                validated.department(), validated.contact(), validated.labId(), validated.reservationDate(), validated.timeRange(),
                validated.scene(), validated.attendeeCount(), validated.reason(), status, conflict, "待审核", null));
        ReservationView created = catalog.findById(reservationId, ReservationAccessScope.forActor(authenticatedActor));
        if (created == null) {
            throw new IllegalStateException("Inserted reservation cannot be read within actor scope");
        }
        return created;
    }

    private void assertCanCreateForLaboratory(ReservationActor actor, long labId) {
        if (actor.role() == ReservationActorRole.SYSTEM_ADMINISTRATOR || actor.role() == ReservationActorRole.TEACHER) {
            return;
        }
        if (actor.role() != ReservationActorRole.LABORATORY_ADMINISTRATOR) {
            throw new IllegalStateException("Unsupported reservation actor role: " + actor.role());
        }
        Set<Long> managed = managedLaboratories.findByManagerUserId(actor.userId());
        if (managed == null) {
            throw new IllegalStateException("Managed laboratory capability did not return a set");
        }
        if (!managed.contains(labId)) {
            throw new IllegalArgumentException("Laboratory administrator cannot create for this laboratory");
        }
    }

    private LabAvailabilitySnapshot requireLaboratoryCapability(ValidatedCommand command) {
        CapabilityResult<LabAvailabilitySnapshot> result = laboratoryAvailability.check(command.labId(), command.attendeeCount(), command.asTimeRange());
        if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
            throw new IllegalStateException("Laboratory availability capability is unavailable");
        }
        LabAvailabilitySnapshot snapshot = result.value();
        if (!command.labId().equals(snapshot.labId())) {
            throw new IllegalStateException("Laboratory availability capability returned a mismatched laboratory");
        }
        return snapshot;
    }

    private ScheduleConflictSnapshot requireScheduleCapability(ValidatedCommand command) {
        CapabilityResult<ScheduleConflictSnapshot> result = scheduleConflicts.check(command.labId(), command.reservationDate(), command.asTimeRange());
        if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
            throw new IllegalStateException("Schedule conflict capability is unavailable");
        }
        ScheduleConflictSnapshot snapshot = result.value();
        if (!command.labId().equals(snapshot.labId()) || !command.reservationDate().equals(snapshot.reservationDate())
                || !command.asTimeRange().equals(snapshot.timeRange())) {
            throw new IllegalStateException("Schedule conflict capability returned a mismatched request");
        }
        return snapshot;
    }

    private ValidatedCommand validate(CreateReservationCommand command) {
        if (command == null || command.labId() == null || command.labId() <= 0 || command.attendeeCount() == null
                || command.attendeeCount() <= 0 || command.reservationDate() == null) {
            throw new IllegalArgumentException("Reservation command has invalid required fields");
        }
        ParsedTimeRange timeRange = parseTimeRange(command.timeRange());
        return new ValidatedCommand(required(command.applicantName(), "applicant name"), required(command.department(), "department"),
                required(command.contact(), "contact"), command.labId(), command.reservationDate(), timeRange.normalized(),
                required(command.scene(), "scene"), command.attendeeCount(), required(command.reason(), "reason"), timeRange.startsAt(), timeRange.endsAt());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Reservation " + field + " is required");
        }
        return value.trim();
    }

    private ParsedTimeRange parseTimeRange(String value) {
        Matcher matcher = TIME_RANGE.matcher(value == null ? "" : value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Reservation time range must be HH:mm-HH:mm");
        }
        LocalTime startsAt = localTime(matcher.group(1), matcher.group(2));
        LocalTime endsAt = localTime(matcher.group(3), matcher.group(4));
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Reservation time range must end after it starts on the same day");
        }
        return new ParsedTimeRange(String.format("%02d:%02d-%02d:%02d", startsAt.getHour(), startsAt.getMinute(), endsAt.getHour(), endsAt.getMinute()), startsAt, endsAt);
    }

    private LocalTime localTime(String hour, String minute) {
        try {
            return LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Reservation time range must contain valid times", exception);
        }
    }

    private record ParsedTimeRange(String normalized, LocalTime startsAt, LocalTime endsAt) { }

    private record ValidatedCommand(String applicantName, String department, String contact, Long labId, LocalDate reservationDate,
                                    String timeRange, String scene, Integer attendeeCount, String reason, LocalTime startsAt, LocalTime endsAt) {
        TimeRange asTimeRange() {
            return new TimeRange(reservationDate.atTime(startsAt).atZone(SHANGHAI).toInstant(), reservationDate.atTime(endsAt).atZone(SHANGHAI).toInstant());
        }
    }
}
