package org.example.backend.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSchemaInitializerCompatibilityTest {
    @Test
    void phaseOneMigrationDocumentsDeviceRuntimeLabAccessAndIotTables() throws IOException {
        String migration = Files.readString(Path.of("..", "..", "web", "database", "migrations", "20260702_device_runtime_iot_phase1.sql"));

        assertAll(
                () -> assertTrue(migration.contains("ALTER TABLE device")),
                () -> assertTrue(migration.contains("lab_name")),
                () -> assertTrue(migration.contains("source_type")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS lab_activation_code")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS lab_terminal")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS device_runtime_status")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS device_status_event")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_device")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_device_capability")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_gateway_config")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_gateway_point")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_command_log")),
                () -> assertTrue(migration.contains("gateway_id")),
                () -> assertTrue(migration.contains("point_id")),
                () -> assertTrue(migration.contains("request_params_json")),
                () -> assertTrue(migration.contains("response_result_json")),
                () -> assertTrue(migration.contains("execution_status")),
                () -> assertTrue(migration.contains("gateway_mode")),
                () -> assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS iot_telemetry_latest"))
        );
    }

    @Test
    void startupInitializerKeepsPhaseOneTablesCompatibleForExistingDatabases() throws IOException {
        String initializer = Files.readString(Path.of("src", "main", "java", "org", "example", "backend", "config", "DatabaseSchemaInitializer.java"));

        assertAll(
                () -> assertTrue(initializer.contains("ensureDeviceRuntimeAndAccessTables")),
                () -> assertTrue(initializer.contains("lab_activation_code")),
                () -> assertTrue(initializer.contains("lab_terminal")),
                () -> assertTrue(initializer.contains("device_runtime_status")),
                () -> assertTrue(initializer.contains("device_status_event")),
                () -> assertTrue(initializer.contains("iot_device")),
                () -> assertTrue(initializer.contains("iot_device_capability")),
                () -> assertTrue(initializer.contains("iot_gateway_config")),
                () -> assertTrue(initializer.contains("iot_gateway_point")),
                () -> assertTrue(initializer.contains("iot_command_log")),
                () -> assertTrue(initializer.contains("addIotCommandLogColumnIfMissing")),
                () -> assertTrue(initializer.contains("iot_telemetry_latest"))
        );
    }

    @Test
    void timetableCalendarPersistenceKeepsSemesterAndStartDateAtomicWithoutRuntimeDdl() throws IOException {
        String mapper = Files.readString(Path.of("src", "main", "resources", "mapper", "ClassTimetableMapper.xml"));
        String implementation = Files.readString(Path.of("src", "main", "java", "org", "example", "backend",
                "service", "impl", "ClassTimetableServiceImpl.java"));

        assertAll(
                () -> assertTrue(mapper.contains("timetable_integration_config")),
                () -> assertTrue(mapper.contains("date_format(semester_start_date, '%Y-%m-%d')")),
                () -> assertTrue(mapper.contains("semester = values(semester)")),
                () -> assertTrue(mapper.contains("semester_start_date = values(semester_start_date)")),
                () -> assertTrue(!mapper.toLowerCase().contains("create table")),
                () -> assertTrue(implementation.contains("TIMETABLE_SEMESTER")),
                () -> assertTrue(implementation.contains("crawlerConfig.get(\"semester\")"))
        );
    }
}
