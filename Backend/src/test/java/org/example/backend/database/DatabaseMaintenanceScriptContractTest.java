package org.example.backend.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMaintenanceScriptContractTest {
    private static final Path ROOT = Path.of("database", "maintenance", "20261010-core-consolidation");

    @Test
    void scriptsRequireBackupAndRestoreProofBeforeShrink() throws Exception {
        String backup = Files.readString(ROOT.resolve("Backup-CoreDatabase.ps1"));
        String restore = Files.readString(ROOT.resolve("Test-CoreDatabaseBackup.ps1"));
        String migrate = Files.readString(ROOT.resolve("Invoke-CoreDatabaseMigration.ps1"));
        String readme = Files.readString(ROOT.resolve("README.md"));
        assertAll(
                () -> assertTrue(backup.contains("mysqldump")),
                () -> assertTrue(backup.contains("Get-FileHash")),
                () -> assertFalse(backup.contains("--password=")),
                () -> assertTrue(restore.contains("smart_lab_restore_rehearsal_")),
                () -> assertTrue(restore.contains("$restoreSchema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$'")),
                () -> assertFalse(restore.contains("$schemaCreated")),
                () -> assertTrue(restore.contains("finally {\n    try {\n        $report.completedAtUtc")),
                () -> assertTrue(restore.contains("finally {\n        if ($restoreSchema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$')")),
                () -> assertTrue(restore.contains("DROP DATABASE IF EXISTS ``$restoreSchema``")),
                () -> assertTrue(migrate.contains("restore-rehearsal.json")),
                () -> assertTrue(migrate.contains("SMART_LAB_FLYWAY_ENABLED")),
                () -> assertTrue(migrate.contains("20261010.3")),
                () -> assertTrue(migrate.startsWith("#requires -Version 7.0")),
                () -> assertTrue(readme.contains("PowerShell 7.0"))
        );
    }
}
