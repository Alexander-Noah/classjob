package org.example.backend.service;

import org.example.backend.mapper.BusinessLoopReportMapper;
import org.example.backend.service.impl.BusinessLoopReportServiceImpl;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessLoopReportBehaviorTest {
    final BusinessLoopReportMapper mapper=mock(BusinessLoopReportMapper.class);
    final BusinessLoopReportService service=new BusinessLoopReportServiceImpl(mapper,new ObjectMapper());
    @SuppressWarnings("unchecked") List<Map<String,Object>> rows(Map<String,Object> overview,String key){return (List<Map<String,Object>>)overview.get(key);}
    @Test void emptyBusinessHasCompleteRatesAndNoPendingActions(){
        Map<String,Object> result=service.getOverview();assertEquals(6,rows(result,"cards").size());assertTrue(rows(result,"cards").stream().allMatch(card->"100%".equals(card.get("value"))));assertTrue(rows(result,"chains").stream().allMatch(chain->"已闭环".equals(chain.get("status"))));assertEquals(List.of(),result.get("actions"));assertEquals(Map.of("reservations",0L,"usageRecords",0L,"repairs",0L,"devices",0L,"environmentTemplates",0L,"consumables",0L,"notices",0L,"events",0L),result.get("totals"));
    }
    @Test void reportsRealCountsRatesActionsAndRankings(){
        when(mapper.countReservations()).thenReturn(10L);when(mapper.countApprovedReservations()).thenReturn(8L);when(mapper.countPendingReservations()).thenReturn(2L);when(mapper.countUsageRecords()).thenReturn(7L);when(mapper.countUsageRecordsFromReservations()).thenReturn(6L);when(mapper.countAbnormalUsageRecords()).thenReturn(4L);when(mapper.countRepairOrdersFromUsageRecords()).thenReturn(3L);when(mapper.countRepairOrders()).thenReturn(5L);when(mapper.countClosedRepairOrders()).thenReturn(2L);when(mapper.countActiveRepairOrders()).thenReturn(3L);when(mapper.countDevices()).thenReturn(10L);when(mapper.countNormalDevices()).thenReturn(7L);when(mapper.countFaultDevices()).thenReturn(1L);when(mapper.countMaintenanceDevices()).thenReturn(2L);when(mapper.countCourseEnvironmentRequests()).thenReturn(4L);when(mapper.countConfirmedCourseEnvironmentRequests()).thenReturn(2L);when(mapper.countEnvironmentTemplates()).thenReturn(1L);when(mapper.countConsumables()).thenReturn(5L);when(mapper.countLowStockConsumables()).thenReturn(1L);when(mapper.countNotices()).thenReturn(8L);when(mapper.countPublishedNotices()).thenReturn(2L);when(mapper.countOperationEvents()).thenReturn(4L);
        List<Map<String,Object>> trend=List.of(Map.of("day","2026-10-18","count",7));when(mapper.listUsageTrend()).thenReturn(trend);when(mapper.listLatestEventJson(30)).thenReturn(List.of("{\"category\":\"reservation\",\"subject\":\"预约\"}","{\"category\":\"reservation\"}","{}","malformed"));
        Map<String,Object> result=service.getOverview();assertEquals(List.of("75%","75%","40%","70%","50%","25%"),rows(result,"cards").stream().map(card->card.get("value")).toList());assertEquals(List.of(2L,1L,3L,3L,1L),rows(result,"actions").stream().map(action->action.get("count")).toList());assertEquals(Map.of("reservation",2L,"unknown",2L),result.get("eventCategories"));assertEquals("malformed",rows(result,"latestEvents").get(3).get("subject"));assertEquals(trend,result.get("usageTrend"));assertEquals(6,rows(result,"chains").size());assertEquals(8L,rows(result,"chains").get(0).get("total"));assertEquals(6L,rows(result,"chains").get(0).get("completed"));
    }
    @Test void overCompletedAndNegativeCountsCannotCreateInvalidRates(){
        when(mapper.countApprovedReservations()).thenReturn(2L);when(mapper.countUsageRecordsFromReservations()).thenReturn(9L);when(mapper.countDevices()).thenReturn(5L);when(mapper.countNormalDevices()).thenReturn(-1L);when(mapper.countAbnormalUsageRecords()).thenReturn(1L);when(mapper.countRepairOrdersFromUsageRecords()).thenReturn(3L);
        Map<String,Object> result=service.getOverview();assertEquals("100%",rows(result,"cards").get(0).get("value"));assertEquals(2L,rows(result,"cards").get(0).get("completed"));assertEquals("0%",rows(result,"cards").get(3).get("value"));assertEquals(0L,rows(result,"chains").get(1).get("pending"));assertEquals(List.of(),result.get("actions"));
    }
}
