package org.example.backend.service;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AiScheduleMapperTest {
    @Test void scheduleQueriesBindFiltersAndPageParametersWithoutModelSuppliedSql() throws Exception {
        Configuration config = new Configuration();
        for (String file : new String[]{"TeachingScheduleReadMapper", "AiScheduleReadMapper"}) {
            String path = "mapper/" + file + ".xml";
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(input);
                new XMLMapperBuilder(input, config, path, config.getSqlFragments()).parse();
            }
        }
        Map<String, Object> params = Map.of("semester", "2026秋", "week", 6, "weekday", "星期五", "sectionStart", 7,
                "sectionEnd", 8, "teacher", "' or 1=1 --", "offset", 100, "limit", 50, "groupBy", "teacher",
                "labPatterns", java.util.List.of("(^|[^A-Za-z0-9])2-206[mM]?($|[^A-Za-z0-9])"));
        String sql = config.getMappedStatement("org.example.backend.mapper.AiScheduleReadMapper.search").getBoundSql(params).getSql();
        assertTrue(sql.contains("limit ? offset ?")); assertTrue(sql.contains("start_section <= ?"));
        assertFalse(sql.contains("1=1")); assertFalse(sql.contains("2026秋"));
        assertTrue(sql.contains("classroom regexp ?"));
        String aggregate = config.getMappedStatement("org.example.backend.mapper.AiScheduleReadMapper.aggregate").getBoundSql(params).getSql().replaceAll("\\s+", " ");
        assertTrue(aggregate.contains("teacher as label")); assertTrue(aggregate.contains("group by label"));
        assertFalse(aggregate.contains("devices"));
        String unscoped = config.getMappedStatement("org.example.backend.mapper.AiScheduleReadMapper.count").getBoundSql(Map.of()).getSql();
        assertTrue(unscoped.contains("1 = 0"));
    }
}
