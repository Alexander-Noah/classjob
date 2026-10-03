package org.example.backend.reservation.application;

import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationView;

public interface ReservationCommandHandler {
    ReservationView create(CreateReservationCommand command, ReservationActor actor);

    ReservationView approve(ReviewReservationCommand command, ReservationActor actor);

    ReservationView reject(ReviewReservationCommand command, ReservationActor actor);
}
