package org.example.backend.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreSchemaMigrationContractTest {
    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");

    @Test
    void expandMigrationCreatesExactlyTheFourReplacementTables() throws Exception {
        String sql = Files.readString(MIGRATIONS.resolve("V20261010_01__expand_core_schema.sql")).toLowerCase();
        Set<String> expected = Set.of(
                "device_event", "timetable_integration_config",
                "timetable_import_job", "audit_event"
        );
        for (String table : expected) {
            assertTrue(sql.contains("create table " + table), table);
        }
        assertTrue(sql.contains("alter table usage_record"));
        assertTrue(sql.contains("add column user_id"));
        assertTrue(sql.contains("add column lab_id"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("drop column"));
    }

    @Test
    void shrinkMigrationDropsRetiredTablesBeforeRemovingRedundantDeviceName() throws Exception {
        String sql = Files.readString(MIGRATIONS.resolve("V20261010_03__shrink_retired_schema.sql"))
                .toLowerCase(Locale.ROOT);
        Set<String> retiredTables = Set.of(
                "iot_command_log", "iot_telemetry_latest", "iot_gateway_point", "iot_device_capability",
                "iot_gateway_config", "iot_device", "device_status_event", "device_runtime_status",
                "lab_terminal", "lab_activation_code", "student_client", "teacher_host",
                "consumable_stock_record", "consumable_inventory", "course_environment_request",
                "environment_template", "lab_software", "schedule_adjustment", "approval_countersign",
                "ai_assistant_conversation", "ai_assistant_user_config", "host_static_asset",
                "device_inventory_record", "device_transfer_record", "timetable_crawler_task",
                "timetable_academic_credential", "timetable_crawler_config", "module_record"
        );

        for (String table : retiredTables) {
            assertTrue(sql.contains("drop table if exists " + table), table);
        }
        assertTrue(sql.contains("alter table device drop column lab_name"));
        assertTrue(sql.indexOf("drop table if exists device_inventory_record")
                < sql.indexOf("alter table device drop column lab_name"));
    }

    @Test
    void applicationKeepsFlywayExplicitlyDisabledByDefault() throws Exception {
        String yaml = Files.readString(Path.of("src", "main", "resources", "application.yaml"));
        assertTrue(yaml.contains("enabled: ${SMART_LAB_FLYWAY_ENABLED:false}"));
        assertTrue(yaml.contains("baseline-version: 20261009"));
    }
}
