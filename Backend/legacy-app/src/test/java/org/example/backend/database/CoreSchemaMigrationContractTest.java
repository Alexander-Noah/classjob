package org.example.backend.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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
    void applicationKeepsFlywayExplicitlyDisabledByDefault() throws Exception {
        String yaml = Files.readString(Path.of("src", "main", "resources", "application.yaml"));
        assertTrue(yaml.contains("enabled: ${SMART_LAB_FLYWAY_ENABLED:false}"));
        assertTrue(yaml.contains("baseline-version: 20261009"));
    }
}
