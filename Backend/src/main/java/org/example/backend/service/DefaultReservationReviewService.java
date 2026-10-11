package org.example.backend.service;

import org.example.backend.entity.ReservationStateMachine;
import org.example.backend.entity.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class DefaultReservationReviewService {
    private static final Set<ReservationStatus> APPROVABLE = Set.of(ReservationStatus.PENDING);
    private static final Set<ReservationStatus> REJECTABLE = Set.of(ReservationStatus.PENDING, ReservationStatus.CONFLICT);

    private final ReservationPersistenceService reservations;
    private final ReservationCatalogQuery catalog;
    private final ReservationAvailabilityEvaluator availability;

    public DefaultReservationReviewService(ReservationPersistenceService reservations, ReservationCatalogQuery catalog,
                                           ReservationAvailabilityEvaluator availability) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public ReservationView approve(ReviewReservationCommand command, ReservationActor actor) {
        return review(command, actor, ReservationStatus.APPROVED, APPROVABLE, "通过");
    }

    public ReservationView reject(ReviewReservationCommand command, ReservationActor actor) {
        return review(command, actor, ReservationStatus.REJECTED, REJECTABLE, "驳回");
    }

    private ReservationView review(ReviewReservationCommand command, ReservationActor actor, ReservationStatus target,
                                   Set<ReservationStatus> allowedCurrentStatuses, String defaultRemark) {
        if (command == null || command.reservationId() == null || command.reservationId() <= 0) {
            throw new IllegalArgumentException("Review command requires a positive reservation id");
        }
        ReservationActor authenticated = Objects.requireNonNull(actor, "actor");
        String reviewerName = requiredReviewerName(authenticated);
        assertCanReview(authenticated);
        ReservationAccessScope scope = ReservationAccessScope.forActor(authenticated);
        ReservationView current = catalog.findById(command.reservationId(), scope);
        if (current == null) {
            throw new IllegalArgumentException("Reservation is not visible to the reviewer");
        }
        if (!allowedCurrentStatuses.contains(current.status())) {
            throw new IllegalStateException("Reservation is not in a reviewable status");
        }
        if (target == ReservationStatus.APPROVED) {
            assertCurrentlyAvailable(current);
        }
        transition(current.status(), target);
        String remark = normalizedRemark(command.reviewRemark(), defaultRemark);
        ReservationReviewUpdate update = new ReservationReviewUpdate(command.reservationId(), target, false, reviewerName,
                remark, allowedCurrentStatuses);
        if (!reservations.updateReview(update)) {
            throw new IllegalStateException("Reservation review failed because the state is stale or changed concurrently");
        }
        ReservationView reviewed = catalog.findById(command.reservationId(), scope);
        if (reviewed == null || reviewed.status() != target || reviewed.conflictFlag()
                || !reviewerName.equals(reviewed.reviewerName()) || !remark.equals(reviewed.reviewRemark())) {
            throw new IllegalStateException("Reservation review result could not be authoritatively verified");
        }
        return reviewed;
    }

    private void assertCurrentlyAvailable(ReservationView current) {
        ReservationTimeRangeParser.ParsedReservationTimeRange parsed;
        try {
            parsed = ReservationTimeRangeParser.parse(current.timeRange(), current.reservationDate());
            if (availability.hasConflict(new ReservationAvailabilityRequest(current.labId(), current.reservationDate(),
                    parsed.normalized(), parsed.timeRange(), current.attendeeCount(), current.id()))) {
                throw new IllegalStateException("Reservation can no longer be approved because it conflicts with current availability");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Reservation contains invalid availability data", exception);
        }
    }

    private String requiredReviewerName(ReservationActor actor) {
        if (actor.displayName() == null || actor.displayName().isBlank()) {
            throw new IllegalStateException("Authenticated reviewer display name is required");
        }
        return actor.displayName();
    }

    private void assertCanReview(ReservationActor actor) {
        if (actor.role() == ReservationActorRole.SYSTEM_ADMINISTRATOR
                || actor.role() == ReservationActorRole.LABORATORY_ADMINISTRATOR) {
            return;
        }
        if (actor.role() == ReservationActorRole.TEACHER) {
            throw new IllegalStateException("Teachers cannot review reservations");
        }
        throw new IllegalStateException("Unsupported reservation actor role: " + actor.role());
    }

    private void transition(ReservationStatus current, ReservationStatus target) {
        if (target == ReservationStatus.APPROVED) {
            ReservationStateMachine.approve(current);
        } else if (target == ReservationStatus.REJECTED) {
            ReservationStateMachine.reject(current);
        } else {
            throw new IllegalStateException("Unsupported review target: " + target);
        }
    }

    private String normalizedRemark(String remark, String defaultRemark) {
        return remark == null || remark.isBlank() ? defaultRemark : remark.trim();
    }
}
