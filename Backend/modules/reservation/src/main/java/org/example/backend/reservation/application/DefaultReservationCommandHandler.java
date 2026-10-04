package org.example.backend.reservation.application;

import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationView;

import java.util.Objects;

public class DefaultReservationCommandHandler implements ReservationCommandHandler {
    private final DefaultReservationCreationService creation;
    private final DefaultReservationReviewService review;

    public DefaultReservationCommandHandler(DefaultReservationCreationService creation, DefaultReservationReviewService review) {
        this.creation = Objects.requireNonNull(creation, "creation");
        this.review = Objects.requireNonNull(review, "review");
    }

    @Override
    public ReservationView create(CreateReservationCommand command, ReservationActor actor) {
        return creation.create(command, actor);
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
