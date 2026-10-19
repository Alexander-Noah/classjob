package org.example.backend.service;

import org.example.backend.entity.ApprovalCountersignEntity;
import org.example.backend.entity.ReservationsEntity;
import org.example.backend.mapper.ApprovalCountersignMapper;
import org.example.backend.mapper.ReservationsMapper;
import org.example.backend.service.impl.ApprovalCountersignServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalCountersignBehaviorTest {
    final ApprovalCountersignMapper mapper=mock(ApprovalCountersignMapper.class);
    final ReservationsMapper reservations=mock(ReservationsMapper.class);
    final BusinessLoopService loop=mock(BusinessLoopService.class);
    final ApprovalCountersignService service=new ApprovalCountersignServiceImpl(mapper,reservations,loop);
    ApprovalCountersignEntity current;
    ApprovalCountersignEntity payload(){ApprovalCountersignEntity row=new ApprovalCountersignEntity();row.setBusinessType(" reservation ");row.setBusinessId(" 7 ");row.setAssigneeName(" 教师甲 ");row.setReason(" 需要专业意见 ");return row;}
    void persistence(){
        when(mapper.insertCountersign(any())).thenAnswer(call->{current=call.getArgument(0);current.setId(1L);return 1;});
        when(mapper.getCountersign(1L)).thenAnswer(call->current);
        when(mapper.completeCountersign(eq(1L),anyString(),anyString(),anyString())).thenAnswer(call->{current.setStatus(call.getArgument(1));current.setResult(call.getArgument(2));current.setResultRemark(call.getArgument(3));return 1;});
        when(mapper.cancelCountersign(eq(1L),anyString())).thenAnswer(call->{current.setStatus("已取消");current.setResultRemark(call.getArgument(1));return 1;});
        ReservationsEntity reservation=new ReservationsEntity();reservation.setApplicantName("李老师");reservation.setLabName("A101");reservation.setReservationDate(java.time.LocalDate.of(2026,10,18));reservation.setStatus("待审核");when(reservations.getReservation(7)).thenReturn(reservation);
    }
    @Test void createNormalizesAndLinksReservationWhileRecordingAudit(){
        persistence();ApprovalCountersignEntity result=service.createCountersign(payload());assertEquals("待加签",result.getStatus());assertEquals("warning",result.getTagType());assertEquals("reservation",result.getBusinessType());assertEquals("教师甲",result.getAssigneeName());assertEquals("张明",result.getAssignerName());assertEquals("李老师 · A101 · 2026-10-18",result.getBusinessTitle());assertEquals("待审核",result.getBusinessStatus());
        verify(reservations).updateReviewStatus(7,"加签中","张明","加签给教师甲：需要专业意见");verify(loop).recordEvent(eq("approval-countersign"),eq("create"),eq(result.getBusinessTitle()),eq("待加签"),eq(Map.of("businessType","reservation","businessId","7","assigneeName","教师甲")));
    }
    @Test void nonReservationBusinessUsesFallbackTitleAndDoesNotMutateReservations(){
        persistence();ApprovalCountersignEntity input=payload();input.setBusinessType("repair");ApprovalCountersignEntity result=service.createCountersign(input);assertEquals("repair#7",result.getBusinessTitle());verifyNoInteractions(reservations);
        service.listCountersigns(" repair "," 7 "," ",null);verify(mapper).listCountersigns("repair","7",null,null);
    }
    @Test void rejectsMissingInvalidAndDuplicateBusinessInformation(){
        assertThrows(IllegalArgumentException.class,()->service.createCountersign(null));ApprovalCountersignEntity input=payload();input.setBusinessType(" ");assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));input.setBusinessType("reservation");input.setBusinessId(null);assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));input.setBusinessId("bad");assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));input.setBusinessId("7");assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));
        when(mapper.countActiveByBusiness("reservation","7")).thenReturn(1);assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));verify(mapper,never()).insertCountersign(any());
    }
    @ParameterizedTest @ValueSource(strings={"", "a", "x<script>bad</script>", "javascript:alert(1)"}) void rejectsInvalidAssigneesAndReasons(String invalid){
        ApprovalCountersignEntity input=payload();input.setAssigneeName(invalid);assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));ApprovalCountersignEntity reasonInput=payload();reasonInput.setReason(invalid);assertThrows(IllegalArgumentException.class,()->service.createCountersign(reasonInput));
    }
    @Test void rejectsOversizedText(){ApprovalCountersignEntity input=payload();input.setReason("x".repeat(501));assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));input.setReason("需要审核信息");input.setAssigneeName("x".repeat(81));assertThrows(IllegalArgumentException.class,()->service.createCountersign(input));}
    @ParameterizedTest @ValueSource(strings={"同意","已同意","退回","驳回","已退回"}) void completionNormalizesDecisionAndResumesOrRejectsOriginalReview(String decision){
        persistence();service.createCountersign(payload());clearInvocations(reservations);ApprovalCountersignEntity input=new ApprovalCountersignEntity();input.setResult(decision);ApprovalCountersignEntity result=service.completeCountersign(1L,input);boolean approve=decision.contains("同意");assertEquals(approve?"已同意":"已退回",result.getStatus());assertEquals(approve?"success":"danger",result.getTagType());verify(reservations).updateReviewStatus(eq(7),eq(approve?"待审核":"已驳回"),eq("教师甲"),eq(result.getResultRemark()));assertThrows(IllegalArgumentException.class,()->service.completeCountersign(1L,input));assertThrows(IllegalArgumentException.class,()->service.cancelCountersign(1L,null));
    }
    @Test void nullCompletionDefaultsToApprovalAndCancellationResumesReview(){
        persistence();service.createCountersign(payload());assertEquals("已同意",service.completeCountersign(1L,null).getResult());current.setStatus("待加签");current.setResult(null);ApprovalCountersignEntity input=new ApprovalCountersignEntity();input.setResultRemark("  审批人撤回  ");ApprovalCountersignEntity result=service.cancelCountersign(1L,input);assertEquals("已取消",result.getStatus());assertEquals("info",result.getTagType());verify(reservations).updateReviewStatus(7,"待审核","加签已撤销","审批人撤回");
    }
    @Test void missingRecordsAndConcurrentUpdatesFailWithoutAudit(){
        assertThrows(IllegalArgumentException.class,()->service.completeCountersign(null,null));assertThrows(IllegalArgumentException.class,()->service.cancelCountersign(9L,null));persistence();service.createCountersign(payload());clearInvocations(loop);ApprovalCountersignEntity invalid=new ApprovalCountersignEntity();invalid.setResult("maybe");assertThrows(IllegalArgumentException.class,()->service.completeCountersign(1L,invalid));
        when(mapper.completeCountersign(eq(1L),anyString(),anyString(),anyString())).thenReturn(0);assertThrows(IllegalArgumentException.class,()->service.completeCountersign(1L,null));when(mapper.cancelCountersign(eq(1L),anyString())).thenReturn(0);assertThrows(IllegalArgumentException.class,()->service.cancelCountersign(1L,null));verifyNoInteractions(loop);
    }
}
