package org.example.backend.reservation.api;

import java.util.Objects;

public record ReservationActor(Long userId, ReservationActorRole role) {
    public ReservationActor {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Authenticated user id must be positive");
        }
        Objects.requireNonNull(role, "Authenticated actor role is required");
    }

    public static ReservationActor systemAdministrator(long userId) {
        return new ReservationActor(userId, ReservationActorRole.SYSTEM_ADMINISTRATOR);
    }

    public static ReservationActor laboratoryAdministrator(long userId) {
        return new ReservationActor(userId, ReservationActorRole.LABORATORY_ADMINISTRATOR);
    }

    public static ReservationActor teacher(long userId) {
        return new ReservationActor(userId, ReservationActorRole.TEACHER);
    }
}
