package org.example.backend.teachingschedule.application;

import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.teachingschedule.api.ScheduleOccupancyQuery;
import org.example.backend.teachingschedule.api.ScheduleOccupancyRequest;
import org.example.backend.teachingschedule.api.ScheduleOccupancyView;
import org.example.backend.teachingschedule.application.port.out.ScheduleCalendarData;
import org.example.backend.teachingschedule.application.port.out.ScheduleOccupancyCriteria;
import org.example.backend.teachingschedule.application.port.out.ScheduleOccupancyRow;
import org.example.backend.teachingschedule.application.port.out.TeachingScheduleReadPort;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class DefaultScheduleOccupancyQuery implements ScheduleOccupancyQuery {
    private static final LocalTime[] SECTION_STARTS = {
            LocalTime.of(8, 30), LocalTime.of(8, 30), LocalTime.of(10, 20), LocalTime.of(10, 20),
            LocalTime.of(14, 0), LocalTime.of(14, 0), LocalTime.of(15, 50), LocalTime.of(15, 50),
            LocalTime.of(18, 40), LocalTime.of(18, 40), LocalTime.of(20, 20), LocalTime.of(20, 20)
    };
    private static final LocalTime[] SECTION_ENDS = {
            LocalTime.of(10, 0), LocalTime.of(10, 0), LocalTime.of(11, 50), LocalTime.of(11, 50),
            LocalTime.of(15, 30), LocalTime.of(15, 30), LocalTime.of(17, 20), LocalTime.of(17, 20),
            LocalTime.of(20, 10), LocalTime.of(20, 10), LocalTime.of(21, 50), LocalTime.of(21, 50)
    };

    private final TeachingScheduleReadPort readPort;

    public DefaultScheduleOccupancyQuery(TeachingScheduleReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public CapabilityResult<ScheduleOccupancyView> check(ScheduleOccupancyRequest request) {
        if (!valid(request)) {
            return unavailable("invalid schedule occupancy request");
        }
        try {
            ScheduleCalendarData calendar = readPort.findActiveCalendar();
            if (calendar == null || blank(calendar.semester()) || blank(calendar.semesterStartDate())) {
                return unavailable("active teaching calendar is unavailable");
            }
            LocalDate semesterStart = LocalDate.parse(calendar.semesterStartDate().trim());
            long elapsedDays = ChronoUnit.DAYS.between(semesterStart, request.reservationDate());
            if (elapsedDays < 0) {
                return unavailable("reservation date precedes active semester");
            }
            int academicWeek = Math.toIntExact(elapsedDays / 7 + 1);
            ScheduleOccupancyCriteria criteria = new ScheduleOccupancyCriteria(
                    calendar.semester().trim(), academicWeek, weekday(request.reservationDate().getDayOfWeek()));
            List<ScheduleOccupancyRow> rows = readPort.findOccupancyCandidates(criteria);
            if (rows == null) {
                return unavailable("teaching schedule candidates are unavailable");
            }
            Set<String> aliases = request.laboratoryAliases().stream()
                    .filter(alias -> !blank(alias)).map(String::trim).collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (aliases.isEmpty()) {
                return unavailable("laboratory aliases are required");
            }
            for (ScheduleOccupancyRow row : rows) {
                if (!valid(row)) {
                    return unavailable("invalid teaching schedule row");
                }
                if (matches(row.classroom(), aliases) && overlaps(request, row)) {
                    return CapabilityResult.available(new ScheduleOccupancyView(true));
                }
            }
            return CapabilityResult.available(new ScheduleOccupancyView(false));
        } catch (DateTimeParseException | ArithmeticException exception) {
            return unavailable("invalid active teaching calendar");
        } catch (RuntimeException exception) {
            return unavailable("teaching schedule occupancy check failed");
        }
    }

    private boolean valid(ScheduleOccupancyRequest request) {
        return request != null && request.reservationDate() != null && request.startsAt() != null
                && request.endsAt() != null && request.startsAt().isBefore(request.endsAt())
                && request.laboratoryAliases() != null && !request.laboratoryAliases().isEmpty();
    }

    private boolean valid(ScheduleOccupancyRow row) {
        return row != null && !blank(row.classroom()) && row.startSection() != null && row.endSection() != null
                && row.startSection() >= 1 && row.endSection() <= 12 && row.startSection() <= row.endSection();
    }

    private boolean matches(String classroom, Set<String> aliases) {
        String candidate = classroom.trim();
        for (String alias : aliases) {
            if (candidate.equalsIgnoreCase(alias)) {
                return true;
            }
            String expression = "(?i)(?<![a-z0-9])" + Pattern.quote(alias) + "(?:M)?(?=$|[^a-z0-9])";
            if (Pattern.compile(expression).matcher(candidate).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean overlaps(ScheduleOccupancyRequest request, ScheduleOccupancyRow row) {
        LocalTime classStart = SECTION_STARTS[row.startSection() - 1];
        LocalTime classEnd = SECTION_ENDS[row.endSection() - 1];
        return request.startsAt().isBefore(classEnd) && classStart.isBefore(request.endsAt());
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private CapabilityResult<ScheduleOccupancyView> unavailable(String message) {
        return CapabilityResult.unavailable(message);
    }
}
