/*
 * 实验室预约实现模块。
 */
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

    /**
     * 将同一天内的文本区间转为规范文本和绝对时间，整个过程不执行 I/O。
     *
     * @param value 严格的 HH:mm-HH:mm，不接受前后空白或跨午夜表达
     * @param reservationDate 预约的上海本地日期，不能为 null
     * @return 补齐两位时分的文本及按 Asia/Shanghai 转换的 Instant 区间
     * @throws IllegalArgumentException 日期缺失、格式错误、时间越界或终点不晚于起点
     */
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
