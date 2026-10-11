package org.example.backend.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreReferenceNormalizationTest {
    @Test
    void currentStateWritesForeignKeysAndReadsDisplayNamesByJoin() throws Exception {
        String devices = Files.readString(Path.of("src/main/resources/mapper/DevicesMapper.xml"));
        String usage = Files.readString(Path.of("src/main/resources/mapper/UsageRecordMapper.xml"));
        assertAll(
                () -> assertFalse(extractStatement(devices, "insert", "InserterDevices").contains("lab_name")),
                () -> assertFalse(extractStatement(devices, "update", "updateDevices").contains("lab_name")),
                () -> assertTrue(devices.contains("left join lab l on d.lab_id = l.id")),
                () -> assertTrue(usage.contains("user_id")),
                () -> assertTrue(usage.contains("lab_id")),
                () -> assertTrue(usage.contains("source_type")),
                () -> assertTrue(usage.contains("source_id"))
        );
    }

    private String extractStatement(String xml, String tag, String id) {
        String start = "<" + tag + " id=\"" + id + "\"";
        int startIndex = xml.indexOf(start);
        assertTrue(startIndex >= 0, start + " should exist");
        int endIndex = xml.indexOf("</" + tag + ">", startIndex);
        assertTrue(endIndex > startIndex, "closing tag should exist");
        return xml.substring(startIndex, endIndex);
    }
}
