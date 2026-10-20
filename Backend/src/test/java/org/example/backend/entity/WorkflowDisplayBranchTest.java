package org.example.backend.entity;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowDisplayBranchTest {
    @Test void transitionMatrixAllowsOnlyDocumentedApprovalRejectionAndExpiryEdges() {
        Set<String> allowed=Set.of("PENDING/APPROVED","PENDING/REJECTED","PENDING/EXPIRED","CONFLICT/REJECTED");
        for(ReservationStatus current:ReservationStatus.values())for(ReservationStatus target:ReservationStatus.values()) {
            if(allowed.contains(current.name()+"/"+target.name()))assertEquals(target,ReservationStateMachine.transition(current,target));
            else assertThrows(IllegalStateException.class,()->ReservationStateMachine.transition(current,target));
        }
        assertThrows(IllegalArgumentException.class,()->ReservationStateMachine.transition(null,ReservationStatus.PENDING));
        assertThrows(IllegalArgumentException.class,()->ReservationStateMachine.transition(ReservationStatus.PENDING,null));
        assertEquals(ReservationStatus.EXPIRED,ReservationStateMachine.expire(ReservationStatus.PENDING));
        assertEquals(ReservationStatus.CONFLICT,ReservationStateMachine.initialStatus(true));
        assertEquals(ReservationStatus.PENDING,ReservationStateMachine.initialStatus(false));
    }
    @Test void reservationLabelsHandleMissingRoomCodeAndReviewMetadataWithoutInventingApproval() {
        var record=new ReservationsEntity();assertEquals("",record.getLab());record.setLabId(7L);assertEquals("7",record.getLab());record.setLabName(" ");assertEquals("7",record.getLab());
        record.setLabName("Lab");for(String code:Arrays.asList(null,""," ")){record.setLabCode(code);assertEquals("Lab",record.getLab());}
        record.setLabCode("L1");assertEquals("L1 Lab",record.getLab());record.setLabName("L1 Lab");assertEquals("L1 Lab",record.getLab());
        assertEquals("",record.getDate());record.setReservationDate(LocalDate.of(2026,10,19));assertEquals("2026-10-19",record.getDate());
        assertEquals("",record.getSubmittedAt());record.setCreatedAt(LocalDateTime.of(2026,10,19,9,30));assertEquals("10-19 09:30",record.getSubmittedAt());
        assertEquals("-",record.getReviewedAt());record.setUpdatedAt(LocalDateTime.of(2026,10,19,10,0));assertEquals("-",record.getReviewedAt());
        for(String reviewer:Arrays.asList(null,""," ","待审核")){record.setReviewerName(reviewer);assertEquals("待审核",record.getReviewer());}
        record.setReviewerName("Reviewer");assertEquals("Reviewer",record.getReviewer());assertEquals("10-19 10:00",record.getReviewedAt());
        for(Integer flag:Arrays.asList(null,0,1,2)){record.setConflictFlag(flag);assertEquals(Integer.valueOf(1).equals(flag),record.getConflict());}
        for(Boolean flag:Arrays.asList(null,false,true)){record.setConflict(flag);assertEquals(Boolean.TRUE.equals(flag),record.getConflict());}
    }
    @Test void reservationAndCountersignStatusColorsReflectAllSupportedStates() {
        var reservation=new ReservationsEntity();
        for(var entry:Map.of("已通过","success","已完成","success","冲突","danger","已驳回","info","已取消","info","已过期","info","待审核","warning").entrySet()){reservation.setStatus(entry.getKey());assertEquals(entry.getValue(),reservation.getTagType());}
        reservation.setStatus(null);assertEquals("warning",reservation.getTagType());
        for(var entry:Map.of("已同意","success","已退回","danger").entrySet()) {
            var sign=new ApprovalCountersignEntity();sign.setStatus(entry.getKey());assertEquals(entry.getValue(),sign.getTagType());
            sign.setStatus(null);sign.setResult(entry.getKey());assertEquals(entry.getValue(),sign.getTagType());
        }
        var sign=new ApprovalCountersignEntity();sign.setStatus("已取消");assertEquals("info",sign.getTagType());sign.setStatus(null);assertEquals("warning",sign.getTagType());
    }
    @Test void usageRecordLabelsPreserveAbsentTimeAndReviewStatuses() {
        var record=new UsageRecordEntity();assertEquals("",record.getTime());record.setUseTime(LocalDateTime.of(2026,10,19,9,30));assertEquals("2026-10-19 09:30:00",record.getTime());
        for(var entry:Map.of("正常","success","已归档","success","异常","danger","待复核","warning","其他","primary").entrySet()){record.setStatus(entry.getKey());assertEquals(entry.getValue(),record.getTagType());}
        record.setStatus(null);assertEquals("primary",record.getTagType());
    }
}
