package org.example.backend.service;

import org.example.backend.entity.NoticeEntity;
import org.example.backend.entity.ScheduleAdjustmentEntity;
import org.example.backend.mapper.NoticeMapper;
import org.example.backend.mapper.ScheduleAdjustmentMapper;
import org.example.backend.service.impl.ScheduleAdjustmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleAdjustmentBehaviorTest {
    final ScheduleAdjustmentMapper mapper=mock(ScheduleAdjustmentMapper.class);
    final NoticeMapper notices=mock(NoticeMapper.class);
    final BusinessLoopService loop=mock(BusinessLoopService.class);
    final ScheduleAdjustmentService service=new ScheduleAdjustmentServiceImpl(mapper,notices,loop);
    ScheduleAdjustmentEntity current;
    @BeforeEach void setup(){when(mapper.insertAdjustment(any())).thenAnswer(call->{current=call.getArgument(0);current.setId(1L);return 1;});when(mapper.getAdjustment(1L)).thenAnswer(call->current);when(mapper.updateAdjustment(eq(1L),any())).thenAnswer(call->{current=call.getArgument(1);current.setId(1L);return 1;});when(mapper.patchAdjustment(eq(1L),anyString(),anyString(),anyInt())).thenAnswer(call->{current.setStatus(call.getArgument(1));current.setTagType(call.getArgument(2));current.setFlowStep(call.getArgument(3));return 1;});}
    @Test void submissionAppliesDefaultsPublishesAdminNoticeAndRecordsAudit(){ScheduleAdjustmentEntity result=service.createAdjustment(new ScheduleAdjustmentEntity());assertEquals("未命名课程",result.getCourse());assertEquals("审核中",result.getStatus());assertEquals("任课教师",result.getTeacherName());assertEquals("教学安排调整",result.getReason());assertEquals(2,result.getFlowStep());var notice=ArgumentCaptor.forClass(NoticeEntity.class);verify(notices).insertNotice(notice.capture());assertEquals("labAdmin",notice.getValue().getTargetRole());assertEquals("已发布",notice.getValue().getPublishStatus());assertTrue(notice.getValue().getContent().contains("目标时间：待确认"));verify(loop).recordEvent(eq("schedule-adjustment"),eq("submit"),eq("未命名课程"),eq("审核中"),anyMap());}
    @Test void approvalAndCompletionAdvanceWorkflowAndNotifyTeachers(){service.createAdjustment(new ScheduleAdjustmentEntity());clearInvocations(notices,loop);assertEquals("已通过",service.approveAdjustment(1L).getStatus());assertEquals(3,current.getFlowStep());assertEquals("已调整",service.completeAdjustment(1L).getStatus());assertEquals(4,current.getFlowStep());var notice=ArgumentCaptor.forClass(NoticeEntity.class);verify(notices,times(2)).insertNotice(notice.capture());assertTrue(notice.getAllValues().stream().allMatch(item->"teacher".equals(item.getTargetRole())));verify(loop).recordEvent(eq("schedule-adjustment"),eq("approve"),anyString(),eq("已通过"),anyMap());verify(loop).recordEvent(eq("schedule-adjustment"),eq("complete"),anyString(),eq("已调整"),anyMap());}
    @Test void updatePreservesExplicitDetailsAndMissingRowsFail(){ScheduleAdjustmentEntity input=new ScheduleAdjustmentEntity();input.setCourse("数据库");input.setOriginalTime("周一");input.setTargetTime("周二");input.setReason("会议冲突");input.setTeacherName("教师甲");input.setReviewer("管理员");input.setStatus("已取消");input.setTagType("info");input.setFlowStep(1);assertSame(input,service.updateAdjustment(1L,input));assertEquals("数据库",current.getCourse());verifyNoInteractions(notices);assertThrows(IllegalArgumentException.class,()->service.updateAdjustment(9L,new ScheduleAdjustmentEntity()));assertThrows(IllegalArgumentException.class,()->service.approveAdjustment(9L));assertThrows(IllegalArgumentException.class,()->service.completeAdjustment(9L));}
    @Test void listNormalizesNullAndAbsentSavedRowsAvoidSpuriousNotices(){when(mapper.listAdjustments()).thenReturn(null);assertEquals(List.of(),service.listAdjustments());ScheduleAdjustmentEntity input=new ScheduleAdjustmentEntity();when(mapper.listAdjustments()).thenReturn(List.of(input));assertEquals(List.of(input),service.listAdjustments());when(mapper.getAdjustment(1L)).thenReturn(null);assertNull(service.createAdjustment(input));verifyNoInteractions(notices,loop);}
}
