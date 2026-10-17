/*
 * AI 业务数据查询回归测试；测试夹具与生产配置分离。
 * 覆盖场景：everyAssetDatasetIsExcludedBeforeAnyDatabaseAccess、roleAndCustomPermissionsAreCheckedBeforeReading、disabledOrRetiredAccountsCannotUseBusinessQueries、actualDatabasePermissionsAreUsedAndMalformedPermissionsFailClosed 等。
 * 主要协作组件：AiBusinessDataService。
 */
package org.example.backend.service;

import org.example.backend.entity.UserEntity;
import org.example.backend.entity.ConsumableEntity;
import org.example.backend.mapper.AiScheduleReadMapper;
import org.example.backend.mapper.UserMapper;
import org.example.backend.mapper.ClassTimetableMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiBusinessDataServiceTest {
    private final ApplicationContext beans = mock(ApplicationContext.class);
    private final AiBusinessDataService service = new AiBusinessDataService(beans,
            Clock.fixed(Instant.parse("2026-10-16T07:57:00Z"), ZoneOffset.UTC));
    private final AiDataActor admin = new AiDataActor(7, "labAdmin", "管理员", Set.of());

    @Test void everyAssetDatasetIsExcludedBeforeAnyDatabaseAccess() {
        for (String id : List.of("devices", "assets", "device-inventory-records", "device-transfer-records", "dashboard"))
            assertEquals("excluded", service.query(admin, Map.of("dataset", id)).get("status"));
        assertFalse(service.catalog(admin).stream().anyMatch(row -> row.get("dataset").contains("asset")));
        verifyNoInteractions(beans);
    }

    @Test void roleAndCustomPermissionsAreCheckedBeforeReading() {
        AiDataActor teacher = new AiDataActor(8, "teacher", "教师", Set.of());
        assertEquals("forbidden", service.query(teacher, Map.of("dataset", "consumables")).get("status"));
        AiDataActor limited = new AiDataActor(7, "labAdmin", "管理员", Set.of("AI智能助手", "课表数据"));
        assertEquals("forbidden", service.query(limited, Map.of("dataset", "maintenance")).get("status"));
        assertTrue(service.catalog(limited).stream().anyMatch(row -> row.get("dataset").equals("timetable")));
        verifyNoInteractions(beans);
    }

    @Test void disabledOrRetiredAccountsCannotUseBusinessQueries() {
        UserMapper users = mock(UserMapper.class);
        when(beans.getBean(UserMapper.class)).thenReturn(users);
        UserEntity user = new UserEntity(); user.setId(7); user.setStatus(0); user.setRoleCode("labAdmin");
        when(users.getUser(7)).thenReturn(user);
        assertThrows(IllegalArgumentException.class, () -> service.actor(7));
        user.setStatus(1); user.setRoleCode("maintenance");
        assertThrows(IllegalArgumentException.class, () -> service.actor(7));
        assertThrows(IllegalArgumentException.class, () -> service.actor(null));
    }

    @Test void actualDatabasePermissionsAreUsedAndMalformedPermissionsFailClosed() {
        UserMapper users = mock(UserMapper.class); when(beans.getBean(UserMapper.class)).thenReturn(users);
        UserEntity user = new UserEntity(); user.setId(7); user.setStatus(1); user.setRoleCode("labAdmin");
        user.setPermissions("[{\"items\":[\"课表数据\",\"AI智能助手\"]}]");
        when(users.getUser(7)).thenReturn(user);
        assertEquals(Set.of("课表数据", "AI智能助手"), service.actor(7).permissions());
        user.setPermissions("broken");
        assertThrows(IllegalArgumentException.class, () -> service.actor(7));
    }

    @Test void labReadsKeepManagerScopeAndReservationsKeepActorScope() {
        LabRecordReadService labs = mock(LabRecordReadService.class);
        when(beans.getBean(LabRecordReadService.class)).thenReturn(labs);
        when(labs.findAll(7L)).thenReturn(List.of());
        service.query(admin, Map.of("dataset", "labs")); verify(labs).findAll(7L);
        ReservationCatalogQuery reservations = mock(ReservationCatalogQuery.class);
        when(beans.getBean(ReservationCatalogQuery.class)).thenReturn(reservations);
        when(reservations.list(any())).thenReturn(List.of());
        service.query(admin, Map.of("dataset", "reservations"));
        ArgumentCaptor<ReservationAccessScope> capture = ArgumentCaptor.forClass(ReservationAccessScope.class);
        verify(reservations).list(capture.capture());
        assertEquals(7L, capture.getValue().actor().userId());
        assertEquals(ReservationActorRole.LABORATORY_ADMINISTRATOR, capture.getValue().actor().role());
    }

    @Test void credentialsAndIndirectAssetDataAreRemovedButSensorValuesRemain() {
        Object clean = service.sanitize(Map.of(
                "apiKey", "fixture-secret", "token", "fixture-token", "assetDeviceId", 12,
                "metricValue", 24.5, "value", 24.5,
                "records", List.of(Map.of("category", "device", "subject", "不应出现"),
                        Map.of("category", "reservation", "status", "已通过", "password", "fixture-password"))), "operation-events");
        String text = service.encode(clean);
        assertFalse(text.contains("fixture")); assertFalse(text.contains("assetDeviceId"));
        assertFalse(text.contains("不应出现")); assertTrue(text.contains("已通过"));
        assertTrue(text.contains("24.5"));
        assertFalse(service.encode(service.sanitize(Map.of("bindings", List.of(Map.of("code", "fixture-code", "status", "active"))), "activation-codes")).contains("fixture-code"));
        assertNull(service.sanitize(Map.of("category", "activation-code", "subject", "fixture-activation-secret"), "operation-events"));
        assertNull(service.sanitize(Map.of("title", "资产台账统计", "content", "金额10000"), "notices"));
        String snapshot = service.encode(service.sanitize(Map.of("metricSnapshot", "{\"token\":\"fixture-token\",\"cpuUsage\":12}"), "runtime-status"));
        assertFalse(snapshot.contains("fixture-token")); assertTrue(snapshot.contains("cpuUsage"));
    }

    @Test void crawlerStatusDoesNotReadLogFilesOrExposeCredentials() {
        ClassTimetableMapper mapper = mock(ClassTimetableMapper.class);
        when(beans.getBean(ClassTimetableMapper.class)).thenReturn(mapper);
        when(mapper.listCrawlerTasks(200)).thenReturn(List.of(Map.of("id", 1, "status", "success", "updatedCount", 80,
                "output", "fixture-secret-in-output", "logPath", "fixture-private-path", "successAccount", "fixture-account")));
        String result = service.encode(service.query(admin, Map.of("dataset", "timetable-sync-tasks")));
        assertFalse(result.contains("fixture")); assertTrue(result.contains("updatedCount"));
        verify(mapper).listCrawlerTasks(200); verifyNoMoreInteractions(mapper);
    }

    @Test void currentClassQueryUsesShanghaiTimeActiveCalendarWeekAndTeachingPeriod() {
        bindLab();
        TeachingScheduleReadService calendar = mock(TeachingScheduleReadService.class);
        when(beans.getBean(TeachingScheduleReadService.class)).thenReturn(calendar);
        when(calendar.findActiveCalendar()).thenReturn(new ScheduleCalendarData("2026秋", "2026-09-07"));
        AiScheduleReadMapper mapper = mock(AiScheduleReadMapper.class);
        when(beans.getBean(AiScheduleReadMapper.class)).thenReturn(mapper);
        when(mapper.count(any())).thenReturn(1L);
        when(mapper.search(any())).thenReturn(List.of(new ScheduleEntryView(1L, "2026秋", "一班", "一班", "星期五",
                "0708", "7-8节", 7, 8, "程序设计", "张老师", "6", "6", "6", "2-206M、9-301", null)));
        Map<String, Object> result = service.query(admin, Map.of("dataset", "timetable", "mode", "now"));
        assertEquals("ok", result.get("status"));
        assertTrue(service.encode(result).contains("张老师"));
        ArgumentCaptor<Map<String, Object>> capture = ArgumentCaptor.forClass(Map.class);
        verify(mapper).search(capture.capture());
        Map<String, Object> filters = capture.getValue();
        assertEquals("2026秋", filters.get("semester")); assertEquals(6L, filters.get("week"));
        assertEquals("星期五", filters.get("weekday")); assertEquals(7, filters.get("sectionStart"));
        assertEquals(8, filters.get("sectionEnd")); assertEquals("15:50-17:20", result.get("timeRange"));
        assertNotNull(filters.get("labPatterns"));
        assertTrue(service.encode(result).contains("移动互联实训室"));
        assertFalse(service.encode(result).contains("9-301"));
        List<String> patterns = (List<String>) filters.get("labPatterns");
        assertTrue(patterns.stream().anyMatch(pattern -> java.util.regex.Pattern.compile(pattern).matcher("2-206M").find()));
        assertFalse(patterns.stream().anyMatch(pattern -> java.util.regex.Pattern.compile(pattern).matcher("12-206").find()));
        assertFalse(patterns.stream().anyMatch(pattern -> java.util.regex.Pattern.compile(pattern).matcher("2-2060").find()));
    }

    @Test void missingCalendarIsUnavailableRatherThanAnEmptySchedule() {
        bindLab();
        TeachingScheduleReadService calendar = mock(TeachingScheduleReadService.class);
        when(beans.getBean(TeachingScheduleReadService.class)).thenReturn(calendar);
        Map<String, Object> result = service.query(admin, Map.of("dataset", "timetable", "mode", "now"));
        assertEquals("unavailable", result.get("status")); assertTrue(result.get("message").toString().contains("开学日期"));
        verify(beans, never()).getBean(AiScheduleReadMapper.class);
    }

    private void bindLab() {
        LabRecordReadService labs = mock(LabRecordReadService.class);
        when(beans.getBean(LabRecordReadService.class)).thenReturn(labs);
        when(labs.findAll(7L)).thenReturn(List.of(new LabRecord(1L, "2-206", "移动互联实训室", "2号楼", "2", "206",
                "教学", 40, 7L, "开放", "", null, null)));
    }

    @Test void noBoundLabsNeverFallsBackToCampusTimetable() {
        LabRecordReadService labs = mock(LabRecordReadService.class);
        when(beans.getBean(LabRecordReadService.class)).thenReturn(labs);
        when(labs.findAll(7L)).thenReturn(List.of());
        Map<String, Object> result = service.query(admin, Map.of("dataset", "timetable", "mode", "now"));
        assertEquals(0, result.get("total")); assertTrue(result.get("message").toString().contains("尚未绑定"));
        verify(beans, never()).getBean(AiScheduleReadMapper.class);
    }

    @Test void noClassPeriodEndsExactlyAtTheConfiguredEndTime() {
        assertEquals(7, TeachingSectionTimes.currentSection(LocalTime.of(15, 50)));
        assertNull(TeachingSectionTimes.currentSection(LocalTime.of(17, 20)));
        assertNull(TeachingSectionTimes.currentSection(LocalTime.of(13, 0)));
    }

    @Test void totalsAndGroupingUseAllMatchingRecordsBeforePagination() {
        ModuleRecordService modules = mock(ModuleRecordService.class); when(beans.getBean(ModuleRecordService.class)).thenReturn(modules);
        when(modules.listModuleRecords("operation-events")).thenReturn(List.of(
                Map.of("id", "1", "status", "成功"), Map.of("id", "2", "status", "成功"), Map.of("id", "3", "status", "失败")));
        Map<String, Object> result = service.query(admin, Map.of("dataset", "operation-events", "limit", 1, "offset", 1, "groupBy", "status"));
        assertEquals(3, result.get("total")); assertEquals(true, result.get("hasMore"));
        assertEquals(Map.of("成功", 2L, "失败", 1L), result.get("groups"));
        assertEquals(List.of(Map.of("id", "2", "status", "成功")), result.get("records"));
    }
}
