package org.example.backend.controller;

import org.example.backend.entity.ClassTimetableEntity;
import org.example.backend.service.ClassTimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClassTimetableQueryCompatibilityTest {
    private ClassTimetableService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ClassTimetableService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ClassTimetableController(service)).build();
    }

    @Test
    void preservesTimetableSearchContract() throws Exception {
        when(service.listTimetables(
                eq(" 2026-Spring "),
                eq(" Alice "),
                eq("Class 1"),
                eq("Lab 201"),
                eq("Networks"),
                eq("net"),
                eq(3),
                eq(8000)
        )).thenReturn(List.of(scheduleEntry()));

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

    @Test
    void preservesSummaryAndSemesterContracts() throws Exception {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", 1L);
        summary.put("classCount", 1L);
        summary.put("teacherCount", 1L);
        summary.put("classroomCount", 1L);
        summary.put("latestSyncAt", LocalDateTime.of(2026, 2, 23, 8, 0));
        summary.put("semesters", List.of("2026-Spring"));
        when(service.getSummary()).thenReturn(summary);
        when(service.listSemesters()).thenReturn(List.of("2026-Spring"));

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

    private ClassTimetableEntity scheduleEntry() {
        ClassTimetableEntity entry = new ClassTimetableEntity();
        entry.setId(7L);
        entry.setSemester("2026-Spring");
        entry.setRowClassName("Class 1 (row)");
        entry.setClassName("Class 1");
        entry.setWeekday("Monday");
        entry.setSectionCode("1-2");
        entry.setSectionText("第1-2节");
        entry.setStartSection(1);
        entry.setEndSection(2);
        entry.setCourseName("Networks");
        entry.setTeacher("Alice");
        entry.setWeekRaw("1-16");
        entry.setWeekText("第1-16周");
        entry.setWeekExpanded("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16");
        entry.setClassroom("Lab 201");
        entry.setCreateTime(LocalDateTime.of(2026, 2, 23, 8, 0));
        return entry;
    }
}
