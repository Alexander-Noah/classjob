/*
 * DatabaseSchemaInitializerCompatibilityTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：phaseOneMigrationDocumentsDeviceRuntimeLabAccessAndIotTables、startupInitializerKeepsPhaseOneTablesCompatibleForExistingDatabases、timetableCalendarPersistenceUsesConsolidatedDateTypedConfiguration。
 */
package org.example.backend.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSchemaInitializerCompatibilityTest {
    /**
     * phaseOneMigrationDocuments设备运行时实验室AccessAnd物联Tables的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void phaseOneMigrationDocumentsDeviceRuntimeLabAccessAndIotTables() throws IOException {
        String migration = Files.readString(Path.of("..", "web", "database", "migrations", "20260702_device_runtime_iot_phase1.sql"));

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("ALTER TABLE device")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("lab_name")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("source_type")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS lab_activation_code")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS lab_terminal")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS device_runtime_status")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS device_status_event")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_device")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_device_capability")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_gateway_config")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_gateway_point")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_command_log")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("gateway_id")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("point_id")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("request_params_json")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("response_result_json")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("execution_status")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("gateway_mode")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_telemetry_latest"))
        );
    }

    /**
     * startupInitializerKeepsPhaseOneTablesCompatibleForExistingDatabases的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void startupInitializerKeepsPhaseOneTablesCompatibleForExistingDatabases() throws IOException {
        String initializer = Files.readString(Path.of("src", "main", "java", "org", "example", "backend", "config", "DatabaseSchemaInitializer.java"));

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("ensureDeviceRuntimeAndAccessTables")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("lab_activation_code")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("lab_terminal")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("device_runtime_status")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("device_status_event")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("iot_device")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("iot_device_capability")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("iot_gateway_config")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("iot_gateway_point")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("iot_command_log")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("addIotCommandLogColumnIfMissing")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(initializer.contains("iot_telemetry_latest"))
        );
    }

    /**
     * 课表CalendarPersistenceUsesConsolidated日期Typed配置的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void timetableCalendarPersistenceUsesConsolidatedDateTypedConfiguration() throws IOException {
        String mapper = Files.readString(Path.of("src", "main", "resources", "mapper", "ClassTimetableMapper.xml"));
        String implementation = Files.readString(Path.of("src", "main", "java", "org", "example", "backend",
                "service", "impl", "ClassTimetableServiceImpl.java"));

        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("timetable_integration_config")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("semester_start_date")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("semester = values(semester)")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(implementation.contains("LocalDate semesterStartDate")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(implementation.contains("TIMETABLE_SEMESTER")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(implementation.contains("crawlerConfig.get(\"semester\")")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(!mapper.contains("timetable_crawler_config")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(!mapper.contains("create table"))
        );
    }
}
