package org.example.backend.service;

import org.example.backend.service.TimeRange;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReservationTimeRangeParser {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Pattern TIME_RANGE = Pattern.compile("^(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2})$");

    private ReservationTimeRangeParser() { }

    public static ParsedReservationTimeRange parse(String value, LocalDate reservationDate) {
        if (reservationDate == null) {
            throw new IllegalArgumentException("Reservation command has invalid required fields");
        }
        Matcher matcher = TIME_RANGE.matcher(value == null ? "" : value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Reservation time range must be HH:mm-HH:mm");
        }
        LocalTime startsAt = localTime(matcher.group(1), matcher.group(2));
        LocalTime endsAt = localTime(matcher.group(3), matcher.group(4));
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Reservation time range must end after it starts on the same day");
        }
        return new ParsedReservationTimeRange(
                String.format("%02d:%02d-%02d:%02d", startsAt.getHour(), startsAt.getMinute(), endsAt.getHour(), endsAt.getMinute()),
                new TimeRange(reservationDate.atTime(startsAt).atZone(SHANGHAI).toInstant(),
                        reservationDate.atTime(endsAt).atZone(SHANGHAI).toInstant())
        );
    }

    private static LocalTime localTime(String hour, String minute) {
        try {
            return LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Reservation time range must contain valid times", exception);
        }
    }

    public record ParsedReservationTimeRange(String normalized, TimeRange timeRange) { }
}
