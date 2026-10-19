package org.example.backend.service;
import org.example.backend.entity.ReservationStatus;
import org.example.backend.mapper.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationPersistenceBehaviorTest {
    final ReservationPersistenceMapper mapper=mock(ReservationPersistenceMapper.class);
    final ReservationPersistenceService service=new ReservationPersistenceService(mapper);
    NewReservationRecord record(){return new NewReservationRecord(7L,"教师甲","学院","13812345678",1L,LocalDate.of(2027,1,4),"08:30-10:00","教学",20,"实验",ReservationStatus.CONFLICT,true,"待审核","说明");}
    @Test void insertRequiresDatabaseGeneratedIdAndItsRowExposesAllCanonicalBindings(){when(mapper.insert(any())).thenAnswer(call->{ReservationInsertRow row=call.getArgument(0);row.setId(9L);return 1;});assertEquals(9L,service.insert(record()));var captor=org.mockito.ArgumentCaptor.forClass(ReservationInsertRow.class);verify(mapper).insert(captor.capture());var json=new ObjectMapper().valueToTree(captor.getValue());assertEquals(9,json.get("id").asInt());assertEquals(7,json.get("applicantUserId").asInt());assertEquals("教师甲",json.get("applicantName").asText());assertEquals("学院",json.get("department").asText());assertEquals("13812345678",json.get("contact").asText());assertEquals(1,json.get("labId").asInt());assertEquals("2027-01-04",json.get("reservationDate").asText());assertEquals("08:30-10:00",json.get("timeRange").asText());assertEquals("教学",json.get("scene").asText());assertEquals(20,json.get("attendeeCount").asInt());assertEquals("实验",json.get("reason").asText());assertEquals("冲突",json.get("status").asText());assertTrue(json.get("conflictFlag").asBoolean());assertEquals("待审核",json.get("reviewerName").asText());assertEquals("说明",json.get("reviewRemark").asText());}
    @Test void missingGeneratedKeysOrOverlapCountsAreExplicitFailures(){assertThrows(IllegalStateException.class,()->service.insert(record()));ReservationOverlapCheck check=new ReservationOverlapCheck(1L,LocalDate.of(2027,1,4),"08:30-10:00",Set.of(ReservationStatus.CANCELLED),9L);assertThrows(IllegalStateException.class,()->service.countActiveOverlaps(check));when(mapper.countActiveOverlaps(check)).thenReturn(new ReservationOverlapCount(2));assertEquals(2,service.countActiveOverlaps(check));assertThrows(NullPointerException.class,()->service.insert(null));}
    @Test void scopedReadsNormalizeNullAndConditionalUpdatesRequireZeroOrOneRow(){assertEquals(List.of(),service.findAll(ReservationRecordScope.all()));assertEquals(Optional.empty(),service.findOne(9,ReservationRecordScope.forApplicant(7)));ReservationReviewUpdate update=new ReservationReviewUpdate(9,ReservationStatus.APPROVED,false,"审批人","通过",Set.of(ReservationStatus.PENDING));assertFalse(service.updateReview(update));when(mapper.updateReview(update)).thenReturn(1);assertTrue(service.updateReview(update));when(mapper.updateReview(update)).thenReturn(2);assertThrows(IllegalStateException.class,()->service.updateReview(update));assertThrows(NullPointerException.class,()->service.findAll(null));}
}
