package org.example.backend.teachingschedule.adapter.in.web;

import org.example.backend.teachingschedule.api.ScheduleEntryView;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.api.ScheduleSummaryView;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeachingScheduleQueryControllerTest {
    private TeachingScheduleQuery query;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        query = mock(TeachingScheduleQuery.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TeachingScheduleQueryController(query)).build();
    }

    @Test
    void exposesTheCompatibleSearchRouteAndFields() throws Exception {
        when(query.search(any())).thenReturn(List.of(scheduleEntry()));

        mockMvc.perform(get("/class-timetables")
                        .param("semester", " 2026-Spring ")
                        .param("teacher", " Alice ")
                        .param("className", "Class 1")
                        .param("classroom", "Lab 201")
                        .param("courseName", "Networks")
                        .param("keyword", "net")
                        .param("week", "3")
                        .param("limit", "8000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].semester").value("2026-Spring"))
                .andExpect(jsonPath("$.data[0].rowClassName").value("Class 1 (row)"))
                .andExpect(jsonPath("$.data[0].className").value("Class 1"))
                .andExpect(jsonPath("$.data[0].weekday").value("Monday"))
                .andExpect(jsonPath("$.data[0].sectionCode").value("1-2"))
                .andExpect(jsonPath("$.data[0].sectionText").value("第1-2节"))
                .andExpect(jsonPath("$.data[0].startSection").value(1))
                .andExpect(jsonPath("$.data[0].endSection").value(2))
                .andExpect(jsonPath("$.data[0].courseName").value("Networks"))
                .andExpect(jsonPath("$.data[0].teacher").value("Alice"))
                .andExpect(jsonPath("$.data[0].weekRaw").value("1-16"))
                .andExpect(jsonPath("$.data[0].weekText").value("第1-16周"))
                .andExpect(jsonPath("$.data[0].weekExpanded").value("1,2,3"))
                .andExpect(jsonPath("$.data[0].classroom").value("Lab 201"))
                .andExpect(jsonPath("$.data[0].createTime").value("2026-02-23T08:00:00"));

        ArgumentCaptor<ScheduleSearchCriteria> criteria = ArgumentCaptor.forClass(ScheduleSearchCriteria.class);
        verify(query).search(criteria.capture());
        assertEquals(" 2026-Spring ", criteria.getValue().semester());
        assertEquals(" Alice ", criteria.getValue().teacher());
        assertEquals("Class 1", criteria.getValue().className());
        assertEquals("Lab 201", criteria.getValue().classroom());
        assertEquals("Networks", criteria.getValue().courseName());
        assertEquals("net", criteria.getValue().keyword());
        assertEquals(3, criteria.getValue().week());
        assertEquals(8000, criteria.getValue().limit());
    }

    @Test
    void exposesCompatibleSummaryAndSemesterRoutes() throws Exception {
        when(query.summary()).thenReturn(new ScheduleSummaryView(
                1, 1, 1, 1, LocalDateTime.of(2026, 2, 23, 8, 0), List.of("2026-Spring")
        ));
        when(query.listSemesters()).thenReturn(List.of("2026-Spring"));

        mockMvc.perform(get("/class-timetables/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.classCount").value(1))
                .andExpect(jsonPath("$.data.teacherCount").value(1))
                .andExpect(jsonPath("$.data.classroomCount").value(1))
                .andExpect(jsonPath("$.data.latestSyncAt").value("2026-02-23T08:00:00"))
                .andExpect(jsonPath("$.data.semesters[0]").value("2026-Spring"));

        mockMvc.perform(get("/class-timetables/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("2026-Spring"));
    }

    @Test
    void doesNotExposeCrawlerRoutes() throws Exception {
        mockMvc.perform(post("/class-timetables/crawl"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/class-timetables/crawler-config"))
                .andExpect(status().isNotFound());
    }

    private ScheduleEntryView scheduleEntry() {
        return new ScheduleEntryView(
                7L,
                "2026-Spring",
                "Class 1 (row)",
                "Class 1",
                "Monday",
                "1-2",
                "第1-2节",
                1,
                2,
                "Networks",
                "Alice",
                "1-16",
                "第1-16周",
                "1,2,3",
                "Lab 201",
                LocalDateTime.of(2026, 2, 23, 8, 0)
        );
    }
}
