# Core database consolidation runbook

Run these commands from this directory in PowerShell 7. Keep the archive outside the repository and retain it until post-migration verification is complete.

```powershell
$env:MYSQL_PWD = Read-Host 'MySQL password' -MaskInput
$archive = ./Backup-CoreDatabase.ps1 -Database smart_lab_basic -ArchiveRoot 'E:\smart-lab-db-archives'
./Test-CoreDatabaseBackup.ps1 -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Expand -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Shrink -ArchiveDirectory $archive
Remove-Item Env:MYSQL_PWD
```

`Backup-CoreDatabase.ps1` writes a full dump, the fixed retired-table subset, hashes, and a row-count manifest. `Test-CoreDatabaseBackup.ps1` restores the full dump into a random `smart_lab_restore_rehearsal_*` schema, verifies all row counts, records the result, and removes only that rehearsal schema. The shrink phase refuses to start unless the dump hashes and restore proof are valid.

Flyway remains disabled during ordinary application startup. The migration wrapper enables it only for its child Maven process and targets the approved expand or shrink version.
