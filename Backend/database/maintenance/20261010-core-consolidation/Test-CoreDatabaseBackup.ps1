[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ArchiveDirectory,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:MYSQL_PWD)) {
    throw 'MYSQL_PWD must be configured in the process environment'
}
$archive = (Resolve-Path -LiteralPath $ArchiveDirectory).Path
$manifestPath = Join-Path $archive 'manifest.json'
$fullDump = Join-Path $archive 'full.sql'
if (-not (Test-Path -LiteralPath $manifestPath) -or -not (Test-Path -LiteralPath $fullDump)) {
    throw 'Archive must contain manifest.json and full.sql'
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$actualHash = (Get-FileHash -LiteralPath $fullDump -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualHash -ne $manifest.files.full.sha256) { throw 'full.sql SHA-256 does not match manifest.json' }
foreach ($file in @($manifest.files.retired) + @($manifest.files.tableArchives)) {
    $path = Join-Path $archive $file.name
    if (-not (Test-Path -LiteralPath $path)) { throw "Archive is missing $($file.name)" }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $file.sha256) { throw "$($file.name) SHA-256 does not match manifest.json" }
}

$schema = 'smart_lab_restore_rehearsal_' + [guid]::NewGuid().ToString('N')
if ($schema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$') { throw 'Unsafe rehearsal schema name' }
$mysqlCommon = @("--host=$DbHost", "--port=$Port", "--user=$User", '--batch', '--skip-column-names')
$reportPath = Join-Path $archive 'restore-rehearsal.json'
$report = [ordered]@{ success = $false; schema = $schema; verifiedAt = $null; tables = @(); error = $null }

function Get-ConstraintSignature {
    param([string]$SchemaName, [string]$TableName)

    $constraintSql = @"
SELECT tc.constraint_name,
       tc.constraint_type,
       COALESCE(kcu.column_name, ''),
       COALESCE(kcu.referenced_table_name, ''),
       COALESCE(kcu.referenced_column_name, ''),
       COALESCE(kcu.ordinal_position, 0)
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.key_column_usage kcu
  ON kcu.constraint_schema = tc.constraint_schema
 AND kcu.table_name = tc.table_name
 AND kcu.constraint_name = tc.constraint_name
WHERE tc.table_schema = '$SchemaName'
  AND tc.table_name = '$TableName'
ORDER BY tc.constraint_name, kcu.ordinal_position
"@
    $rows = @(& mysql @mysqlCommon "--execute=$constraintSql")
    if ($LASTEXITCODE -ne 0) { throw "Unable to verify constraints for $TableName (exit $LASTEXITCODE)" }
    @($rows | ForEach-Object { [string]$_ })
}

try {
    & mysql @mysqlCommon "--execute=CREATE DATABASE ``$schema`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
    if ($LASTEXITCODE -ne 0) { throw "Unable to create rehearsal schema (exit $LASTEXITCODE)" }

    $sourcePath = $fullDump.Replace('\', '/')
    & mysql @mysqlCommon $schema "--execute=source $sourcePath"
    if ($LASTEXITCODE -ne 0) { throw "Unable to restore full.sql (exit $LASTEXITCODE)" }

    $tableResults = foreach ($table in $manifest.tables) {
        $name = [string]$table.name
        if ($name -notmatch '^[A-Za-z0-9_]+$') { throw "Unsafe manifest table name: $name" }
        $actual = [long]((& mysql @mysqlCommon "--execute=SELECT COUNT(*) FROM ``$schema``.``$name``") | Select-Object -First 1)
        if ($LASTEXITCODE -ne 0) { throw "Unable to count restored table $name (exit $LASTEXITCODE)" }
        $expected = [long]$table.rowCount
        if ($actual -ne $expected) { throw "Row count mismatch for ${name}: expected $expected, actual $actual" }
        $actualKeys = @(Get-ConstraintSignature -SchemaName $schema -TableName $name)
        $expectedKeys = @($table.keys | ForEach-Object {
                "$($_.name)`t$($_.type)`t$($_.column)`t$($_.referencedTable)`t$($_.referencedColumn)`t$($_.ordinal)"
            })
        if (($actualKeys -join "`n") -ne ($expectedKeys -join "`n")) {
            throw "Constraint mismatch for $name"
        }
        [ordered]@{ name = $name; expected = $expected; actual = $actual; keysVerified = $true; keyCount = $actualKeys.Count }
    }
    $report.success = $true
    $report.verifiedAt = (Get-Date).ToUniversalTime().ToString('o')
    $report.tables = @($tableResults)
} catch {
    $report.error = $_.Exception.Message
    throw
} finally {
    $report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $reportPath -Encoding utf8
    if ($schema -match '^smart_lab_restore_rehearsal_[0-9a-f]{32}$') {
        & mysql @mysqlCommon "--execute=DROP DATABASE IF EXISTS ``$schema``" | Out-Null
    }
}

$reportPath
