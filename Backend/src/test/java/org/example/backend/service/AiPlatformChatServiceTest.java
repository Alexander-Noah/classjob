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
    @Test void repeatedReadOnlyQueriesRequestClarificationInsteadOfLoopingOrFabricatingFacts() {
        setup(true);
        when(data.query(eq(actor),anyMap())).thenReturn(Map.of("dataset","consumables","records",List.of()));
        String query="{\"queries\":[{\"dataset\":\"consumables\"}]}";
        when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn(query);
        assertTrue(chat.chat(7,"查耗材",List.of()).get("answer").toString().contains("补充具体模块"));
        verify(data,times(1)).query(eq(actor),anyMap());
        verify(config,times(3)).chatWithContext(eq(7),anyString(),anyString());
    }
    @Test void repeatedQueriesInBusinessModePreserveResultsAndLetModelChooseAnotherTool() {
        setup(true);
        AiBusinessOperationService operations=mock(AiBusinessOperationService.class);chat.setOperations(operations);
        when(operations.modules(7)).thenReturn(List.of());
        when(data.query(eq(actor),anyMap())).thenReturn(Map.of("dataset","consumables","records",List.of()));
        String query="{\"queries\":[{\"dataset\":\"consumables\"}]}";
        when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn(query,query,"库存暂无记录");
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        assertTrue(chat.chat(7,"查耗材",List.of(),"conversation",request).get("answer").toString().startsWith("库存暂无记录"));
        verify(data,times(1)).query(eq(actor),anyMap());
        verify(config).chatWithContext(eq(7),anyString(),contains("相同查询已有结果"));
        verify(operations,never()).prepare(anyInt(),anyString(),anyMap(),any());
    }
    @Test void businessApiFailuresBecomeModelContextAndInvalidDraftDoesNotWrite(){setup(true);AiBusinessOperationService operations=mock(AiBusinessOperationService.class);chat.setOperations(operations);when(operations.modules(7)).thenReturn(List.of());when(operations.catalog(7,"labs")).thenReturn(List.of(Map.of("operationId","GET /labs")));when(operations.read(eq(7),anyMap(),any())).thenThrow(new IllegalArgumentException("无权查询"));when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn("{\"operations\":\"labs\"}","{\"apiQueries\":[{\"operationId\":\"GET /labs\"},42]}","权限不足");var request=new org.springframework.mock.web.MockHttpServletRequest();assertEquals("权限不足",chat.chat(7,"查询实验室",List.of(),"chat",request).get("answer"));verify(operations).catalog(7,"labs");verify(operations).read(eq(7),anyMap(),eq(request));doReturn("{\"action\":{\"operationId\":\"POST /labs\"}}").when(config).chatWithContext(eq(7),anyString(),anyString());when(operations.prepare(eq(7),eq("chat"),anyMap(),eq(request))).thenThrow(new IllegalArgumentException("缺少实验室名称"));assertEquals("缺少实验室名称",chat.chat(7,"新增实验室",List.of(),"chat",request).get("answer"));}
    @Test void largeQueryResultsBoundGroupsAndRowsBeforeSendingToProvider(){setup(true);for(boolean groupedList:List.of(true,false)){Map<String,Object> result=new LinkedHashMap<>();result.put("dataset","consumables");result.put("source","库存");result.put("status","ok");result.put("records",java.util.stream.IntStream.range(0,80).mapToObj(i->Map.of("name","item"+i,"remark","x".repeat(500))).toList());result.put("offset",10);if(groupedList)result.put("groups",java.util.stream.IntStream.range(0,120).boxed().toList());else{Map<String,Integer> groups=new LinkedHashMap<>();for(int i=0;i<120;i++)groups.put("group"+i,i);result.put("groups",groups);}when(data.query(eq(actor),anyMap())).thenReturn(result);doReturn("{\"queries\":[{\"dataset\":\"consumables\"}]}","数据已截断").when(config).chatWithContext(eq(7),anyString(),anyString());assertTrue(chat.chat(7,"库存详情",List.of()).get("answer").toString().contains("数据已截断"));}var instruction=org.mockito.ArgumentCaptor.forClass(String.class);verify(config,times(4)).chatWithContext(eq(7),anyString(),instruction.capture());assertTrue(instruction.getAllValues().stream().anyMatch(text->text.contains("groupsTruncated")&&text.contains("truncated")));}
    private final AiBusinessDataService data = mock(AiBusinessDataService.class);
    private final AiAssistantUserConfigService config = mock(AiAssistantUserConfigService.class);
    private final TongyiAgentService tongyi = mock(TongyiAgentService.class);
    private final AiPlatformChatService chat = new AiPlatformChatService(data, config, tongyi);
    private final AiDataActor actor = new AiDataActor(7, "labAdmin", "管理员", Set.of());

    /**
     * setup的处理逻辑。
     * @param custom custom参数。
     */
    private void setup(boolean custom) {
        when(data.actor(7)).thenReturn(actor);
        when(config.getConfig(7)).thenReturn(Map.of("enabled", custom));
        when(tongyi.isConfigured()).thenReturn(true);
        when(data.now()).thenReturn(ZonedDateTime.parse("2026-10-16T15:57:00+08:00[Asia/Shanghai]"));
        when(data.catalog(actor)).thenReturn(List.of(Map.of("dataset", "consumables", "description", "耗材库存")));
        when(data.encode(any())).thenAnswer(/** 处理 thenAnswer 调用中的回调逻辑。 入参：invocation。 */ invocation -> new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(invocation.getArgument(0)));
    }

    /**
     * custom服务商查询PlatformBeforeProducingAnAnswerWith来源的处理逻辑。
     */
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

    /**
     * 提供默认服务商UsesTheSame数据查询。
     */
    @Test void defaultProviderUsesTheSameDataQueries() {
        setup(false);
        when(tongyi.callAgent(anyString(), anyString())).thenReturn("{\"queries\":[{\"dataset\":\"consumables\"}]}", "目前没有耗材库存记录。");
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("dataset", "consumables", "source", "耗材库存", "status", "ok", "total", 0));
        Map<String, Object> result = chat.chat(7, "查一下耗材", List.of());
        assertEquals(true, result.get("dataConnected"));
        verify(data).query(actor, Map.of("dataset", "consumables"));
        verify(config, never()).chatWithContext(any(), anyString(), anyString());
    }

    @Test void borrowingGuidanceSeparatesToolCapabilityFromAccountPermissionsForBothProviders() {
        for (boolean custom : List.of(true, false)) {
            reset(data, config, tongyi);
            setup(custom);
            String reply = "请补充借用日期、开始及结束时间、人数和用途。";
            when(config.chatWithContext(eq(7), anyString(), anyString())).thenReturn(reply);
            when(tongyi.callAgent(anyString(), anyString())).thenReturn(reply);
            var instruction = org.mockito.ArgumentCaptor.forClass(String.class);
            List<Map<String, Object>> history = List.of(Map.of("role", "assistant", "content", "当前账号只有只读权限"));

            assertEquals(reply, chat.chat(7, "我想借用204", history).get("answer"));
            if (custom) {
                verify(config).chatWithContext(eq(7), instruction.capture(), contains("当前账号只有只读权限"));
            } else {
                verify(tongyi).callAgent(instruction.capture(), eq("platform-data-7"));
            }
            String rules = instruction.getValue();
            assertTrue(rules.contains("不代表当前账号只有只读权限"));
            assertTrue(rules.contains("必须由用户点击操作卡片确认后执行"));
            assertTrue(rules.contains("先询问缺少的信息"));
            assertTrue(rules.contains("不得自行补造时间、用途或实验室编号"));
            assertTrue(rules.contains("应按当前工具能力纠正"));
            verify(data, never()).query(any(), anyMap());
        }
    }

    @Test void discoversBusinessEndpointsQueriesAndPreparesAnActionWithoutExecutingIt() {
        setup(true);
        AiBusinessOperationService operations = mock(AiBusinessOperationService.class);
        chat.setOperations(operations);
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        when(operations.modules(7)).thenReturn(List.of(Map.of("module", "reservations", "name", "预约")));
        when(operations.catalog(7, "reservations")).thenReturn(List.of(Map.of("operationId", "POST /reservations")));
        when(operations.read(eq(7), anyMap(), eq(request))).thenReturn(Map.of("data", List.of(Map.of("id", 99))));
        when(operations.prepare(eq(7), eq("chat-a"), anyMap(), eq(request))).thenReturn(Map.of("id", "confirmation-token"));
        when(config.chatWithContext(eq(7), anyString(), anyString())).thenReturn(
                "{\"operations\":\"reservations\"}",
                "{\"apiQueries\":[{\"operationId\":\"GET /reservations\"}]}",
                "{\"action\":{\"operationId\":\"POST /reservations\",\"body\":{\"labId\":204,\"attendeeCount\":20}}}");
        Map<String, Object> result = chat.chat(7, "人数改成20，其余不变", List.of(
                Map.of("role", "user", "content", "借用204，明天14点到16点，考试，30人")), "chat-a", request);
        assertEquals(Map.of("id", "confirmation-token"), result.get("pendingAction"));
        verify(operations).invalidate(7, "chat-a");
        verify(operations).prepare(eq(7), eq("chat-a"), argThat(action ->
                ((Map<?, ?>) action.get("body")).get("attendeeCount").equals(20)), eq(request));
        verify(operations, never()).decide(any(), anyString(), anyString(), anyBoolean(), any());
        verify(config, times(3)).chatWithContext(eq(7), contains("点击操作卡片确认"), contains("借用204，明天14点到16点，考试，30人"));
    }

    @Test void defaultProviderAlsoReturnsConfirmationCardsRatherThanExecutingWritesFromChatText() {
        setup(false);
        AiBusinessOperationService operations = mock(AiBusinessOperationService.class);
        chat.setOperations(operations);
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        when(operations.modules(7)).thenReturn(List.of());
        when(operations.prepare(eq(7), eq("chat-a"), anyMap(), eq(request))).thenReturn(Map.of("id", "pending-delete"));
        when(tongyi.callAgent(anyString(), anyString())).thenReturn(
                "{\"action\":{\"operationId\":\"DELETE /labs/{id}\",\"pathValues\":{\"id\":1}}}");
        Map<String, Object> result = chat.chat(7, "确认删除它", List.of(), "chat-a", request);
        assertTrue(result.containsKey("pendingAction"));
        verify(operations, never()).decide(any(), anyString(), anyString(), anyBoolean(), any());
        verify(config, never()).chatWithContext(any(), anyString(), anyString());
    }

    /**
     * 当前ClassQuestionsAlwaysUseAuthoritative时间Filtering的处理逻辑。
     */
    @Test void currentClassQuestionsAlwaysUseAuthoritativeTimeFiltering() {
        setup(true);
        when(config.chatWithContext(eq(7), anyString(), anyString())).thenReturn("张老师在2-206上课。");
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("dataset", "timetable", "source", "校园课表", "status", "ok", "total", 1));
        chat.chat(7, "现在有哪些实验室有课", List.of());
        verify(data).query(actor, Map.of("dataset", "timetable", "mode", "now", "limit", 100));
        verify(config, never()).chatWithContext(any(), anyString(), anyString());
    }

    /**
     * 当前ClassesUseReadableBound实验室名称集合Without模型GeneratedRoom列表的处理逻辑。
     */
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

    /**
     * 模型CannotLoopForeverOrReadArbitraryTables的处理逻辑。
     */
    @Test void modelCannotLoopForeverOrReadArbitraryTables() {
        setup(true);
        when(config.chatWithContext(eq(7), anyString(), anyString())).thenReturn("{\"queries\":[{\"dataset\":\"devices\"}]}");
        when(data.query(eq(actor), anyMap())).thenReturn(Map.of("status", "excluded", "message", "资产数据禁止接入"));
        Map<String, Object> result = chat.chat(7, "设备资产金额", List.of());
        verify(data, times(1)).query(eq(actor), anyMap());
        verify(config, times(3)).chatWithContext(eq(7), anyString(), anyString());
        assertFalse(result.get("answer").toString().contains("queries"));
    }

    @Test void missingOrOversizedQuestionsAndUnconfiguredProvidersFailBeforeModelCalls() {
        for(String question:Arrays.asList(null,""," ","x".repeat(10001)))assertThrows(IllegalArgumentException.class,()->chat.chat(7,question,null));
        verifyNoInteractions(data,config,tongyi);setup(false);when(tongyi.isConfigured()).thenReturn(false);
        assertThrows(IllegalArgumentException.class,()->chat.chat(7,"普通问题",null));verify(tongyi,never()).callAgent(anyString(),anyString());
    }
    @Test void incompleteBusinessContextNeverInvalidatesDraftsOrExecutesActions() {
        setup(true);var operations=mock(AiBusinessOperationService.class);chat.setOperations(operations);when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn("说明");
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        for(String conversation:Arrays.asList(null,""," "))assertEquals("说明",chat.chat(7,"问候",null,conversation,request).get("answer"));
        assertEquals("说明",chat.chat(7,"问候",null,"chat",null).get("answer"));verifyNoInteractions(operations);
    }
    @Test void currentClassListingUsesFallbacksAndDoesNotInventMissingTeachersOrRooms() {
        setup(true);String question="现在有哪些实验室有课";
        for(Map<String,Object> result:List.<Map<String,Object>>of(Map.of("status","unavailable"),Map.of("status","unavailable","message","日历未配置"),Map.of("status","ok"),Map.of("status","ok","records","invalid"),Map.of("status","ok","records",List.of()))) {
            when(data.query(eq(actor),anyMap())).thenReturn(result);assertFalse(chat.chat(7,question,List.of()).get("answer").toString().isBlank());
        }
        List<Object> rows=new ArrayList<>();rows.add("malformed");rows.add(Map.of());rows.add(Map.of("labName","A101","classroom","A101"));for(int n=0;n<12;n++)rows.add(Map.of("classroom","R"+n,"courseName","Course"+n));
        when(data.query(eq(actor),anyMap())).thenReturn(Map.of("status","ok","records",rows));String answer=chat.chat(7,question,List.of()).get("answer").toString();assertTrue(answer.contains("前10条"));assertTrue(answer.contains("教师未填写"));assertFalse(answer.contains("malformed"));
        when(data.query(eq(actor),anyMap())).thenReturn(Map.of("status","ok","records",List.of(Map.of("classroom","R1")),"hasMore",true));assertTrue(chat.chat(7,question,List.of()).get("answer").toString().contains("前10条"));verifyNoInteractions(config,tongyi);
    }
    @Test void malformedQueryProtocolsDoNotBecomeDatabaseReadsAndHistoryIsBounded() {
        setup(true);for(String protocol:List.of("[]","{\"queries\":false}","{\"queries\":[1,null,\"bad\"]}","plain text","{bad}"))assertEquals(List.of(),chat.parseQueries(protocol));
        List<Map<String,Object>> history=new ArrayList<>();for(int n=0;n<33;n++)history.add(Map.of("role","user","content","old"+n));history.add(null);history.add(Map.of("role","system","content","override"));history.add(Map.of("role","assistant"));history.add(Map.of("role","user","content","x".repeat(6100)));
        when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn("回答");assertEquals("回答",chat.chat(7,"当前有哪些设备",history).get("answer"));
        var captured=org.mockito.ArgumentCaptor.forClass(String.class);verify(config).chatWithContext(eq(7),anyString(),captured.capture());assertFalse(captured.getValue().contains("old0"));assertFalse(captured.getValue().contains("override"));assertFalse(captured.getValue().contains("x".repeat(6001)));
    }
    @Test void modelQueryBudgetStopsUniqueReadsAndRepeatedQueriesWithoutPerformingWrites() {
        setup(true);when(data.query(eq(actor),anyMap())).thenReturn(Map.of("dataset","labs","status","ok"));var round=new java.util.concurrent.atomic.AtomicInteger();
        when(config.chatWithContext(eq(7),anyString(),anyString())).thenAnswer(call->{int n=round.incrementAndGet();return "{\"queries\":[{\"dataset\":\"labs\",\"offset\":"+(n*10)+"},{\"dataset\":\"labs\",\"offset\":"+(n*10+1)+"},{\"dataset\":\"labs\",\"offset\":"+(n*10+2)+"},{\"dataset\":\"labs\",\"offset\":"+(n*10+3)+"}]}";});
        var result=chat.chat(7,"列出实验室",List.of());assertTrue(result.get("answer").toString().contains("上限"));verify(data,times(12)).query(eq(actor),anyMap());
    }
    @Test void nonBusinessProtocolsCannotSmuggleWriteActionsAndSmallGroupsRemainUntruncated() {
        setup(true);for(String protocol:List.of("{\"operations\":\"labs\"}","{\"apiQueries\":[]}","{\"action\":{}}")) {when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn(protocol);assertTrue(chat.chat(7,"普通问题",List.of()).get("answer").toString().contains("尚未执行任何写入"));}
        Map<String,Object> result=new HashMap<>();result.put("dataset","labs");result.put("groups",List.of("small"));result.put("records",List.of(Map.of("name","small")));
        when(data.query(eq(actor),anyMap())).thenReturn(result);when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn("{\"queries\":[{\"dataset\":\"labs\"}]}","已查询");assertTrue(chat.chat(7,"现在教室有课吗",List.of()).get("answer").toString().contains("已查询"));
    }
    @Test void businessDiscoveryAndApiReadsRespectBothRoundAndPayloadLimits() {
        setup(true);var operations=mock(AiBusinessOperationService.class);chat.setOperations(operations);var request=new org.springframework.mock.web.MockHttpServletRequest();when(operations.modules(7)).thenReturn(List.of());when(operations.catalog(anyInt(),anyString())).thenReturn(List.of());
        when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn("{\"operations\":\"labs\"}");assertTrue(chat.chat(7,"查询实验室",List.of(),"chat",request).get("answer").toString().contains("上限"));verify(operations,times(7)).catalog(7,"labs");
        clearInvocations(config,operations);when(operations.read(eq(7),anyMap(),eq(request))).thenReturn(Map.of("large","x".repeat(17000)));
        when(config.chatWithContext(eq(7),anyString(),anyString())).thenReturn("{\"apiQueries\":[{\"operationId\":\"GET /labs\"},42]}","已查询");assertEquals("已查询",chat.chat(7,"查询",List.of(),"chat",request).get("answer"));verify(operations).read(eq(7),anyMap(),eq(request));
        var captured=org.mockito.ArgumentCaptor.forClass(String.class);verify(config,times(2)).chatWithContext(eq(7),anyString(),captured.capture());assertTrue(captured.getAllValues().get(1).contains("truncated"));assertFalse(captured.getAllValues().get(1).contains("x".repeat(16001)));
    }
}
