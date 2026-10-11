package org.example.backend.timetable;

import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.impl.AcademicCredentialServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetablePersistenceContractTest {
    @Test
    void timetableMapperUsesConsolidatedTablesAndNoDdl() throws IOException {
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

    @Test
    void academicCredentialsReachMapperOnlyAsCiphertext() {
        AtomicReference<String> usernameCipher = new AtomicReference<>();
        AtomicReference<String> passwordCipher = new AtomicReference<>();
        ClassTimetableMapper mapper = (ClassTimetableMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ClassTimetableMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "upsertCrawlerCredential" -> {
                        usernameCipher.set((String) args[1]);
                        passwordCipher.set((String) args[2]);
                        yield 1;
                    }
                    case "getCrawlerCredential" -> Map.of(
                            "usernameCipher", usernameCipher.get(),
                            "passwordCipher", passwordCipher.get(),
                            "updatedAt", "2026-10-11 15:00:00"
                    );
                    default -> 0;
                }
        );
        AcademicCredentialServiceImpl service = new AcademicCredentialServiceImpl(
                mapper,
                "test-only-strong-academic-secret-with-40-plus-chars"
        );
        AcademicScheduleImportRequest request = new AcademicScheduleImportRequest();
        request.setCredentialKey("class-import");
        request.setUsername("plain-username");
        request.setPassword("plain-password");

        service.saveCredential(request);

        assertAll(
                () -> assertFalse("plain-username".equals(usernameCipher.get())),
                () -> assertFalse("plain-password".equals(passwordCipher.get()))
        );
    }
}
