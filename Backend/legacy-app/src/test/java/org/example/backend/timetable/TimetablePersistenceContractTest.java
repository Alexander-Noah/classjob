package org.example.backend.timetable;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetablePersistenceContractTest {
    @Test
    void timetableMapperUsesConsolidatedTablesAndNoDdl() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/ClassTimetableMapper.xml"));
        String mapper = Files.readString(Path.of("src/main/java/org/example/backend/mapper/ClassTimetableMapper.java"));
        assertAll(
                () -> assertTrue(xml.contains("timetable_integration_config")),
                () -> assertTrue(xml.contains("timetable_import_job")),
                () -> assertFalse(xml.contains("timetable_crawler_config")),
                () -> assertFalse(xml.contains("timetable_academic_credential")),
                () -> assertFalse(xml.contains("timetable_crawler_task")),
                () -> assertFalse(xml.toLowerCase().contains("create table")),
                () -> assertFalse(mapper.contains("createCrawler")),
                () -> assertFalse(mapper.contains("addCrawlerConfigSemesterColumn"))
        );
    }
}
