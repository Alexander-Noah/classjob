package org.example.backend.reservation.api;

import java.util.Objects;

public record ReservationAccessScope(ReservationActor actor) {
    public ReservationAccessScope {
        Objects.requireNonNull(actor, "Reservation access scope requires an actor");
    }

    public static ReservationAccessScope forActor(ReservationActor actor) {
        return new ReservationAccessScope(actor);
    }
}
