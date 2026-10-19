package org.example.backend.service;

import org.example.backend.entity.LabEntity;
import org.example.backend.entity.ReservationsEntity;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.mapper.LabMapper;
import org.example.backend.mapper.ReservationsMapper;
import org.example.backend.service.impl.ReservationsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReservationWorkflowBehaviorTest {
    @Test void conflictChecksPassEachCalendarWeekdayWithoutMondayFallback(){LocalDate monday=LocalDate.of(2027,1,4);when(timetable.getCrawlerConfig(anyString())).thenReturn(Map.of("semesterStartDate",monday.toString()));for(int day=0;day<7;day++){ReservationsEntity record=reservation();record.setReservationDate(monday.plusDays(day));service.insertReservation(record);verify(mapper).countTimetableConflicts(eq(record),anyInt(),eq("星期"+"一二三四五六日".charAt(day)));}}
    private final ReservationsMapper mapper = mock(ReservationsMapper.class);
    private final LabMapper labs = mock(LabMapper.class);
    private final ClassTimetableMapper timetable = mock(ClassTimetableMapper.class);
    private final BusinessLoopService events = mock(BusinessLoopService.class);
    private final ReservationsServiceImpl service = new ReservationsServiceImpl(mapper, labs, timetable, events);
    private ReservationsEntity saved;
    private LabEntity lab;
    @BeforeEach
    void setup() {
        saved = reservation();
        lab = new LabEntity(); lab.setId(7); lab.setLabCode("A101"); lab.setLabName("机房"); lab.setCapacity(40); lab.setOpenStatus("开放");
        when(labs.getLabById(7)).thenReturn(lab);
        when(labs.getLabs()).thenReturn(List.of(lab));
        when(mapper.getReservations()).thenReturn(List.of());
        when(mapper.getReservation(1)).thenReturn(saved);
        when(mapper.updateReservation(anyInt(), any())).thenReturn(1);
        when(mapper.getApproved(anyInt(), any())).thenAnswer(call -> { saved.setStatus("已通过"); return 1; });
        when(mapper.getRejected(anyInt(), any())).thenAnswer(call -> { saved.setStatus("已驳回"); return 1; });
        when(timetable.getCrawlerConfig(anyString())).thenReturn(Map.of("semesterStartDate", LocalDate.now().toString()));
    }

    @Test
    void insertResolvesCanonicalLabDefaultsAndRecordsEvent() {
        saved.setStatus("加签中"); saved.setAttendeeCount(null); saved.setReviewerName(null);
        saved.setApplicantName(" 张同学 "); saved.setLabId(null); saved.setLabName(" A101 机房 ");
        service.insertReservation(saved);
        assertEquals("张同学", saved.getApplicantName());
        assertEquals(7L, saved.getLabId()); assertEquals("机房", saved.getLabName()); assertEquals("A101", saved.getLabCode());
        assertEquals("待审核", saved.getStatus()); assertEquals(1, saved.getAttendeeCount()); assertEquals("待审核", saved.getReviewerName());
        assertFalse(saved.getConflict());
        verify(mapper).insertReservation(saved);
        verify(events).recordEvent(eq("reservation"), eq("create"), eq("A101 机房"), eq("待审核"), anyMap());
    }

    @Test
    void reservationAndTimetableConflictsMarkPendingApplications() {
        when(mapper.countConflictingReservations(isNull(), any())).thenReturn(1);
        when(mapper.countTimetableConflicts(any(), anyInt(), anyString())).thenReturn(1);
        service.insertReservation(saved);
        assertEquals("冲突", saved.getStatus()); assertTrue(saved.getConflict());
        assertTrue(saved.getReviewRemark().contains("预约记录")); assertTrue(saved.getReviewRemark().contains("课表占用"));
        verify(mapper).countTimetableConflicts(eq(saved), anyInt(), anyString());
    }

    @Test
    void expiredInsertAndTerminalStatusesDoNotRunAvailabilityQueries() {
        saved.setReservationDate(LocalDate.now().minusDays(1)); saved.setReviewerName("");
        service.insertReservation(saved);
        assertEquals("已过期", saved.getStatus()); assertFalse(saved.getConflict());
        assertEquals("预约已过期。", saved.getReviewRemark());
        verify(mapper, never()).countConflictingReservations(any(), any());
        for (String status : List.of("已驳回", "已取消", "已过期", "已完成")) {
            var value = reservation(); value.setStatus(status); value.setConflict(true);
            service.insertReservation(value); assertFalse(value.getConflict());
        }
    }

    @Test
    void readsRefreshLegacyAndExpiredApplications() {
        var expired = reservation(); expired.setReservationDate(LocalDate.now().minusDays(1));
        var legacy = reservation(); legacy.setId(2L); legacy.setStatus("加签中");
        when(mapper.getReservations()).thenReturn(List.of(expired, legacy));
        assertEquals(List.of(expired, legacy), service.getReservations());
        assertEquals("已过期", expired.getStatus()); assertEquals("待审核", legacy.getStatus());
        verify(mapper).updateReviewStatus(1, "已过期", "系统", "预约已过期。");
        verify(mapper).updateReviewStatus(eq(2), eq("待审核"), eq("待审核"), anyString());
        assertSame(saved, service.getReservation(1));
    }

    @Test
    void updatesAndDeletesRefreshLabOpenStatusFromCurrentAndUpcomingReservations() {
        for (String target : List.of("使用中", "预约中", "开放", "维护中", "停用")) {
            when(mapper.countCurrentApprovedReservationsByLabId(7L)).thenReturn(target.equals("使用中") ? 1 : 0);
            when(mapper.countUpcomingApprovedReservationsByLabId(7L)).thenReturn(target.equals("预约中") ? 1 : 0);
            lab.setOpenStatus(target);
            service.updateReservation(1, reservation());
            verify(labs, atLeastOnce()).updateLabOpenStatus(7, target);
        }
        service.deleteReservation(1); verify(mapper).deleteReservation(1);
        service.deleteReservation(999); verify(mapper).deleteReservation(999);
        assertThrows(IllegalArgumentException.class, () -> service.updateReservation(999, reservation()));
        when(mapper.updateReservation(1, saved)).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.updateReservation(1, saved));
    }

    @Test
    void cancellationRequiresActiveFutureStatusAndAReason() {
        var next = reservation(); next.setStatus("已取消"); next.setReviewRemark("教学计划调整");
        for (String status : List.of("已完成", "已取消", "已驳回", "已过期")) {
            saved.setStatus(status);
            assertThrows(IllegalArgumentException.class, () -> service.updateReservation(1, next));
            assertThrows(IllegalArgumentException.class, () -> service.updateReservation(1, reservation()));
        }
        saved.setStatus("待审核"); next.setReviewRemark(null);
        assertThrows(IllegalArgumentException.class, () -> service.updateReservation(1, next));
        next.setReviewRemark("教学计划调整"); saved.setReservationDate(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.updateReservation(1, next));
        saved = reservation(); when(mapper.getReservation(1)).thenReturn(saved);
        service.updateReservation(1, next);
        verify(mapper).updateReservation(1, next);
    }

    @Test
    void approvalTransitionsAndSynchronizesUsageAndRejectsUnavailableResources() {
        var patch = new ReservationsEntity(); patch.setReviewRemark("管理员审核通过");
        assertSame(saved, service.getApproved(1, patch));
        assertEquals("已通过", saved.getStatus());
        verify(events).syncUsageRecordAfterReservationApproved(saved);
        assertThrows(IllegalArgumentException.class, () -> service.getApproved(1, patch));
        saved.setStatus("冲突"); assertThrows(IllegalArgumentException.class, () -> service.getApproved(1, patch));
        saved.setStatus("待审核"); saved.setConflict(true); assertThrows(IllegalArgumentException.class, () -> service.getApproved(1, patch));
        saved.setConflict(false); saved.setAttendeeCount(41); assertThrows(IllegalArgumentException.class, () -> service.getApproved(1, patch));
        saved.setAttendeeCount(10); saved.setReservationDate(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.getApproved(1, patch)); assertEquals("已过期", saved.getStatus());
        assertThrows(IllegalArgumentException.class, () -> service.getApproved(999, patch));
    }

    @Test
    void rejectionAcceptsPendingOrConflictAndRejectsExpiredOrMissingRecords() {
        var patch = new ReservationsEntity(); patch.setReviewRemark("信息填写不完整");
        assertSame(saved, service.getRejected(1, patch));
        assertEquals("已驳回", saved.getStatus());
        assertThrows(IllegalArgumentException.class, () -> service.getRejected(1, patch));
        saved.setStatus("冲突"); service.getRejected(1, patch);
        saved.setStatus("待审核"); saved.setReservationDate(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.getRejected(1, patch));
        assertThrows(IllegalArgumentException.class, () -> service.getRejected(999, patch));
        patch.setReviewRemark(" "); assertThrows(IllegalArgumentException.class, () -> service.getRejected(1, patch));
    }

    @Test
    void bulkReplacementIgnoresEmptyInputAndNormalizesNonEmptyRecords() {
        service.replaceReservations(null); service.replaceReservations(List.of());
        verify(mapper, never()).deleteAllReservations();
        service.replaceReservations(List.of(saved));
        verify(mapper).deleteAllReservations(); verify(mapper).insertReservation(saved);
    }

    @Test
    void scanProfileAndStatusVerifyLabCodeAndContactBeforeExposingDetails() {
        assertEquals("A101 机房", service.getScanReservationProfile(7, "a101").get("displayName"));
        assertThrows(IllegalArgumentException.class, () -> service.getScanReservationProfile(7, "wrong"));
        assertThrows(IllegalArgumentException.class, () -> service.getScanReservationProfile(999, null));
        assertThrows(IllegalArgumentException.class, () -> service.getScanReservationStatus(null, "13800138000"));
        assertThrows(IllegalArgumentException.class, () -> service.getScanReservationStatus(1, " "));
        assertThrows(IllegalArgumentException.class, () -> service.getScanReservationStatus(1, "wrong"));
        assertThrows(IllegalArgumentException.class, () -> service.getScanReservationStatus(999, "13800138000"));
        var status = service.getScanReservationStatus(1, "138 0013 8000");
        assertEquals(1L, status.get("id")); assertEquals("张同学", status.get("applicant"));
        assertEquals(saved.getDate(), status.get("date"));
    }

    @Test
    void scanSubmissionRejectsCapacityConflictsPastDatesAndInvalidContact() {
        assertThrows(IllegalArgumentException.class, () -> service.submitScanReservation(null));
        for (Consumer<ReservationsEntity> change : List.<Consumer<ReservationsEntity>>of(
                value -> value.setDepartment(null), value -> value.setContact("张老师"),
                value -> value.setReservationDate(LocalDate.now().minusDays(1)), value -> value.setAttendeeCount(41))) {
            var request = reservation(); change.accept(request);
            assertThrows(IllegalArgumentException.class, () -> service.submitScanReservation(request));
        }
        when(mapper.countConflictingReservations(isNull(), any())).thenReturn(1);
        assertThrows(IllegalArgumentException.class, () -> service.submitScanReservation(reservation()));
        when(mapper.countConflictingReservations(isNull(), any())).thenReturn(0);
        assertSame(saved, service.submitScanReservation(saved));
        assertEquals("待审核", saved.getStatus()); assertFalse(saved.getConflict());
        assertEquals("扫码提交，待管理员审核。", saved.getReviewRemark());
    }

    @Test
    void fieldValidationRejectsUnsafeTextBadTimeUnsupportedScenesAndUnresolvedLabs() {
        List<Consumer<ReservationsEntity>> invalid = List.of(
                value -> value.setApplicantName(null), value -> value.setApplicantName("一"),
                value -> value.setApplicantName("a".repeat(51)), value -> value.setApplicantName("12345"),
                value -> value.setReason("<script>alert(1)</script>"), value -> value.setContact(null),
                value -> value.setContact("<img src=x>"), value -> value.setContact("a".repeat(81)),
                value -> value.setContact("12345"), value -> value.setReservationDate(null),
                value -> value.setTimeRange("bad"), value -> value.setTimeRange("25:00-26:00"),
                value -> value.setTimeRange("10:00-08:00"), value -> value.setScene(null),
                value -> value.setScene("其他"), value -> { value.setLabId(null); value.setLabName(null); value.setLabCode(null); });
        for (var change : invalid) {
            var request = reservation(); change.accept(request);
            assertThrows(IllegalArgumentException.class, () -> service.insertReservation(request));
        }
        verify(mapper, never()).insertReservation(any());
    }

    @Test
    void validContactVariantsAndMissingCalendarDoNotPreventManualReservations() {
        for (String contact : List.of("张老师", "teacher@example.com", "0731-12345678", "13800138000")) {
            var request = reservation(); request.setContact(contact); service.insertReservation(request);
        }
        for (Map<String, Object> config : List.<Map<String, Object>>of(Map.of(), Map.of("semesterStartDate", "bad"), Map.of("semesterStartDate", LocalDate.now().plusYears(2).toString()))) {
            when(timetable.getCrawlerConfig(anyString())).thenReturn(config);
            assertDoesNotThrow(() -> service.insertReservation(reservation()));
        }
    }

    private ReservationsEntity reservation() {
        var value = new ReservationsEntity(); value.setId(1L); value.setApplicantName("张同学");
        value.setDepartment("信息学院"); value.setContact("13800138000"); value.setLabId(7L); value.setLabCode("A101"); value.setLabName("机房");
        value.setReservationDate(LocalDate.now().plusDays(7)); value.setTimeRange("08:30-10:00");
        value.setScene("课程"); value.setAttendeeCount(10); value.setReason("程序设计课程"); value.setStatus("待审核");
        return value;
    }
}
