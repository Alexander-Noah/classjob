package org.example.backend.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMaintenanceScriptContractTest {
    private static final Path ROOT = Path.of("..", "database", "maintenance", "20261010-core-consolidation");

    @Test
    void scriptsRequireBackupAndRestoreProofBeforeShrink() throws Exception {
        String backup = Files.readString(ROOT.resolve("Backup-CoreDatabase.ps1"));
        String restore = Files.readString(ROOT.resolve("Test-CoreDatabaseBackup.ps1"));
        String migrate = Files.readString(ROOT.resolve("Invoke-CoreDatabaseMigration.ps1"));
        assertAll(
                () -> assertTrue(backup.contains("mysqldump")),
                () -> assertTrue(backup.contains("Get-FileHash")),
                () -> assertTrue(backup.contains("table-archives")),
                () -> assertTrue(backup.contains("information_schema.table_constraints")),
                () -> assertFalse(backup.contains("--password=")),
                () -> assertTrue(restore.contains("smart_lab_restore_rehearsal_")),
                () -> assertTrue(restore.contains("tableArchives")),
                () -> assertTrue(restore.contains("constraint")),
                () -> assertTrue(migrate.contains("restore-rehearsal.json")),
                () -> assertTrue(migrate.contains("tableArchives")),
                () -> assertTrue(migrate.contains("CoreDatabaseMigrationRunner")),
                () -> assertTrue(migrate.contains("SMART_LAB_FLYWAY_TARGET")),
                () -> assertTrue(migrate.contains("SMART_LAB_FLYWAY_ENABLED")),
                () -> assertTrue(migrate.contains("20261010.3"))
        );
    }
}
