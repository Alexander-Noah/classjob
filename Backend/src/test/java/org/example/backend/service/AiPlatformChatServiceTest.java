/*
 * AI 平台问答回归测试；测试夹具与生产配置分离。
 * 覆盖场景：customProviderQueriesPlatformBeforeProducingAnAnswerWithSources、defaultProviderUsesTheSameDataQueries、currentClassQuestionsAlwaysUseAuthoritativeTimeFiltering、currentClassesUseReadableBoundLabNamesWithoutModelGeneratedRoomLists 等。
 * 主要协作组件：AiBusinessDataService、AiAssistantUserConfigService、TongyiAgentService、AiPlatformChatService。
 */
package org.example.backend.service;

import org.junit.jupiter.api.Test;
import java.time.ZonedDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiPlatformChatServiceTest {
    private final AiBusinessDataService data = mock(AiBusinessDataService.class);
    private final AiAssistantUserConfigService config = mock(AiAssistantUserConfigService.class);
    private final TongyiAgentService tongyi = mock(TongyiAgentService.class);
    private final AiPlatformChatService chat = new AiPlatformChatService(data, config, tongyi);
    private final AiDataActor actor = new AiDataActor(7, "labAdmin", "管理员", Set.of());

    private void setup(boolean custom) {
        when(data.actor(7)).thenReturn(actor);
        when(config.getConfig(7)).thenReturn(Map.of("enabled", custom));
        when(tongyi.isConfigured()).thenReturn(true);
        when(data.now()).thenReturn(ZonedDateTime.parse("2026-10-16T15:57:00+08:00[Asia/Shanghai]"));
        when(data.catalog(actor)).thenReturn(List.of(Map.of("dataset", "consumables", "description", "耗材库存")));
        when(data.encode(any())).thenAnswer(invocation -> new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(invocation.getArgument(0)));
    }

    @Test void customProviderQueriesPlatformBeforeProducingAnAnswerWithSources() {
        setup(true);
        when(config.chatWithContext(eq(7), anyString(), anyString()))
                .thenReturn("{\"queries\":[{\"dataset\":\"consumables\",\"keyword\":\"网线\"}]}", "网线库存为86根。");
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("dataset", "consumables", "source", "耗材库存",
                "queriedAt", "2026-10-16T15:57", "status", "ok", "records", List.of(Map.of("name", "网线", "stock", 86))));
        Map<String, Object> result = chat.chat(7, "网线还有多少", List.of());
        assertTrue(result.get("answer").toString().contains("86根"));
        assertTrue(result.get("answer").toString().contains("来源：耗材库存"));
        verify(data).query(actor, Map.of("dataset", "consumables", "keyword", "网线"));
        verify(config, times(2)).chatWithContext(eq(7), anyString(), anyString());
        verifyNoInteractions(tongyi);
    }

    @Test void defaultProviderUsesTheSameDataQueries() {
        setup(false);
        when(tongyi.callAgent(anyString(), anyString())).thenReturn("{\"queries\":[{\"dataset\":\"consumables\"}]}", "目前没有耗材库存记录。");
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("dataset", "consumables", "source", "耗材库存", "status", "ok", "total", 0));
        Map<String, Object> result = chat.chat(7, "查一下耗材", List.of());
        assertEquals(true, result.get("dataConnected"));
        verify(data).query(actor, Map.of("dataset", "consumables"));
        verify(config, never()).chatWithContext(any(), anyString(), anyString());
    }

    @Test void currentClassQuestionsAlwaysUseAuthoritativeTimeFiltering() {
        setup(true);
        when(config.chatWithContext(eq(7), anyString(), anyString())).thenReturn("张老师在2-206上课。");
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("dataset", "timetable", "source", "校园课表", "status", "ok", "total", 1));
        chat.chat(7, "现在有哪些实验室有课", List.of());
        verify(data).query(actor, Map.of("dataset", "timetable", "mode", "now", "limit", 100));
        verify(config, never()).chatWithContext(any(), anyString(), anyString());
    }

    @Test void currentClassesUseReadableBoundLabNamesWithoutModelGeneratedRoomLists() {
        setup(true);
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("dataset", "timetable", "status", "ok", "total", 1,
                "records", List.of(Map.of("labName", "移动互联实训室", "classroom", "2-206", "courseName", "程序设计",
                        "teacher", "张老师", "timeRange", "15:50—17:20"))));
        String answer = chat.chat(7, "现在有哪些实验室有课", List.of()).get("answer").toString();
        assertTrue(answer.contains("1. 移动互联实训室（2-206）：程序设计，张老师，15:50—17:20。"));
        assertFalse(answer.contains("|")); assertFalse(answer.contains("weekExpanded"));
        verifyNoInteractions(config, tongyi);
    }

    @Test void modelCannotLoopForeverOrReadArbitraryTables() {
        setup(true);
        when(config.chatWithContext(eq(7), anyString(), anyString())).thenReturn("{\"queries\":[{\"dataset\":\"devices\"}]}");
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("status", "excluded", "message", "资产数据禁止接入"));
        Map<String, Object> result = chat.chat(7, "设备资产金额", List.of());
        verify(data, times(1)).query(eq(actor), anyMap());
        verify(config, times(3)).chatWithContext(eq(7), anyString(), anyString());
        assertFalse(result.get("answer").toString().contains("queries"));
    }
}
