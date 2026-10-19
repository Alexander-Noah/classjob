package org.example.backend.service;

import org.example.backend.entity.LabEntity;
import org.example.backend.mapper.DashboardMapper;
import org.example.backend.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardBehaviorTest {
    final DashboardMapper mapper=mock(DashboardMapper.class);
    final LabService labs=mock(LabService.class);
    final BusinessLoopReportService loops=mock(BusinessLoopReportService.class);
    final DashboardService service=new DashboardServiceImpl(mapper,loops,labs);
    LabEntity lab(int id,String code,String name,String room,String status){LabEntity lab=new LabEntity();lab.setId(id);lab.setLabCode(code);lab.setLabName(name);lab.setRoomNo(room);lab.setOpenStatus(status);return lab;}
    @SuppressWarnings("unchecked") List<Map<String,Object>> rows(Map<String,Object> overview,String key){return (List<Map<String,Object>>)overview.get(key);}
    @Test void aggregatesScopedLabsWithActiveUsageAndDistinctCourseCounts(){
        when(labs.getLabs(7)).thenReturn(List.of(lab(1,"A101","机房一","101","开放"),lab(2,"B202","机房二","202","维护中"),lab(3,"C303","机房三","303",null)));
        when(mapper.listActiveLabIds(7)).thenReturn(List.of(1L,2L));
        when(mapper.listTodaySchedules(7)).thenReturn(List.of(Map.of("labId",1L,"time","10:00-11:00","course","数据库"),Map.of("labId","1","time","08:00-09:00","course","程序设计"),Map.of("lab","outside","time","09:00-10:00","course","外部")));
        when(mapper.countDevices(7)).thenReturn(20L);when(mapper.countOnlineDevices(7)).thenReturn(18L);when(mapper.countAbnormalDevices(7)).thenReturn(2L);when(mapper.countPendingReservations(7)).thenReturn(3L);when(mapper.countTodayReservations(7)).thenReturn(5L);when(mapper.countApprovedTodayReservations(7)).thenReturn(4L);when(mapper.countActiveRepairs(7)).thenReturn(2L);when(mapper.countHighPriorityRepairs(7)).thenReturn(1L);
        when(loops.getOverview()).thenReturn(Map.of("healthy",true));
        Map<String,Object> result=service.getOverview(7);List<Map<String,Object>> stats=rows(result,"stats");
        assertEquals(List.of(3L,1L,1L,4L,5L,20L),stats.stream().map(row->row.get("value")).toList());assertEquals("开放 1 间",stats.get(0).get("trend"));
        assertEquals(List.of(88,35,12),rows(result,"labUsage").stream().map(row->row.get("value")).toList());
        List<Map<String,Object>> schedules=rows(result,"todaySchedules");assertEquals(4,schedules.size());assertEquals("程序设计",schedules.get(0).get("course"));assertEquals("A101",schedules.get(0).get("lab"));assertEquals("使用中",schedules.get(2).get("status"));assertEquals("开放",schedules.get(3).get("status"));assertEquals(Map.of("healthy",true),result.get("businessLoop"));assertEquals(List.of(),result.get("loadWarnings"));assertEquals(6,rows(result,"shortcuts").size());
        verify(mapper).listDeviceStatus(7);verify(mapper).listTodoItems(7);verify(labs).getLabs(7);
    }
    @Test void matchesScheduleAliasesAndUsesLabDisplayFallbacks(){
        when(labs.getLabs(null)).thenReturn(List.of(lab(1,"A101",null,null,"开放"),lab(2,null,"第二机房",null,"预约中"),lab(3,null,null,"C303","停用"),lab(4,null,null,null,"其他"),lab(5,"E505",null,null,"维护中")));
        when(mapper.listTodaySchedules(null)).thenReturn(List.of(Map.of("labId","bad","classroom","楼宇 a 101","course","A"),Map.of("lab"," 第二机房 ","course","B"),Map.of("lab","c303","course","C")));
        Map<String,Object> result=service.getOverview(null);List<Map<String,Object>> schedules=rows(result,"todaySchedules");assertEquals(5,schedules.size());assertEquals(List.of("A101","第二机房","C303","LAB-4","E505"),schedules.stream().map(row->row.get("lab")).toList());assertEquals("primary",schedules.get(3).get("statusType"));assertEquals("warning",schedules.get(4).get("statusType"));assertEquals(3L,rows(result,"stats").get(2).get("value"));
    }
    @Test void retainsUsableEmptySectionsForNullDependencyResults(){
        when(labs.getLabs(null)).thenReturn(null);when(mapper.listActiveLabIds(null)).thenReturn(null);when(mapper.listTodaySchedules(null)).thenReturn(null);when(mapper.listNotices()).thenReturn(null);when(loops.getOverview()).thenReturn(null);
        Map<String,Object> result=service.getOverview(null);assertEquals(List.of(),result.get("todaySchedules"));assertEquals(List.of(),result.get("notices"));assertEquals(Map.of(),result.get("businessLoop"));assertEquals(List.of(),result.get("loadWarnings"));assertTrue(rows(result,"stats").stream().allMatch(row->Long.valueOf(0).equals(row.get("value"))));
    }
    @Test void isolatesFailedSectionsAndNamesEachWarning(){
        when(labs.getLabs(7)).thenThrow(new IllegalStateException("labs offline"));when(mapper.listActiveLabIds(7)).thenThrow(new IllegalStateException("usage offline"));when(mapper.listTodaySchedules(7)).thenThrow(new IllegalStateException("schedule offline"));when(mapper.countDevices(7)).thenThrow(new IllegalStateException("device offline"));when(mapper.listNotices()).thenThrow(new IllegalStateException("notice offline"));when(loops.getOverview()).thenThrow(new IllegalStateException("loop offline"));
        Map<String,Object> result=service.getOverview(7);assertEquals(List.of("实验室资源","当前使用实验室","今日安排","设备总数","通知公告","业务闭环"),result.get("loadWarnings"));assertEquals(List.of(),result.get("notices"));assertEquals(Map.of(),result.get("businessLoop"));assertEquals(0L,rows(result,"stats").get(5).get("value"));
    }
    @Test void statusTagsCoverOpenOccupiedReservedAndDisabledLabs(){
        List<String> statuses=List.of("开放","使用中","占用中","预约中","待预约","维护中","停用");List<LabEntity> source=new ArrayList<>();for(int index=0;index<statuses.size();index++)source.add(lab(index+1,"L"+index,null,null,statuses.get(index)));when(labs.getLabs(null)).thenReturn(source);
        assertEquals(List.of("success","success","success","warning","warning","warning","info"),rows(service.getOverview(null),"todaySchedules").stream().map(row->row.get("statusType")).toList());
    }
}
