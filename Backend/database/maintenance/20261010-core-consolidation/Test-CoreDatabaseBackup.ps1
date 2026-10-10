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

$schema = 'smart_lab_restore_rehearsal_' + [guid]::NewGuid().ToString('N')
if ($schema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$') { throw 'Unsafe rehearsal schema name' }
$mysqlCommon = @("--host=$DbHost", "--port=$Port", "--user=$User", '--batch', '--skip-column-names')
$reportPath = Join-Path $archive 'restore-rehearsal.json'
$report = [ordered]@{ success = $false; schema = $schema; verifiedAt = $null; tables = @(); error = $null }

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
        [ordered]@{ name = $name; expected = $expected; actual = $actual }
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
