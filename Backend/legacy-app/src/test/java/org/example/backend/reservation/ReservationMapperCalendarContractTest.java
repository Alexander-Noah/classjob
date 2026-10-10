package org.example.backend.reservation;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationMapperCalendarContractTest {
    @Test
    void timetableConflictQueryUsesCharsetThenCollationForWeekday() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/ReservationsMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(
                    input,
                    configuration,
                    "mapper/ReservationsMapper.xml",
                    configuration.getSqlFragments()
            ).parse();
        }

        BoundSql boundSql = configuration
                .getMappedStatement("org.example.backend.mapper.ReservationsMapper.countTimetableConflicts")
                .getBoundSql(Map.of("reservation", Map.of(), "weekday", 5, "week", 7));
        String sql = boundSql.getSql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        assertFalse(sql.contains("using utf8mb4_general_ci"));
        assertTrue(sql.contains(
                "convert(cast(? as char) using utf8mb4) collate utf8mb4_general_ci"
        ));
        assertTrue(sql.contains(
                "convert(t.weekday using utf8mb4) collate utf8mb4_general_ci = "
                        + "convert(cast(? as char) using utf8mb4) collate utf8mb4_general_ci"
        ));
        assertTrue(sql.indexOf("find_in_set(") == sql.lastIndexOf("find_in_set("));
    }
}
