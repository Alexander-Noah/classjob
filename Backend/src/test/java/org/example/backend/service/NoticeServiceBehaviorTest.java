package org.example.backend.service;

import org.example.backend.entity.NoticeEntity;
import org.example.backend.entity.NoticeRecipientEntity;
import org.example.backend.mapper.NoticeMapper;
import org.example.backend.service.impl.NoticeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NoticeServiceBehaviorTest {
    private final NoticeMapper mapper = mock(NoticeMapper.class);
    private final BusinessLoopService events = mock(BusinessLoopService.class);
    private final NoticeServiceImpl service = new NoticeServiceImpl(mapper, events);
    private NoticeEntity saved;

    @BeforeEach
    void configurePersistence() {
        saved = notice(1L, "已发布", "labAdmin");
        when(mapper.getNotice(anyLong())).thenReturn(saved);
        when(mapper.listNotices()).thenReturn(List.of(saved));
        doAnswer(call -> { NoticeEntity value = call.getArgument(0); value.setId(1L); return 1; })
                .when(mapper).insertNotice(any());
    }

    @Test
    void createNormalizesEmptyDraftWithoutPublishingRecipients() {
        saved.setStatus("草稿");
        saved.setPublishStatus("草稿");
        assertSame(saved, service.createNotice(null));
        ArgumentCaptor<NoticeEntity> captor = ArgumentCaptor.forClass(NoticeEntity.class);
        verify(mapper).insertNotice(captor.capture());
        NoticeEntity draft = captor.getValue();
        assertAll(
                () -> assertEquals("系统提醒", draft.getTitle()),
                () -> assertEquals("系统提醒", draft.getType()),
                () -> assertEquals("系统提醒", draft.getNoticeType()),
                () -> assertEquals("系统提醒", draft.getContent()),
                () -> assertEquals("普通", draft.getPriority()),
                () -> assertEquals("草稿", draft.getStatus()),
                () -> assertEquals("草稿", draft.getPublishStatus()),
                () -> assertEquals("labAdmin", draft.getTargetRole()),
                () -> assertNull(draft.getPublishTime()));
        verify(mapper, never()).insertRecipientsForRole(any(), any());
        verify(events).recordEvent(eq("notice"), eq("create"), anyString(), eq("草稿"), anyMap());
    }

    @Test
    void updatePublishedNoticeNormalizesAliasesAndRefreshesRecipients() {
        NoticeEntity input = new NoticeEntity();
        input.setTitle("课程通知");
        input.setNoticeType("课程");
        input.setTarget("全部用户");
        input.setPublishStatus("已发布");
        input.setContent("正文");
        saved.setTargetRole("全部用户");
        assertSame(saved, service.updateNotice(1L, input));
        assertEquals("课程", input.getType());
        assertEquals("all", input.getTargetRole());
        assertEquals("all", input.getTarget());
        assertEquals("已发布", input.getStatus());
        assertNotNull(input.getPublishTime());
        verify(mapper).updateNotice(1L, input);
        verify(mapper).deleteRecipientsByNotice(1L);
        verify(mapper).insertRecipientsForRole(1L, "all");
    }

    @Test
    void publishWithdrawArchiveAndDeleteRecordTheirBusinessEvents() {
        assertSame(saved, service.publishNotice(1L));
        verify(mapper).publishNotice(1L);
        assertSame(saved, service.withdrawNotice(1L));
        verify(mapper).updateNoticeStatus(1L, "已撤回");
        assertSame(saved, service.archiveNotice(1L));
        verify(mapper).archiveNotice(1L);
        assertEquals(List.of(saved), service.deleteNotice(1L));
        verify(mapper).logicalDeleteNotice(1L);
        for (String action : List.of("publish", "withdraw", "archive", "delete")) {
            verify(events).recordEvent(eq("notice"), eq(action), anyString(), anyString(), anyMap());
        }
        when(mapper.getNotice(2L)).thenReturn(null);
        service.deleteNotice(2L);
        verify(events).recordEvent("notice", "delete", "2", "已删除", Map.of("id", 2L));
    }

    @Test
    void replacementClearsRecipientsBeforeNoticesAndSupportsNullInput() {
        assertEquals(List.of(saved), service.replaceNotices(null));
        var order = inOrder(mapper);
        order.verify(mapper).deleteAllRecipients();
        order.verify(mapper).deleteAllNotices();
        verify(mapper, never()).insertNotice(any());
        service.replaceNotices(List.of(notice(null, "已发布", "教师")));
        verify(mapper).insertNotice(any());
        verify(mapper).insertRecipientsForRole(1L, "labAdmin");
    }

    @Test
    void resetCreatesPublishedAndDraftDefaults() {
        service.resetNotices();
        ArgumentCaptor<NoticeEntity> captor = ArgumentCaptor.forClass(NoticeEntity.class);
        verify(mapper, times(3)).insertNotice(captor.capture());
        assertEquals(2, captor.getAllValues().stream().filter(value -> "已发布".equals(value.getStatus())).count());
        assertTrue(captor.getAllValues().stream().allMatch(value -> value.getTitle() != null && value.getContent() != null));
        verify(events).recordEvent(eq("notice"), eq("reset"), anyString(), anyString(), eq(Map.of("count", 3)));
    }

    @Test
    void inboxSynchronizesRolesAndMarkReadReturnsOnlyTheRequestedNotice() {
        NoticeRecipientEntity other = new NoticeRecipientEntity();
        other.setNoticeId(2L);
        NoticeRecipientEntity requested = new NoticeRecipientEntity();
        requested.setNoticeId(1L);
        when(mapper.listUserNotices(7, null)).thenReturn(List.of(other, requested));
        when(mapper.getNoticeStats(7)).thenReturn(Map.of("unread", 2));
        assertSame(requested, service.markRead(1L, 7, "系统管理员"));
        verify(mapper).syncUserRecipients(7, "systemAdmin");
        verify(mapper).markRead(1L, 7);
        assertNull(service.markRead(99L, 7, "teacher"));
        assertEquals(Map.of("unread", 2), service.getNoticeStats(7, "labAdmin"));
        when(mapper.listRecipients(1L)).thenReturn(List.of(requested));
        assertEquals(List.of(requested), service.listRecipients(1L));
        service.listUserNotices(null, "labAdmin", "未读");
        verify(mapper, never()).syncUserRecipients(isNull(), any());
    }

    @Test
    void targetRoleAliasesAndCustomValuesArePreservedInInboxSync() {
        String[][] cases = {{"全部", "all"}, {"平台管理员", "systemAdmin"}, {"任课教师", "teacher"},
                {"维修人员", "maintenance"}, {"实验室管理员", "labAdmin"}, {"custom", "custom"}, {"", "labAdmin"}};
        for (String[] item : cases) {
            service.listUserNotices(7, item[0], "未读");
            verify(mapper, atLeastOnce()).syncUserRecipients(7, item[1]);
        }
    }

    @Test
    void businessReminderSyncSkipsDuplicateSourcesAndBuildsAllReminderTypes() {
        when(mapper.listLowStockConsumables()).thenReturn(List.of(Map.of("id", 1, "name", "纸", "stock", 1, "warnThreshold", 5)));
        when(mapper.listPendingReservations()).thenReturn(List.of(Map.of("id", 2, "status", "待审批")));
        when(mapper.listActiveRepairs()).thenReturn(List.of(Map.of("id", 3, "status", "待处理")));
        when(mapper.countNoticeBySource("reservation", "2")).thenReturn(1);
        assertEquals(Map.of("created", 2), service.syncBusinessReminders());
        ArgumentCaptor<NoticeEntity> captor = ArgumentCaptor.forClass(NoticeEntity.class);
        verify(mapper, times(2)).insertNotice(captor.capture());
        var notices = captor.getAllValues();
        assertEquals(List.of("consumable", "repair"), notices.stream().map(NoticeEntity::getSourceModule).toList());
        assertTrue(notices.get(0).getContent().contains("1件"));
        assertEquals("maintenance", notices.get(1).getTargetRole());
        assertTrue(notices.stream().allMatch(value -> "已发布".equals(value.getPublishStatus())));
        assertEquals(List.of(saved), service.listNotices());
    }

    private NoticeEntity notice(Long id, String status, String target) {
        NoticeEntity value = new NoticeEntity();
        value.setId(id);
        value.setTitle("通知");
        value.setStatus(status);
        value.setPublishStatus(status);
        value.setTargetRole(target);
        return value;
    }
}
