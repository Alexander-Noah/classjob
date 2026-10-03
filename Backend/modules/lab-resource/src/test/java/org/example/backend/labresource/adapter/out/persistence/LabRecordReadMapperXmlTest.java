package org.example.backend.labresource.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabRecordReadMapperXmlTest {
    @Test
    void mapsEveryOwnedColumnExplicitlyAndKeepsStableOrdering() throws IOException {
        String xml = mapperXml();

        for (String column : new String[]{
                "id", "lab_code", "lab_name", "building", "floor", "room_no", "lab_type",
                "capacity", "manager_user_id", "open_status", "remark", "created_at", "updated_at"
        }) {
            assertTrue(xml.contains("column=\"" + column + "\""), column);
        }
        assertTrue(xml.contains("order by id asc"));
        assertTrue(xml.contains("manager_user_id = #{managerUserId}"));
        assertTrue(xml.contains("and manager_user_id = #{managerUserId}"));
    }

    @Test
    void readsOnlyTheLabTable() throws IOException {
        String xml = mapperXml().toLowerCase();

        assertTrue(xml.contains("from lab"));
        assertFalse(xml.contains("sys_user"));
        assertFalse(xml.contains("from device"));
        assertFalse(xml.contains("join device"));
        assertFalse(xml.contains("lab_reservation"));
        assertFalse(xml.contains("class_timetable"));
    }

    private String mapperXml() throws IOException {
        try (var input = getClass().getResourceAsStream("/mapper/LabRecordReadMapper.xml")) {
            if (input == null) {
                throw new IOException("LabRecordReadMapper.xml not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }
}
