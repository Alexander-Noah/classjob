package org.example.backend.dashboard;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardMapperCalendarContractTest {
    @Test
    void todayScheduleUsesConfiguredAtomicSemesterCalendar() throws IOException {
        byte[] mapperBytes;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/DashboardMapper.xml")) {
            assertNotNull(input);
            mapperBytes = input.readAllBytes();
        }

        Configuration configuration = new Configuration();
        new XMLMapperBuilder(
                new ByteArrayInputStream(mapperBytes),
                configuration,
                "mapper/DashboardMapper.xml",
                configuration.getSqlFragments()
        ).parse();

        BoundSql boundSql = configuration
                .getMappedStatement("org.example.backend.mapper.DashboardMapper.listTodaySchedules")
                .getBoundSql(Map.of("managerUserId", 1L));
        String xml = boundSql.getSql()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        assertTrue(xml.contains("join timetable_crawler_config"));
        assertTrue(xml.contains("convert(t.semester"));
        assertTrue(xml.contains("convert(crawler_config.semester"));
        assertTrue(xml.contains("crawler_config.semester_start_date"));
        assertFalse(xml.contains("datediff(curdate(), '2026-03-02')"));
    }
}
