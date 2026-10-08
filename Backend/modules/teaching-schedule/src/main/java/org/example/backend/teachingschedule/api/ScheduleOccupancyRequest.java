package org.example.backend.teachingschedule.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record ScheduleOccupancyRequest(
        LocalDate reservationDate,
        LocalTime startsAt,
        LocalTime endsAt,
        Set<String> laboratoryAliases
) {
    public ScheduleOccupancyRequest {
        laboratoryAliases = laboratoryAliases == null ? Set.of() : Set.copyOf(laboratoryAliases);
    }
}
