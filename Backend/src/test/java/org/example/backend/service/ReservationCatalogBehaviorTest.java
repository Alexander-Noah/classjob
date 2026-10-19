package org.example.backend.service;
import org.example.backend.entity.ReservationStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationCatalogBehaviorTest {
    final ReservationPersistenceService persistence=mock(ReservationPersistenceService.class);
    final ReservationLabService labs=mock(ReservationLabService.class);
    final ReservationCatalogQuery service=new DefaultReservationCatalogQuery(persistence,labs);
    ReservationRecord record(Long lab){return new ReservationRecord(1L,7L,"教师甲","学院","123",lab,LocalDate.of(2027,1,1),"08:30-10:00","教学",20,"实验",ReservationStatus.PENDING,false,"待审核",null,null,null);}
    ReservationAccessScope scope(ReservationActor actor){return ReservationAccessScope.forActor(actor);}
    @Test void actorRoleSelectsOnlyItsAuthoritativeScope(){when(persistence.findAll(any())).thenReturn(List.of());when(labs.findManagedLaboratoryIds(7L)).thenReturn(Set.of(1L,2L));assertEquals(List.of(),service.list(scope(ReservationActor.systemAdministrator(7))));verify(persistence).findAll(ReservationRecordScope.all());service.list(scope(ReservationActor.teacher(7)));verify(persistence).findAll(ReservationRecordScope.forApplicant(7));service.list(scope(ReservationActor.laboratoryAdministrator(7)));verify(persistence).findAll(ReservationRecordScope.forLaboratoryIds(Set.of(1L,2L)));verify(labs,never()).findLaboratorySummaries(anySet());}
    @Test void enrichesLaboratoryMetadataAndAllowsMissingOptionalSummary(){when(persistence.findAll(any())).thenReturn(List.of(record(1L),record(2L)));when(labs.findLaboratorySummaries(Set.of(1L,2L))).thenReturn(Map.of(1L,new LaboratorySummary("机房","A101")));List<ReservationView> result=service.list(scope(ReservationActor.teacher(7)));assertEquals("机房",result.get(0).labName());assertEquals("A101",result.get(0).labCode());assertNull(result.get(1).labName());assertEquals("教师甲",result.get(0).applicantName());assertEquals(ReservationStatus.PENDING,result.get(0).status());}
    @Test void missingDetailsRemainMissingAndValidDetailUsesTheSameEnrichment(){when(persistence.findOne(anyLong(),any())).thenReturn(Optional.empty());assertNull(service.findById(9,scope(ReservationActor.teacher(7))));when(persistence.findOne(eq(1L),any())).thenReturn(Optional.of(record(1L)));when(labs.findLaboratorySummaries(Set.of(1L))).thenReturn(Map.of(1L,new LaboratorySummary("机房","A101")));assertEquals("A101",service.findById(1,scope(ReservationActor.teacher(7))).labCode());}
    @Test void invalidRowsOrForeignSummaryMetadataCannotProduceAView(){for(List<ReservationRecord> invalid:List.of(Collections.<ReservationRecord>singletonList(null),List.of(record(null)),List.of(record(0L)))){when(persistence.findAll(any())).thenReturn(invalid);assertThrows(IllegalStateException.class,()->service.list(scope(ReservationActor.teacher(7))));}when(persistence.findAll(any())).thenReturn(List.of(record(1L)));when(labs.findLaboratorySummaries(anySet())).thenReturn(Map.of(99L,new LaboratorySummary("foreign","X")));assertThrows(IllegalStateException.class,()->service.list(scope(ReservationActor.teacher(7))));when(labs.findLaboratorySummaries(anySet())).thenReturn(null);assertThrows(NullPointerException.class,()->service.list(scope(ReservationActor.teacher(7))));}
    @Test void missingCapabilitiesFailInsteadOfBroadeningScope(){when(labs.findManagedLaboratoryIds(7L)).thenReturn(null);assertThrows(NullPointerException.class,()->service.list(scope(ReservationActor.laboratoryAdministrator(7))));when(persistence.findAll(any())).thenReturn(null);assertThrows(NullPointerException.class,()->service.list(scope(ReservationActor.teacher(7))));assertThrows(NullPointerException.class,()->service.list(null));}
}
