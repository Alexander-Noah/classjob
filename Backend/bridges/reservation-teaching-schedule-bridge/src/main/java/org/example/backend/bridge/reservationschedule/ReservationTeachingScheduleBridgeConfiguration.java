package org.example.backend.bridge.reservationschedule;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.labresource.api.LabAccessScope;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.api.LabView;
import org.example.backend.reservation.application.ScheduleConflictPort;
import org.example.backend.reservation.application.ScheduleConflictSnapshot;
import org.example.backend.reservation.domain.TimeRange;
import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class ReservationTeachingScheduleBridgeConfiguration {
    private static final String DEFAULT_SEMESTER_START_DATE = "2026-03-02";
    private static final int MAX_SEARCH_LIMIT = 5000;
    private static final Map<Integer, LocalTime> SECTION_STARTS = Map.of(
            1, LocalTime.of(8, 30),
            3, LocalTime.of(10, 20),
            5, LocalTime.of(14, 0),
            7, LocalTime.of(15, 50),
            9, LocalTime.of(18, 40),
            11, LocalTime.of(20, 20)
    );
    private static final Map<Integer, LocalTime> SECTION_ENDS = Map.of(
            2, LocalTime.of(10, 0),
            4, LocalTime.of(11, 50),
            6, LocalTime.of(15, 30),
            8, LocalTime.of(17, 20),
            10, LocalTime.of(20, 10),
            12, LocalTime.of(21, 50)
    );

    @Bean
    @ConditionalOnBean({TeachingScheduleQuery.class, LabCatalogQuery.class})
    public ScheduleConflictPort bridgedScheduleConflictPort(
            TeachingScheduleQuery schedules,
            LabCatalogQuery labs,
            @Value("${reservation.schedule.semester-start-date:" + DEFAULT_SEMESTER_START_DATE + "}") String configuredSemesterStartDate
    ) {
        return (labId, reservationDate, timeRange) -> check(
                schedules, labs, configuredSemesterStartDate, labId, reservationDate, timeRange
        );
    }

    private CapabilityResult<ScheduleConflictSnapshot> check(
            TeachingScheduleQuery schedules,
            LabCatalogQuery labs,
            String configuredSemesterStartDate,
            Long labId,
            LocalDate reservationDate,
            TimeRange timeRange
    ) {
        if (labId == null || labId <= 0 || reservationDate == null || !isValid(timeRange)) {
            return unavailable("reservation request is malformed");
        }

        LocalDate semesterStart = semesterStart(configuredSemesterStartDate);
        if (semesterStart == null || reservationDate.isBefore(semesterStart)) {
            return unavailable("semester start date is unavailable for the reservation date");
        }

        LabView lab;
        try {
            lab = labs.findById(labId, LabAccessScope.all());
        } catch (RuntimeException exception) {
            return unavailable("laboratory context is unavailable");
        }
        if (lab == null || !labId.equals(lab.id()) || !hasLaboratoryIdentity(lab)) {
            return unavailable("laboratory context is unavailable");
        }

        int academicWeek;
        try {
            academicWeek = Math.toIntExact(ChronoUnit.DAYS.between(semesterStart, reservationDate) / 7 + 1);
        } catch (ArithmeticException exception) {
            return unavailable("academic week is unavailable");
        }

        List<ScheduleEntryView> entries;
        try {
            entries = schedules.search(new ScheduleSearchCriteria(null, null, null, null, null, null, academicWeek, MAX_SEARCH_LIMIT));
        } catch (RuntimeException exception) {
            return unavailable("teaching schedule is unavailable");
        }
        if (entries == null) {
            return unavailable("teaching schedule is unavailable");
        }

        String weekday = weekday(reservationDate.getDayOfWeek());
        for (ScheduleEntryView entry : entries) {
            if (!isValid(entry)) {
                return unavailable("teaching schedule contains malformed timetable data");
            }
            if (!weekday.equals(entry.weekday()) || !matchesLaboratory(entry.classroom(), lab)) {
                continue;
            }
            if (overlaps(reservationDate, timeRange, entry)) {
                return CapabilityResult.available(new ScheduleConflictSnapshot(labId, reservationDate, timeRange, true));
            }
        }
        return CapabilityResult.available(new ScheduleConflictSnapshot(labId, reservationDate, timeRange, false));
    }

    private LocalDate semesterStart(String configuredSemesterStartDate) {
        if (configuredSemesterStartDate == null || configuredSemesterStartDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(configuredSemesterStartDate.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isValid(TimeRange timeRange) {
        return timeRange != null && timeRange.startsAt() != null && timeRange.endsAt() != null
                && timeRange.startsAt().isBefore(timeRange.endsAt());
    }

    private boolean hasLaboratoryIdentity(LabView lab) {
        return hasText(lab.labCode()) || hasText(lab.labName());
    }

    private boolean isValid(ScheduleEntryView entry) {
        return entry != null
                && hasText(entry.weekday())
                && hasText(entry.classroom())
                && entry.startSection() != null
                && entry.endSection() != null
                && entry.startSection() >= 1
                && entry.endSection() <= 12
                && entry.startSection() <= entry.endSection();
    }

    private boolean matchesLaboratory(String classroom, LabView lab) {
        return matchesExactOrToken(classroom, lab.labCode()) || matchesExactly(classroom, lab.labName());
    }

    private boolean matchesExactOrToken(String classroom, String code) {
        if (!hasText(code)) {
            return false;
        }
        String normalizedClassroom = classroom.trim();
        String normalizedCode = code.trim();
        if (normalizedClassroom.equalsIgnoreCase(normalizedCode)) {
            return true;
        }
        String pattern = "(?i)(?<![a-z0-9])" + java.util.regex.Pattern.quote(normalizedCode) + "(?![a-z0-9])";
        return java.util.regex.Pattern.compile(pattern).matcher(normalizedClassroom).find();
    }

    private boolean matchesExactly(String classroom, String name) {
        return hasText(name) && classroom.trim().equals(name.trim());
    }

    private boolean overlaps(LocalDate reservationDate, TimeRange request, ScheduleEntryView entry) {
        LocalDateTime classStart = LocalDateTime.of(reservationDate, SECTION_STARTS.get(sectionPairStart(entry.startSection())));
        LocalDateTime classEnd = LocalDateTime.of(reservationDate, SECTION_ENDS.get(sectionPairEnd(entry.endSection())));
        LocalDateTime requestStart = LocalDateTime.ofInstant(request.startsAt(), ZoneOffset.UTC);
        LocalDateTime requestEnd = LocalDateTime.ofInstant(request.endsAt(), ZoneOffset.UTC);
        return requestStart.isBefore(classEnd) && classStart.isBefore(requestEnd);
    }

    private int sectionPairStart(int section) {
        return section % 2 == 0 ? section - 1 : section;
    }

    private int sectionPairEnd(int section) {
        return section % 2 == 0 ? section : section + 1;
    }

    private String weekday(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private CapabilityResult<ScheduleConflictSnapshot> unavailable(String message) {
        return CapabilityResult.unavailable(message);
    }
}
