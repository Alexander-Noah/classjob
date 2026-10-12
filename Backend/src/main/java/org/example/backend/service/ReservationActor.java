package org.example.backend.service;

import java.util.Objects;

public record ReservationActor(Long userId, ReservationActorRole role, String displayName) {
    public ReservationActor(Long userId, ReservationActorRole role) {
        this(userId, role, null);
    }

    public ReservationActor {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Authenticated user id must be positive");
        }
        Objects.requireNonNull(role, "Authenticated actor role is required");
        displayName = displayName == null ? null : displayName.trim();
    }

    public static ReservationActor systemAdministrator(long userId) {
        return systemAdministrator(userId, null);
    }

    public static ReservationActor systemAdministrator(long userId, String displayName) {
        return new ReservationActor(userId, ReservationActorRole.SYSTEM_ADMINISTRATOR, displayName);
    }

    public static ReservationActor laboratoryAdministrator(long userId) {
        return laboratoryAdministrator(userId, null);
    }

    public static ReservationActor laboratoryAdministrator(long userId, String displayName) {
        return new ReservationActor(userId, ReservationActorRole.LABORATORY_ADMINISTRATOR, displayName);
    }

    public static ReservationActor teacher(long userId) {
        return teacher(userId, null);
    }

    public static ReservationActor teacher(long userId, String displayName) {
        return new ReservationActor(userId, ReservationActorRole.TEACHER, displayName);
    }
}
