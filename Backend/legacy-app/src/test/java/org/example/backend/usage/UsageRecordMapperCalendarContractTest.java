package org.example.backend.usage;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsageRecordMapperCalendarContractTest {
    @Test
    void courseUsageUsesConfiguredAtomicSemesterCalendar() throws IOException {
        byte[] mapperBytes;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/UsageRecordMapper.xml")) {
            assertNotNull(input);
            mapperBytes = input.readAllBytes();
        }

        Configuration configuration = new Configuration();
        new XMLMapperBuilder(
                new ByteArrayInputStream(mapperBytes),
                configuration,
                "mapper/UsageRecordMapper.xml",
                configuration.getSqlFragments()
        ).parse();

        BoundSql boundSql = configuration
                .getMappedStatement("org.example.backend.mapper.UsageRecordMapper.listUsageRecords")
                .getBoundSql(null);
        String sql = boundSql.getSql()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("join timetable_integration_config"));
        assertTrue(sql.contains("convert(t.semester"));
        assertTrue(sql.contains("convert(crawler_config.semester"));
        assertTrue(sql.contains("crawler_config.semester_start_date"));
        assertFalse(sql.contains("str_to_date(crawler_config.semester_start_date"));
        assertFalse(sql.contains("datediff(curdate(), '2026-03-02')"));
    }
}
