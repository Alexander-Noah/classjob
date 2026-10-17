/*
 * 课表回归测试；测试夹具与生产配置分离。
 * 覆盖场景：timetableMapperUsesConsolidatedTablesAndNoDdl、academicCredentialsUseCiphertextAndClearSharedRowWithoutDelete。
 */
package org.example.backend.timetable;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.entity.ClassTimetableEntity;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.impl.AcademicCredentialServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetablePersistenceContractTest {
    @Test
    void timetableMapperUsesConsolidatedTablesAndNoDdl() throws Exception {
        Configuration configuration = parseMapper();
        String xml = Files.readString(Path.of("src/main/resources/mapper/ClassTimetableMapper.xml"));
        String mapper = Files.readString(Path.of("src/main/java/org/example/backend/mapper/ClassTimetableMapper.java"));
        assertAll(
                () -> assertTrue(xml.contains("timetable_integration_config")),
                () -> assertTrue(xml.contains("timetable_import_job")),
                () -> assertFalse(xml.contains("timetable_crawler_config")),
                () -> assertFalse(xml.contains("timetable_academic_credential")),
                () -> assertFalse(xml.contains("timetable_crawler_task")),
                () -> assertFalse(xml.toLowerCase(Locale.ROOT).contains("create table")),
                () -> assertFalse(mapper.contains("createCrawler")),
                () -> assertFalse(mapper.contains("addCrawlerConfigSemesterColumn")),
                () -> assertTrue(boundSql(configuration, "deleteBySemester", null).contains("class_timetable")),
                () -> assertTrue(boundSql(configuration, "insertTimetable", new ClassTimetableEntity()).contains("insert into class_timetable")),
                () -> assertTrue(boundSql(configuration, "insertCrawlerTask", Map.of("id", 1L)).contains("config_id")),
                () -> assertFalse(boundSql(configuration, "insertCrawlerTask", Map.of("id", 1L)).contains("timetable_crawler_task")),
                () -> assertFalse(boundSql(configuration, "getCrawlerCredential", null).contains("timetable_academic_credential"))
        );
    }

    @Test
    void academicCredentialsUseCiphertextAndClearSharedRowWithoutDelete() throws Exception {
        AtomicReference<String> usernameCipher = new AtomicReference<>();
        AtomicReference<String> passwordCipher = new AtomicReference<>();
        AtomicBoolean deleteCalled = new AtomicBoolean();
        Map<String, Object> storedCredential = new HashMap<>();
        ClassTimetableMapper mapper = (ClassTimetableMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ClassTimetableMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "upsertCrawlerCredential" -> {
                        usernameCipher.set((String) args[1]);
                        passwordCipher.set((String) args[2]);
                        storedCredential.put("usernameCipher", usernameCipher.get());
                        storedCredential.put("passwordCipher", passwordCipher.get());
                        storedCredential.put("updatedAt", "2026-10-11 15:00:00");
                        yield 1;
                    }
                    case "getCrawlerCredential" -> storedCredential;
                    case "deleteCrawlerCredential" -> {
                        deleteCalled.set(true);
                        storedCredential.put("usernameCipher", null);
                        storedCredential.put("passwordCipher", null);
                        yield 1;
                    }
                    default -> 0;
                }
        );
        AcademicCredentialServiceImpl service = new AcademicCredentialServiceImpl(
                mapper, "test-only-strong-academic-secret-with-40-plus-chars");
        AcademicScheduleImportRequest request = new AcademicScheduleImportRequest();
        request.setCredentialKey("class-import");
        request.setUsername("plain-username");
        request.setPassword("plain-password");

        service.saveCredential(request);
        service.deleteCredential("class-import");

        Configuration configuration = parseMapper();
        String deleteSql = boundSql(configuration, "deleteCrawlerCredential", Map.of("credentialKey", "class-import"));
        assertAll(
                () -> assertFalse("plain-username".equals(usernameCipher.get())),
                () -> assertFalse("plain-password".equals(passwordCipher.get())),
                () -> assertTrue(deleteCalled.get()),
                () -> assertTrue(storedCredential.containsKey("usernameCipher")),
                () -> assertTrue(storedCredential.containsKey("passwordCipher")),
                () -> assertTrue(storedCredential.get("usernameCipher") == null),
                () -> assertTrue(storedCredential.get("passwordCipher") == null),
                () -> assertTrue(deleteSql.contains("set credential_username_cipher = null")),
                () -> assertTrue(deleteSql.contains("credential_password_cipher = null")),
                () -> assertFalse(deleteSql.contains("delete from"))
        );
    }

    private Configuration parseMapper() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/ClassTimetableMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "mapper/ClassTimetableMapper.xml", configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private String boundSql(Configuration configuration, String statementName, Object parameter) {
        return configuration.getMappedStatement("org.example.backend.mapper.ClassTimetableMapper." + statementName)
                .getBoundSql(parameter)
                .getSql()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
