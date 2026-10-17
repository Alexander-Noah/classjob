package org.example.backend.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMaintenanceScriptContractTest {
    private static final Path ROOT = Path.of("database", "maintenance", "20261010-core-consolidation");

    /**
     * scriptsRequire备份AndRestoreProofBeforeShrink的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void scriptsRequireBackupAndRestoreProofBeforeShrink() throws Exception {
        String backup = Files.readString(ROOT.resolve("Backup-CoreDatabase.ps1"));
        String restore = Files.readString(ROOT.resolve("Test-CoreDatabaseBackup.ps1"))
                .replace("\r\n", "\n");
        String migrate = Files.readString(ROOT.resolve("Invoke-CoreDatabaseMigration.ps1"));
        String readme = Files.readString(ROOT.resolve("README.md"));
        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(backup.contains("mysqldump")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(backup.contains("Get-FileHash")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(backup.contains("--password=")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(restore.contains("smart_lab_restore_rehearsal_")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(restore.contains("$restoreSchema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$'")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(restore.contains("$schemaCreated")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(restore.contains("finally {\n    try {\n        $report.completedAtUtc")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(restore.contains("finally {\n        if ($restoreSchema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$')")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(restore.contains("DROP DATABASE IF EXISTS ``$restoreSchema``")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migrate.contains("restore-rehearsal.json")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migrate.contains("SMART_LAB_FLYWAY_ENABLED")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migrate.contains("20261010.3")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(migrate.startsWith("#requires -Version 7.0")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(migrate.contains("legacy-app")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(readme.contains("PowerShell 7.0"))
        );
    }
}
