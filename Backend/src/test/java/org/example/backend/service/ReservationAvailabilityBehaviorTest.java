package org.example.backend.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationAvailabilityBehaviorTest {
    final ReservationPersistenceService persistence=mock(ReservationPersistenceService.class);
    final ReservationLabService labs=mock(ReservationLabService.class);
    final ReservationScheduleService schedules=mock(ReservationScheduleService.class);
    final ReservationAvailabilityEvaluator service=new DefaultReservationAvailabilityEvaluator(persistence,labs,schedules);
    final LocalDate date=LocalDate.of(2027,1,4);
    final TimeRange range=new TimeRange(Instant.parse("2027-01-04T00:30:00Z"),Instant.parse("2027-01-04T02:00:00Z"));
    final ReservationAvailabilityRequest request=new ReservationAvailabilityRequest(1L,date,"08:30-10:00",range,20,9L);
    void ready(){when(labs.checkAvailability(1L,20,range)).thenReturn(CapabilityResult.available(new LabAvailabilitySnapshot(1L,30,true)));when(schedules.checkConflict(1L,date,range)).thenReturn(CapabilityResult.available(new ScheduleConflictSnapshot(1L,date,range,false)));}
    @Test void freeCapacityAndNoOverlapAreRequiredAndSelfIsExcluded(){ready();assertFalse(service.hasConflict(request));verify(persistence).countActiveOverlaps(argThat(check->check.excludingReservationId().equals(9L)&&check.terminalStatuses().size()==4));when(persistence.countActiveOverlaps(any())).thenReturn(1L);assertTrue(service.hasConflict(request));}
    @Test void insufficientCapacityClosedLabOrCourseConflictAreEachBlocking(){ready();when(labs.checkAvailability(1L,20,range)).thenReturn(CapabilityResult.available(new LabAvailabilitySnapshot(1L,19,true)));assertTrue(service.hasConflict(request));when(labs.checkAvailability(1L,20,range)).thenReturn(CapabilityResult.available(new LabAvailabilitySnapshot(1L,30,false)));assertTrue(service.hasConflict(request));ready();when(schedules.checkConflict(1L,date,range)).thenReturn(CapabilityResult.available(new ScheduleConflictSnapshot(1L,date,range,true)));assertTrue(service.hasConflict(request));}
    @Test void unavailableOrMismatchedCapabilitiesNeverBecomeFreeSlots(){assertThrows(IllegalStateException.class,()->service.hasConflict(request));ready();when(labs.checkAvailability(1L,20,range)).thenReturn(CapabilityResult.available(new LabAvailabilitySnapshot(2L,30,true)));assertThrows(IllegalStateException.class,()->service.hasConflict(request));ready();when(schedules.checkConflict(1L,date,range)).thenReturn(CapabilityResult.unavailable("offline"));assertThrows(IllegalStateException.class,()->service.hasConflict(request));ready();when(schedules.checkConflict(1L,date,range)).thenReturn(CapabilityResult.available(new ScheduleConflictSnapshot(2L,date,range,false)));assertThrows(IllegalStateException.class,()->service.hasConflict(request));ready();when(persistence.countActiveOverlaps(any())).thenReturn(-1L);assertThrows(IllegalStateException.class,()->service.hasConflict(request));}
}
