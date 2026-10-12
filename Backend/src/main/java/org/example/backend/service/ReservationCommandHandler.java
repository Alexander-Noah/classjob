package org.example.backend.service;

import org.example.backend.service.ReservationActor;
import org.example.backend.service.ReservationView;

public interface ReservationCommandHandler {
    ReservationView create(CreateReservationCommand command, ReservationActor actor);

    ReservationView approve(ReviewReservationCommand command, ReservationActor actor);

    ReservationView reject(ReviewReservationCommand command, ReservationActor actor);
}
