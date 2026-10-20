package org.example.backend.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleOccupancyBehaviorTest {
    @Test void incompleteRequestsAndCalendarNeverReachCandidateLookup() {
        for(ScheduleOccupancyRequest invalid:List.of(
                new ScheduleOccupancyRequest(null,LocalTime.MIN,LocalTime.NOON,Set.of("A101")),
                request(null,LocalTime.NOON,Set.of("A101")),request(LocalTime.MIN,null,Set.of("A101")),
                request(LocalTime.MIN,LocalTime.NOON,null)))
            assertEquals(CapabilityStatus.UNAVAILABLE,service.check(invalid).status());
        for(ScheduleCalendarData invalid:List.of(new ScheduleCalendarData(null,"2026-09-07"),new ScheduleCalendarData(" ","2026-09-07"),new ScheduleCalendarData("2026秋",null),new ScheduleCalendarData("2026秋"," "))){
            when(read.findActiveCalendar()).thenReturn(invalid);assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());
        }
        verify(read,never()).findOccupancyCandidates(any());
    }
    @Test void missingSectionsRemainUnavailableAndEarlierNonOverlappingTimeIsFree() {
        calendar();
        for(ScheduleOccupancyRow row:List.of(new ScheduleOccupancyRow(null,1,2),new ScheduleOccupancyRow("A101",null,2),new ScheduleOccupancyRow("A101",1,null))){
            when(read.findOccupancyCandidates(any())).thenReturn(List.of(row));assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());
        }
        when(read.findOccupancyCandidates(any())).thenReturn(List.of(new ScheduleOccupancyRow("A101",1,2)));
        assertFalse(service.check(request(LocalTime.of(6,0),TeachingSectionTimes.start(1),Set.of("A101"))).value().occupied());
    }
    final TeachingScheduleReadService read=mock(TeachingScheduleReadService.class);
    final ScheduleOccupancyQuery service=new DefaultScheduleOccupancyQuery(read);
    ScheduleOccupancyRequest request(LocalTime start,LocalTime end,Set<String> aliases){return new ScheduleOccupancyRequest(LocalDate.of(2026,9,7),start,end,aliases);}
    ScheduleOccupancyRequest request(){return request(LocalTime.of(8,30),LocalTime.of(10,0),Set.of("A101"));}
    void calendar(){when(read.findActiveCalendar()).thenReturn(new ScheduleCalendarData("2026秋","2026-09-07"));}
    @Test void matchesLaboratoryTokensAndOverlappingTeachingPeriods(){calendar();when(read.findOccupancyCandidates(any())).thenReturn(List.of(new ScheduleOccupancyRow("教学楼 A101M",1,2)));var result=service.check(request());assertEquals(CapabilityStatus.AVAILABLE,result.status());assertTrue(result.value().occupied());verify(read).findOccupancyCandidates(new ScheduleOccupancyCriteria("2026秋",1,"星期一"));when(read.findOccupancyCandidates(any())).thenReturn(List.of(new ScheduleOccupancyRow("A1010",1,2)));assertFalse(service.check(request()).value().occupied());}
    @Test void touchingIntervalsDoNotOverlapAndDayOfWeekIsDerivedForEveryDay(){calendar();when(read.findOccupancyCandidates(any())).thenReturn(List.of(new ScheduleOccupancyRow("a101",1,2)));var late=request(TeachingSectionTimes.end(2),LocalTime.of(12,0),Set.of("A101"));assertFalse(service.check(late).value().occupied());for(int offset=0;offset<7;offset++){var date=LocalDate.of(2026,9,7).plusDays(offset);assertEquals(CapabilityStatus.AVAILABLE,service.check(new ScheduleOccupancyRequest(date,LocalTime.of(8,30),LocalTime.of(10,0),Set.of("A101"))).status());}}
    @Test void invalidRequestsAndMissingCalendarAreUnavailableRatherThanFree(){assertEquals(CapabilityStatus.UNAVAILABLE,service.check(null).status());assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request(LocalTime.NOON,LocalTime.MIN,Set.of("A101"))).status());assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request(LocalTime.MIN,LocalTime.NOON,Set.of())).status());assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());when(read.findActiveCalendar()).thenReturn(new ScheduleCalendarData("2026秋","bad"));assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());when(read.findActiveCalendar()).thenReturn(new ScheduleCalendarData("2026秋","2026-10-01"));assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());}
    @Test void corruptRowsBlankAliasesAndDependencyFailuresCannotReportAvailability(){calendar();when(read.findOccupancyCandidates(any())).thenReturn(null);assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());for(ScheduleOccupancyRow row:Arrays.asList(null,new ScheduleOccupancyRow("",1,2),new ScheduleOccupancyRow("A101",0,2),new ScheduleOccupancyRow("A101",3,2),new ScheduleOccupancyRow("A101",1,13))){when(read.findOccupancyCandidates(any())).thenReturn(Collections.singletonList(row));assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());}when(read.findOccupancyCandidates(any())).thenReturn(List.of());assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request(LocalTime.MIN,LocalTime.NOON,Set.of(" "))).status());when(read.findActiveCalendar()).thenThrow(new IllegalStateException("offline"));assertEquals(CapabilityStatus.UNAVAILABLE,service.check(request()).status());}
}
