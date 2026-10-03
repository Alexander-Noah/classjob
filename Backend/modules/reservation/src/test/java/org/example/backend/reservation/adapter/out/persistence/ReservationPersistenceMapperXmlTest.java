package org.example.backend.reservation.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationPersistenceMapperXmlTest {
    @Test
    void mapsEveryOwnedColumnWithConstructorResultMapAndStableListOrdering() throws IOException {
        String xml = mapperXml();

        assertTrue(xml.contains("<constructor>"));
        for (String column : new String[]{
                "id", "applicant_user_id", "applicant_name", "department", "contact", "lab_id",
                "reservation_date", "time_range", "scene", "attendee_count", "reason", "status",
                "conflict_flag", "reviewer_name", "review_remark", "created_at", "updated_at"
        }) {
            assertTrue(xml.contains("column=\"" + column + "\""), column);
        }
        assertTrue(xml.contains("order by created_at desc, id desc"));
    }

    @Test
    void ownsOnlyReservationTableAndNeverUnscopesAnEmptyLaboratorySet() throws IOException {
        String xml = mapperXml().toLowerCase();

        assertTrue(xml.contains("from lab_reservation"));
        assertTrue(xml.contains("scope.laboratoryids.isempty()"));
        assertTrue(xml.contains("1 = 0"));
        assertFalse(xml.contains(" join "));
        assertFalse(xml.contains("select *"));
        assertFalse(xml.contains("resulttype"));
        assertFalse(xml.contains("from lab "));
        assertFalse(xml.contains("class_timetable"));
        assertFalse(xml.contains("usage"));
        assertFalse(xml.contains("notice"));
        assertFalse(xml.contains("dashboard"));
        assertFalse(xml.contains("sys_user"));
    }

    @Test
    void conditionallyUpdatesReviewConflictFlagInTheSameStatement() throws IOException {
        String xml = mapperXml();

        assertTrue(xml.contains("<update id=\"updateReview\">"));
        assertTrue(xml.contains("conflict_flag = #{update.conflictFlag}"));
    }

    private String mapperXml() throws IOException {
        try (var input = getClass().getResourceAsStream("/mapper/ReservationPersistenceMapper.xml")) {
            if (input == null) {
                throw new IOException("ReservationPersistenceMapper.xml not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }
}
