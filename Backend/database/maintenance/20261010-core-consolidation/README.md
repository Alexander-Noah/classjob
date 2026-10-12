# Core Database Consolidation Runbook

Run these commands from this directory in PowerShell 7. Stop application writes before the final backup and keep the archive outside the repository until post-migration verification is complete.

The guarded workflow has two destructive-migration gates:

1. `Test-CoreDatabaseBackup.ps1` must restore `full.sql`, match every table row count, and verify every recorded primary, unique, and foreign-key constraint.
2. The expanded schema must pass the application/API smoke matrix before `-Phase Shrink` is allowed.

```powershell
$env:MYSQL_PWD = Read-Host 'MySQL password' -MaskInput
$archive = ./Backup-CoreDatabase.ps1 -Database smart_lab_basic -ArchiveRoot 'E:\smart-lab-db-archives'
./Test-CoreDatabaseBackup.ps1 -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Expand -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Shrink -ArchiveDirectory $archive
Remove-Item Env:MYSQL_PWD
```

`Backup-CoreDatabase.ps1` writes a full dump, the fixed retired-table subset, one SQL archive per source table, SHA-256 hashes, and a row/key manifest. `Test-CoreDatabaseBackup.ps1` restores the full dump into a random `smart_lab_restore_rehearsal_*` schema, verifies all row counts and key signatures, records the result, and removes only that rehearsal schema. The shrink phase refuses to start unless every archive hash and the restore proof are valid.

The archive also contains `table-archives/*.sql`, one SQL archive per source table. `manifest.json` records each archive SHA-256 and the source table's primary, unique, and foreign-key signature. `restore-rehearsal.json` records the row-count and constraint verification result without storing credentials or ciphertext values.

Flyway remains disabled during ordinary application startup. The migration wrapper invokes the dedicated `CoreDatabaseMigrationRunner` with the approved expand or shrink target, then verifies the corresponding successful row in `flyway_schema_history`; starting the web application alone is not considered a migration.

After `Shrink`, the expected physical table set is exactly:

```text
sys_user
role
user_role
lab
device
device_event
class_timetable
lab_reservation
timetable_integration_config
timetable_import_job
repair_order
usage_record
notice
notice_recipient
audit_event
flyway_schema_history
```

Verify the live database after cutover with:

```sql
select table_name
from information_schema.tables
where table_schema = 'smart_lab_basic'
order by table_name;

select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;
```

Record the archive path, checksums, before/after table sets and row counts, Flyway history, smoke results, and restart results in `docs/database/2026-10-10-core-database-migration-report.md`. Do not put passwords, tokens, encrypted credentials, or API keys in that report.
