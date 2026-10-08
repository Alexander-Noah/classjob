package org.example.backend.teachingschedule.adapter.out.persistence;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.application.port.out.ScheduleOccupancyCriteria;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeachingScheduleReadMapperXmlTest {
    private static final String RESOURCE = "mapper/TeachingScheduleReadMapper.xml";
    private static final Pattern READ_TABLE = Pattern.compile("(?i)\\b(?:from|join)\\s+([a-zA-Z0-9_]+)");

    @Test
    void declaresCompatibleReadStatementsAndSql() throws IOException {
        byte[] xml = mapperXml();
        Configuration configuration = new Configuration();
        new XMLMapperBuilder(
                new ByteArrayInputStream(xml),
                configuration,
                RESOURCE,
                configuration.getSqlFragments()
        ).parse();

        String mapperName = TeachingScheduleReadMapper.class.getName();
        assertTrue(configuration.hasStatement(mapperName + ".search"));
        assertTrue(configuration.hasStatement(mapperName + ".summary"));
        assertTrue(configuration.hasStatement(mapperName + ".listSemesters"));
        assertTrue(configuration.hasStatement(mapperName + ".findActiveCalendar"));
        assertTrue(configuration.hasStatement(mapperName + ".findOccupancyCandidates"));

        BoundSql boundSql = configuration.getMappedStatement(mapperName + ".search").getBoundSql(
                new ScheduleSearchCriteria(
                        "2026-Spring", "Alice", "Class 1", "Lab 201", "Networks", "net", 3, 500
                )
        );
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("from class_timetable"));
        assertTrue(sql.contains("semester = ?"));
        assertTrue(sql.contains("find_in_set"));
        assertTrue(sql.contains("order by id desc"));
        assertTrue(sql.contains("limit ?"));

        BoundSql occupancySql = configuration.getMappedStatement(mapperName + ".findOccupancyCandidates")
                .getBoundSql(new ScheduleOccupancyCriteria("2026-2027-1", 6, "星期四"));
        String occupancy = occupancySql.getSql().replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        assertTrue(occupancy.contains("from class_timetable"));
        assertTrue(occupancy.contains("semester = ?"));
        assertTrue(occupancy.contains("weekday = ?"));
        assertTrue(occupancy.contains("find_in_set"));
        assertTrue(!occupancy.contains("limit"), "occupancy arbitration must inspect every candidate row");
    }

    @Test
    void readsOnlyTheClassTimetableTable() throws IOException {
        String xml = new String(mapperXml(), StandardCharsets.UTF_8);
        Matcher matcher = READ_TABLE.matcher(xml);
        Set<String> tables = matcher.results()
                .map(result -> result.group(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertEquals(Set.of("class_timetable", "timetable_crawler_config"), tables);
    }

    @Test
    void mapsEveryDatabaseColumnExplicitlyToTheApiView() throws IOException {
        byte[] xml = mapperXml();
        Configuration configuration = new Configuration();
        new XMLMapperBuilder(
                new ByteArrayInputStream(xml),
                configuration,
                RESOURCE,
                configuration.getSqlFragments()
        ).parse();

        ResultMap resultMap = configuration.getResultMap(
                TeachingScheduleReadMapper.class.getName() + ".scheduleEntryView"
        );
        Map<String, String> columnByProperty = resultMap.getResultMappings().stream()
                .collect(Collectors.toMap(ResultMapping::getProperty, ResultMapping::getColumn));

        assertEquals(Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("semester", "semester"),
                Map.entry("rowClassName", "row_class_name"),
                Map.entry("className", "class_name"),
                Map.entry("weekday", "weekday"),
                Map.entry("sectionCode", "section_code"),
                Map.entry("sectionText", "section_text"),
                Map.entry("startSection", "start_section"),
                Map.entry("endSection", "end_section"),
                Map.entry("courseName", "course_name"),
                Map.entry("teacher", "teacher"),
                Map.entry("weekRaw", "week_raw"),
                Map.entry("weekText", "week_text"),
                Map.entry("weekExpanded", "week_expanded"),
                Map.entry("classroom", "classroom"),
                Map.entry("createTime", "create_time")
        ), columnByProperty);

        ResultMap summaryResultMap = configuration.getResultMap(
                TeachingScheduleReadMapper.class.getName() + ".scheduleSummaryData"
        );
        Map<String, String> summaryColumnByProperty = summaryResultMap.getResultMappings().stream()
                .collect(Collectors.toMap(ResultMapping::getProperty, ResultMapping::getColumn));
        assertEquals(Map.of(
                "total", "total",
                "classCount", "class_count",
                "teacherCount", "teacher_count",
                "classroomCount", "classroom_count",
                "latestSyncAt", "latest_sync_at"
        ), summaryColumnByProperty);

        ResultMap calendar = configuration.getResultMap(mapperName() + ".scheduleCalendarData");
        assertEquals(Map.of("semester", "semester", "semesterStartDate", "semester_start_date"),
                calendar.getResultMappings().stream()
                        .collect(Collectors.toMap(ResultMapping::getProperty, ResultMapping::getColumn)));
        ResultMap occupancy = configuration.getResultMap(mapperName() + ".scheduleOccupancyRow");
        assertEquals(Map.of("classroom", "classroom", "startSection", "start_section", "endSection", "end_section"),
                occupancy.getResultMappings().stream()
                        .collect(Collectors.toMap(ResultMapping::getProperty, ResultMapping::getColumn)));
    }

    private String mapperName() {
        return TeachingScheduleReadMapper.class.getName();
    }

    private byte[] mapperXml() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            assertNotNull(input, "module mapper XML must exist at " + RESOURCE);
            return input.readAllBytes();
        }
    }
}
