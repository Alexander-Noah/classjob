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
    /**
     * 课表数据映射器UsesConsolidatedTablesAndNoDdl的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void timetableMapperUsesConsolidatedTablesAndNoDdl() throws Exception {
        Configuration configuration = parseMapper();
        String xml = Files.readString(Path.of("src/main/resources/mapper/ClassTimetableMapper.xml"));
        String mapper = Files.readString(Path.of("src/main/java/org/example/backend/mapper/ClassTimetableMapper.java"));
        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(xml.contains("timetable_integration_config")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(xml.contains("timetable_import_job")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(xml.contains("timetable_crawler_config")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(xml.contains("timetable_academic_credential")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(xml.contains("timetable_crawler_task")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(xml.toLowerCase(Locale.ROOT).contains("create table")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(mapper.contains("createCrawler")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(mapper.contains("addCrawlerConfigSemesterColumn")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(boundSql(configuration, "deleteBySemester", null).contains("class_timetable")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(boundSql(configuration, "insertTimetable", new ClassTimetableEntity()).contains("insert into class_timetable")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(boundSql(configuration, "insertCrawlerTask", Map.of("id", 1L)).contains("config_id")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(boundSql(configuration, "insertCrawlerTask", Map.of("id", 1L)).contains("timetable_crawler_task")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(boundSql(configuration, "getCrawlerCredential", null).contains("timetable_academic_credential"))
        );
    }

    /**
     * 教务凭据UseCiphertextAndClearShared行WithoutDelete的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void academicCredentialsUseCiphertextAndClearSharedRowWithoutDelete() throws Exception {
        AtomicReference<String> usernameCipher = new AtomicReference<>();
        AtomicReference<String> passwordCipher = new AtomicReference<>();
        AtomicBoolean deleteCalled = new AtomicBoolean();
        Map<String, Object> storedCredential = new HashMap<>();
        ClassTimetableMapper mapper = (ClassTimetableMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ClassTimetableMapper.class},
                /**
                 * 处理 newProxyInstance 调用中的回调逻辑。
                 * 入参：proxy、method、args。
                 */
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
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse("plain-username".equals(usernameCipher.get())),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse("plain-password".equals(passwordCipher.get())),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(deleteCalled.get()),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(storedCredential.containsKey("usernameCipher")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(storedCredential.containsKey("passwordCipher")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(storedCredential.get("usernameCipher") == null),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(storedCredential.get("passwordCipher") == null),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(deleteSql.contains("set credential_username_cipher = null")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(deleteSql.contains("credential_password_cipher = null")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(deleteSql.contains("delete from"))
        );
    }

    /**
     * 解析数据映射器。
     * @return Configuration 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Configuration parseMapper() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/ClassTimetableMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "mapper/ClassTimetableMapper.xml", configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    /**
     * boundSQL的处理逻辑。
     * @param configuration 配置参数。
     * @param statementName statement名称参数。
     * @param parameter parameter参数。
     * @return String 类型的处理结果。
     */
    private String boundSql(Configuration configuration, String statementName, Object parameter) {
        return configuration.getMappedStatement("org.example.backend.mapper.ClassTimetableMapper." + statementName)
                .getBoundSql(parameter)
                .getSql()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
