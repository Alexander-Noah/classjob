package org.example.backend.service;

import org.example.backend.entity.CourseEnvironmentEntity;
import org.example.backend.entity.EnvironmentTemplateEntity;
import org.example.backend.entity.NoticeEntity;
import org.example.backend.entity.RepairEntity;
import org.example.backend.mapper.CourseEnvironmentMapper;
import org.example.backend.mapper.EnvironmentTemplateMapper;
import org.example.backend.mapper.NoticeMapper;
import org.example.backend.mapper.RepairMapper;
import org.example.backend.service.impl.CourseEnvironmentServiceImpl;
import org.example.backend.service.impl.RepairServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EnvironmentRepairBehaviorTest {
    private final BusinessLoopService events = mock(BusinessLoopService.class);

    @Test
    void courseEnvironmentDefaultsAndAliasesAreSavedAndReturnedFromPersistence() {
        var mapper = mock(CourseEnvironmentMapper.class);
        var templates = mock(EnvironmentTemplateMapper.class);
        var notices = mock(NoticeMapper.class);
        var service = new CourseEnvironmentServiceImpl(mapper, templates, notices, events);
        var saved = new CourseEnvironmentEntity(); saved.setId(1); saved.setCourseName("程序设计"); saved.setProcessStatus("待配置");
        when(mapper.getCourseEnvironment(any())).thenReturn(saved);
        when(mapper.getEnvironment()).thenReturn(List.of(saved));
        assertSame(saved, service.InserterCourseEnvironment(null));
        var captor = ArgumentCaptor.forClass(CourseEnvironmentEntity.class);
        verify(mapper).InserterCourseEnvironment(captor.capture());
        var defaults = captor.getValue();
        assertEquals("新课程环境", defaults.getCourseName()); assertEquals("新课程环境", defaults.getCourse());
        assertEquals("待配置", defaults.getStatus()); assertEquals("待确认", defaults.getConfirmStatus());
        assertEquals(3, defaults.getTeacherUserId()); assertEquals("待填写班级", defaults.getClassName());
        assertEquals("待确认", defaults.getUseTime()); assertEquals("计算机实验室", defaults.getLabType());
        var request = new CourseEnvironmentEntity(); request.setCourse("数据库"); request.setSoftware("MySQL"); request.setAssignedLabId(7L);
        assertSame(saved, service.updateCourseEnvironment(1, request));
        assertEquals("数据库", request.getCourseName()); assertEquals("MySQL", request.getSoftwareRequirements());
        assertEquals("配置中", request.getProcessStatus());
        assertEquals(List.of(saved), service.getEnvironment()); assertSame(saved, service.getCourseEnvironment(1));
        assertEquals(List.of(saved), service.deleteCourseEnvironment(1)); verify(mapper).deleteCourseEnvironment(1);
        verify(templates, never()).insertTemplate(any());
    }

    @Test
    void confirmedEnvironmentsCreateOrUpdateReusableTemplateAndPublishNotice() {
        var mapper = mock(CourseEnvironmentMapper.class);
        var templates = mock(EnvironmentTemplateMapper.class);
        var notices = mock(NoticeMapper.class);
        var service = new CourseEnvironmentServiceImpl(mapper, templates, notices, events);
        var saved = new CourseEnvironmentEntity(); saved.setId(1); saved.setCourseName("数据库"); saved.setConfirmStatus("已确认"); saved.setAssignedLabName("机房"); saved.setSoftwareRequirements("MySQL"); saved.setSystemRequirements("Linux");
        when(mapper.getCourseEnvironment(1)).thenReturn(saved);
        assertSame(saved, service.confirmCourseEnvironment(1));
        var template = ArgumentCaptor.forClass(EnvironmentTemplateEntity.class);
        verify(templates).insertTemplate(template.capture());
        assertEquals("数据库环境模板", template.getValue().getName()); assertEquals("可复用", template.getValue().getStatus());
        assertEquals("MySQL", template.getValue().getSoftwareList()); assertEquals("Linux", template.getValue().getOs()); assertEquals("机房", template.getValue().getLabs());
        var notice = ArgumentCaptor.forClass(NoticeEntity.class); verify(notices).insertNotice(notice.capture());
        assertEquals("数据库课程环境已确认", notice.getValue().getTitle()); assertEquals("已发布", notice.getValue().getPublishStatus()); assertNotNull(notice.getValue().getPublishTime());
        var existing = new EnvironmentTemplateEntity(); existing.setId(9L);
        when(templates.getTemplateByCourse("数据库")).thenReturn(existing);
        service.updateCourseEnvironment(1, saved);
        verify(templates).updateTemplate(eq(9L), any());
        when(mapper.getCourseEnvironment(99)).thenReturn(null);
        assertNull(service.confirmCourseEnvironment(99));
        service.deleteCourseEnvironment(99);
    }

    @Test
    void repairsNormalizeDefaultsAndSynchronizeSavedDeviceState() {
        var mapper = mock(RepairMapper.class); var service = new RepairServiceImpl(mapper, events);
        when(mapper.getRepair(anyString())).thenAnswer(call -> { var value = new RepairEntity(); value.setTicket(call.getArgument(0)); value.setStatus("待派单"); return value; });
        var request = new RepairEntity();
        var saved = service.createRepair(request);
        assertTrue(request.getTicket().matches("R\\d{8}[A-F0-9]{4}")); assertEquals("待派单", request.getStatus());
        assertEquals("待分配", request.getAssignee()); assertEquals(8, request.getProgress()); assertEquals("待维修人员接单。", request.getResult());
        assertEquals(request.getTicket(), saved.getTicket()); verify(events).syncDeviceAfterRepairChanged(saved);
        when(mapper.updateRepair(anyString(), any())).thenReturn(1);
        assertNotNull(service.updateRepair(request.getTicket(), request));
        when(mapper.updateRepair(anyString(), any())).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.updateRepair("missing", request));
        when(mapper.listRepairs()).thenReturn(null); assertEquals(List.of(), service.listRepairs());
        when(mapper.listRepairs()).thenReturn(List.of(saved)); assertEquals(List.of(saved), service.resetRepairs());
    }

    @Test
    void repairBatchAndPatchPreserveHistoryAndOnlyFillMissingPatchFields() {
        var mapper = mock(RepairMapper.class); var service = new RepairServiceImpl(mapper, events);
        var saved = new RepairEntity(); saved.setTicket("R1"); saved.setStatus("维修中"); saved.setProgress(50);
        when(mapper.getRepair("R1")).thenReturn(saved); when(mapper.listRepairs()).thenReturn(List.of(saved));
        service.replaceRepairs(null); service.replaceRepairs(List.of()); verify(mapper, never()).deleteAllRepairs();
        service.replaceRepairs(List.of(saved)); verify(mapper).deleteAllRepairs(); verify(mapper).insertRepair(saved);
        when(mapper.patchRepair(eq("R1"), any())).thenReturn(1);
        assertSame(saved, service.patchRepair("R1", null, "维修中", 50, "正在维修"));
        var captor = ArgumentCaptor.forClass(RepairEntity.class); verify(mapper).patchRepair(eq("R1"), captor.capture());
        assertEquals("维修中", captor.getValue().getStatus()); assertEquals(50, captor.getValue().getProgress()); assertEquals("正在维修", captor.getValue().getResult());
        var explicit = new RepairEntity(); explicit.setStatus("已完成"); explicit.setProgress(100); explicit.setResult("已修复");
        service.patchRepair("R1", explicit, "维修中", 50, "默认"); assertEquals("已完成", explicit.getStatus());
        assertThrows(IllegalArgumentException.class, () -> service.patchRepair("missing", null, "维修中", 50, "默认"));
        service.deleteRepair("R1"); service.deleteRepair("missing"); verify(mapper).deleteRepair("R1"); verify(mapper).deleteRepair("missing");
    }
}
