package org.example.backend.reservation.application;

import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationView;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.Objects;

public class DefaultReservationCommandHandler implements ReservationCommandHandler {
    private static final int MAX_CREATION_ATTEMPTS = 3;
    private final DefaultReservationCreationService creation;
    private final DefaultReservationReviewService review;

    public DefaultReservationCommandHandler(DefaultReservationCreationService creation, DefaultReservationReviewService review) {
        this.creation = Objects.requireNonNull(creation, "creation");
        this.review = Objects.requireNonNull(review, "review");
    }

    @Override
    public ReservationView create(CreateReservationCommand command, ReservationActor actor) {
        for (int attempt = 1; attempt <= MAX_CREATION_ATTEMPTS; attempt++) {
            try {
                return creation.create(command, actor);
            } catch (PessimisticLockingFailureException exception) {
                if (attempt == MAX_CREATION_ATTEMPTS) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("reservation creation retry loop exhausted");
    }

    @Override
    public ReservationView approve(ReviewReservationCommand command, ReservationActor actor) {
        return review.approve(command, actor);
    }

    @Override
    public ReservationView reject(ReviewReservationCommand command, ReservationActor actor) {
        return review.reject(command, actor);
    }
}
