package org.example.backend.bridge.reservationschedule;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.kernel.capability.CapabilityStatus;
import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.application.ScheduleConflictSnapshot;
import org.example.backend.reservation.domain.TimeRange;
import org.example.backend.teachingschedule.api.ScheduleOccupancyQuery;
import org.example.backend.teachingschedule.api.ScheduleOccupancyRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class ReservationTeachingScheduleBridgeConfiguration {
    private static final ZoneId RESERVATION_TIME_ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    @ConditionalOnBean({ScheduleOccupancyQuery.class, LabCatalogQuery.class})
    public ScheduleConflictPort bridgedScheduleConflictPort(ScheduleOccupancyQuery schedules, LabCatalogQuery labs) {
        return (labId, date, range) -> check(schedules, labs, labId, date, range);
    }

    private CapabilityResult<ScheduleConflictSnapshot> check(ScheduleOccupancyQuery schedules, LabCatalogQuery labs,
                                                               Long labId, LocalDate date, TimeRange range) {
        if (labId == null || labId <= 0 || date == null || !valid(range)) return unavailable("reservation request is malformed");
        LocalDateTime start = LocalDateTime.ofInstant(range.startsAt(), RESERVATION_TIME_ZONE);
        LocalDateTime end = LocalDateTime.ofInstant(range.endsAt(), RESERVATION_TIME_ZONE);
        if (!start.toLocalDate().equals(date) || !end.toLocalDate().equals(date))
            return unavailable("reservation time must be within reservation date");

        LabView lab;
        try { lab = labs.findById(labId, LabAccessScope.all()); }
        catch (RuntimeException exception) { return unavailable("laboratory context is unavailable"); }
        if (lab == null || !labId.equals(lab.id())) return unavailable("laboratory context is unavailable");
        Set<String> aliases = aliases(lab);
        if (aliases.isEmpty()) return unavailable("laboratory identity is unavailable");

        try {
            var result = schedules.check(new ScheduleOccupancyRequest(date, start.toLocalTime(), end.toLocalTime(), aliases));
            if (result == null || result.status() != CapabilityStatus.AVAILABLE || result.value() == null)
                return unavailable(result == null || result.message() == null ? "teaching schedule is unavailable" : result.message());
            return CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, result.value().occupied()));
        } catch (RuntimeException exception) { return unavailable("teaching schedule is unavailable"); }
    }

    private Set<String> aliases(LabView lab) {
        Set<String> aliases = new LinkedHashSet<>();
        if (text(lab.labCode())) aliases.add(lab.labCode().trim());
        if (text(lab.labName())) aliases.add(lab.labName().trim());
        return Set.copyOf(aliases);
    }

    private boolean valid(TimeRange range) {
        return range != null && range.startsAt() != null && range.endsAt() != null && range.startsAt().isBefore(range.endsAt());
    }
    private boolean text(String value) { return value != null && !value.isBlank(); }
    private CapabilityResult<ScheduleConflictSnapshot> unavailable(String message) { return CapabilityResult.unavailable(message); }
}
