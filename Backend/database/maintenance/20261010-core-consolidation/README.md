# Core database consolidation operator runbook

Use PowerShell 7.0 or newer. `Invoke-CoreDatabaseMigration.ps1` relies on
`.NET ProcessStartInfo.ArgumentList`, and the operator command below uses the
PowerShell 7 `Read-Host -MaskInput` parameter. Run these commands from this
directory after verifying that `mysql`,
`mysqldump`, Maven, and the intended MySQL server are available. The password is
kept only in the current PowerShell process through `MYSQL_PWD`; none of the
scripts accept a password argument or write it to an archive.

```powershell
$env:MYSQL_PWD = Read-Host 'MySQL password' -MaskInput
$archive = ./Backup-CoreDatabase.ps1 -Database smart_lab_basic -ArchiveRoot 'E:\smart-lab-db-archives'
./Test-CoreDatabaseBackup.ps1 -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Expand -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Shrink -ArchiveDirectory $archive
Remove-Item Env:MYSQL_PWD
```

`Backup-CoreDatabase.ps1` emits only the created archive directory on its success
pipeline output. Each archive contains `full.sql`, `retired-tables.sql`,
`manifest.json`, and SHA-256 files. It accepts only `smart_lab_basic` and creates
all output under the supplied archive root.

`Test-CoreDatabaseBackup.ps1` imports `full.sql` into a unique schema beginning
with `smart_lab_restore_rehearsal_`, compares the retired-table counts and ID
ranges with `manifest.json`, writes `restore-rehearsal.json`, and drops only that
generated schema in its `finally` block.

`Invoke-CoreDatabaseMigration.ps1` uses Flyway target `20261010.2` for `Expand`
and target `20261010.3` for `Shrink`. Shrink verifies both dump hashes and a
successful `restore-rehearsal.json` before starting Flyway. The script provides
`SMART_LAB_FLYWAY_ENABLED=true` and database credentials only to the Maven child
process; it does not modify the caller's environment.
