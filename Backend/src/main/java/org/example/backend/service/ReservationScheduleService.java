package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ReservationScheduleService {
    private static final ZoneId RESERVATION_TIME_ZONE = ZoneId.of("Asia/Shanghai");

    private final ScheduleOccupancyQuery schedules;
    private final LabCatalogQuery laboratories;

    public ReservationScheduleService(ScheduleOccupancyQuery schedules, LabCatalogQuery laboratories) {
        this.schedules = schedules;
        this.laboratories = laboratories;
    }

    public CapabilityResult<ScheduleConflictSnapshot> checkConflict(Long labId, LocalDate date, TimeRange range) {
        if (labId == null || labId <= 0 || date == null || !valid(range)) {
            return unavailable("reservation request is malformed");
        }
        LocalDateTime start = LocalDateTime.ofInstant(range.startsAt(), RESERVATION_TIME_ZONE);
        LocalDateTime end = LocalDateTime.ofInstant(range.endsAt(), RESERVATION_TIME_ZONE);
        if (!start.toLocalDate().equals(date) || !end.toLocalDate().equals(date)) {
            return unavailable("reservation time must be within reservation date");
        }
        LabView lab;
        try {
            lab = laboratories.findById(labId, LabAccessScope.all());
        } catch (RuntimeException exception) {
            return unavailable("laboratory context is unavailable");
        }
        if (lab == null || !labId.equals(lab.id())) {
            return unavailable("laboratory context is unavailable");
        }
        Set<String> aliases = aliases(lab);
        if (aliases.isEmpty()) {
            return unavailable("laboratory identity is unavailable");
        }
        try {
            var result = schedules.check(new ScheduleOccupancyRequest(date, start.toLocalTime(), end.toLocalTime(), aliases));
            if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null) {
                return unavailable(result == null || result.message() == null
                        ? "teaching schedule is unavailable" : result.message());
            }
            return CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, result.value().occupied()));
        } catch (RuntimeException exception) {
            return unavailable("teaching schedule is unavailable");
        }
    }

    private Set<String> aliases(LabView lab) {
        Set<String> aliases = new LinkedHashSet<>();
        if (text(lab.labCode())) {
            aliases.add(lab.labCode().trim());
        }
        if (text(lab.labName())) {
            aliases.add(lab.labName().trim());
        }
        return Set.copyOf(aliases);
    }

    private boolean valid(TimeRange range) {
        return range != null && range.startsAt() != null && range.endsAt() != null
                && range.startsAt().isBefore(range.endsAt());
    }

    private boolean text(String value) {
        return value != null && !value.isBlank();
    }

    private CapabilityResult<ScheduleConflictSnapshot> unavailable(String message) {
        return CapabilityResult.unavailable(message);
    }
}
