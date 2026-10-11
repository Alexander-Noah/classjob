package org.example.backend.service;

import org.example.backend.entity.ReservationStateMachine;
import org.example.backend.entity.ReservationStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

@Transactional(isolation = Isolation.SERIALIZABLE)
public class DefaultReservationCreationService {
    private final ReservationPersistenceService reservations;
    private final ReservationCatalogQuery catalog;
    private final ReservationLabService laboratories;
    private final ReservationAvailabilityEvaluator availability;

    public DefaultReservationCreationService(ReservationPersistenceService reservations, ReservationCatalogQuery catalog,
                                             ReservationLabService laboratories,
                                             ReservationAvailabilityEvaluator availability) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.laboratories = Objects.requireNonNull(laboratories, "laboratories");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public ReservationView create(CreateReservationCommand command, ReservationActor actor) {
        ValidatedCommand validated = validate(command);
        ReservationActor authenticatedActor = Objects.requireNonNull(actor, "actor");
        assertCanCreateForLaboratory(authenticatedActor, validated.labId());
        boolean conflict = availability.hasConflict(new ReservationAvailabilityRequest(validated.labId(), validated.reservationDate(),
                validated.timeRange().normalized(), validated.timeRange().timeRange(), validated.attendeeCount(), null));
        ReservationStatus status = ReservationStateMachine.initialStatus(conflict);
        long reservationId = reservations.insert(new NewReservationRecord(authenticatedActor.userId(), validated.applicantName(),
                validated.department(), validated.contact(), validated.labId(), validated.reservationDate(), validated.timeRange().normalized(),
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
        Set<Long> managed = laboratories.findManagedLaboratoryIds(actor.userId());
        if (managed == null) {
            throw new IllegalStateException("Managed laboratory capability did not return a set");
        }
        if (!managed.contains(labId)) {
            throw new IllegalArgumentException("Laboratory administrator cannot create for this laboratory");
        }
    }

    private ValidatedCommand validate(CreateReservationCommand command) {
        if (command == null || command.labId() == null || command.labId() <= 0 || command.attendeeCount() == null
                || command.attendeeCount() <= 0 || command.reservationDate() == null) {
            throw new IllegalArgumentException("Reservation command has invalid required fields");
        }
        ReservationTimeRangeParser.ParsedReservationTimeRange timeRange = ReservationTimeRangeParser.parse(command.timeRange(), command.reservationDate());
        return new ValidatedCommand(required(command.applicantName(), "applicant name"), required(command.department(), "department"),
                required(command.contact(), "contact"), command.labId(), command.reservationDate(), timeRange,
                required(command.scene(), "scene"), command.attendeeCount(), required(command.reason(), "reason"));
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Reservation " + field + " is required");
        }
        return value.trim();
    }

    private record ValidatedCommand(String applicantName, String department, String contact, Long labId, LocalDate reservationDate,
                                    ReservationTimeRangeParser.ParsedReservationTimeRange timeRange, String scene,
                                    Integer attendeeCount, String reason) { }
}
