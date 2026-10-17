/*
 * 课表与时间占用实现模块。
 * 主要协作组件：TeachingScheduleReadService。
 */
package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public final class DefaultScheduleOccupancyQuery implements ScheduleOccupancyQuery {
    private final TeachingScheduleReadService readService;

    public DefaultScheduleOccupancyQuery(TeachingScheduleReadService readService) {
        this.readService = readService;
    }

    @Override
    public CapabilityResult<ScheduleOccupancyView> check(ScheduleOccupancyRequest request) {
        if (!valid(request)) {
            return unavailable("invalid schedule occupancy request");
        }
        try {
            ScheduleCalendarData calendar = readService.findActiveCalendar();
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
            List<ScheduleOccupancyRow> rows = readService.findOccupancyCandidates(criteria);
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
        LocalTime classStart = TeachingSectionTimes.start(row.startSection());
        LocalTime classEnd = TeachingSectionTimes.end(row.endSection());
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
