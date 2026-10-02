package org.example.backend.teachingschedule.adapter.out.persistence;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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
    }

    @Test
    void readsOnlyTheClassTimetableTable() throws IOException {
        String xml = new String(mapperXml(), StandardCharsets.UTF_8);
        Matcher matcher = READ_TABLE.matcher(xml);
        Set<String> tables = matcher.results()
                .map(result -> result.group(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertEquals(Set.of("class_timetable"), tables);
    }

    private byte[] mapperXml() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            assertNotNull(input, "module mapper XML must exist at " + RESOURCE);
            return input.readAllBytes();
        }
    }
}
