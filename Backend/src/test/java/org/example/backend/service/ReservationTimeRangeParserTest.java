package org.example.backend.service;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTimeRangeParserTest {
    @Test void normalizesValidTimesInShanghaiAndRejectsMalformedBoundaries() {
        LocalDate date=LocalDate.of(2026,10,20);
        var valid=ReservationTimeRangeParser.parse("08:30-10:00",date);
        assertEquals("08:30-10:00",valid.normalized());
        assertEquals(Instant.parse("2026-10-20T00:30:00Z"),valid.timeRange().startsAt());
        assertEquals(Instant.parse("2026-10-20T02:00:00Z"),valid.timeRange().endsAt());
        assertThrows(IllegalArgumentException.class,()->ReservationTimeRangeParser.parse("08:30-10:00",null));
        for(String value:new String[]{null,"","8:30-10:00"," 08:30-10:00","08:30-10:00 "})
            assertEquals("Reservation time range must be HH:mm-HH:mm",assertThrows(IllegalArgumentException.class,()->ReservationTimeRangeParser.parse(value,date)).getMessage());
        for(String value:new String[]{"08:30-08:30","23:00-01:00"})
            assertTrue(assertThrows(IllegalArgumentException.class,()->ReservationTimeRangeParser.parse(value,date)).getMessage().contains("end after"));
        for(String value:new String[]{"24:00-25:00","08:60-10:00","08:00-10:99"})
            assertTrue(assertThrows(IllegalArgumentException.class,()->ReservationTimeRangeParser.parse(value,date)).getMessage().contains("valid times"));
    }
}
