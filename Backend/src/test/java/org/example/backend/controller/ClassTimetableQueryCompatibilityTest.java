/*
 * 全校课表采集回归测试；测试夹具与生产配置分离。
 * 覆盖场景：preservesTimetableSearchContract、preservesSummaryAndSemesterContracts。
 */
package org.example.backend.controller;

import org.example.backend.controller.TeachingScheduleQueryController;
import org.example.backend.service.ScheduleEntryView;
import org.example.backend.service.ScheduleSearchCriteria;
import org.example.backend.service.ScheduleSummaryView;
import org.example.backend.service.TeachingScheduleQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClassTimetableQueryCompatibilityTest {
    private TeachingScheduleQuery query;
    private MockMvc mockMvc;

    /**
     * 设置抬起。
     */
    @BeforeEach
    void setUp() {
        query = mock(TeachingScheduleQuery.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TeachingScheduleQueryController(query)).build();
    }

    /**
     * preserves课表SearchContract的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void preservesTimetableSearchContract() throws Exception {
        when(query.search(eq(new ScheduleSearchCriteria(
                " 2026-Spring ",
                " Alice ",
                "Class 1",
                "Lab 201",
                "Networks",
                "net",
                3,
                8000
        )))).thenReturn(List.of(scheduleEntry()));

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
                .andExpect(jsonPath("$.data[0].weekExpanded").value("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16"))
                .andExpect(jsonPath("$.data[0].classroom").value("Lab 201"))
                .andExpect(jsonPath("$.data[0].createTime").value("2026-02-23T08:00:00"));
    }

    /**
     * preserves摘要And学期Contracts的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void preservesSummaryAndSemesterContracts() throws Exception {
        ScheduleSummaryView summary = new ScheduleSummaryView(
                1,
                1,
                1,
                1,
                LocalDateTime.of(2026, 2, 23, 8, 0),
                List.of("2026-Spring")
        );
        when(query.summary()).thenReturn(summary);
        when(query.listSemesters()).thenReturn(List.of("2026-Spring"));

        mockMvc.perform(get("/class-timetables/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.classCount").value(1))
                .andExpect(jsonPath("$.data.teacherCount").value(1))
                .andExpect(jsonPath("$.data.classroomCount").value(1))
                .andExpect(jsonPath("$.data.latestSyncAt").value("2026-02-23T08:00:00"))
                .andExpect(jsonPath("$.data.semesters[0]").value("2026-Spring"));

        mockMvc.perform(get("/class-timetables/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("2026-Spring"));
    }

    /**
     * 安排条目。
     * @return ScheduleEntryView 类型的处理结果。
     */
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
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16",
                "Lab 201",
                LocalDateTime.of(2026, 2, 23, 8, 0)
        );
    }
}
