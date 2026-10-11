[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ArchiveDirectory,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root'
)

$ErrorActionPreference = 'Stop'

function Resolve-ArchiveDirectory {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw 'ArchiveDirectory must not be blank'
    }
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        throw 'ArchiveDirectory must exist'
    }
    return (Resolve-Path -LiteralPath $Path).Path
}

function Assert-ArchiveChild {
    param(
        [string]$Parent,
        [string]$Name
    )

    $parentPath = [System.IO.Path]::GetFullPath($Parent).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $childPath = [System.IO.Path]::GetFullPath((Join-Path $parentPath $Name))
    $prefix = $parentPath + [System.IO.Path]::DirectorySeparatorChar
    if (-not $childPath.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Archive output must remain inside ArchiveDirectory'
    }
    return $childPath
}

if ([string]::IsNullOrWhiteSpace($env:MYSQL_PWD)) {
    throw 'MYSQL_PWD must be configured in the process environment'
}
if ([string]::IsNullOrWhiteSpace($DbHost)) {
    throw 'DbHost must not be blank'
}
if ([string]::IsNullOrWhiteSpace($User)) {
    throw 'User must not be blank'
}
if ($Port -lt 1 -or $Port -gt 65535) {
    throw 'Port must be between 1 and 65535'
}

$archivePath = Resolve-ArchiveDirectory -Path $ArchiveDirectory
$fullDumpPath = Assert-ArchiveChild -Parent $archivePath -Name 'full.sql'
$manifestPath = Assert-ArchiveChild -Parent $archivePath -Name 'manifest.json'
$reportPath = Assert-ArchiveChild -Parent $archivePath -Name 'restore-rehearsal.json'
if (-not (Test-Path -LiteralPath $fullDumpPath -PathType Leaf)) {
    throw 'Archive is missing full.sql'
}
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw 'Archive is missing manifest.json'
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$restoreSchema = "smart_lab_restore_rehearsal_$([Guid]::NewGuid().ToString('N'))"
if ($restoreSchema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$') {
    throw 'Generated rehearsal schema did not match the required safety prefix'
}

$mysqlArguments = @(
    "--host=$DbHost",
    "--port=$Port",
    "--user=$User"
)
$report = [ordered]@{
    archiveDirectory = $archivePath
    schema = $restoreSchema
    startedAtUtc = [DateTime]::UtcNow.ToString('o')
    completedAtUtc = $null
    success = $false
    comparisons = @()
    error = $null
}

try {
    & mysql @mysqlArguments "--execute=CREATE DATABASE ``$restoreSchema`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
    if ($LASTEXITCODE -ne 0) {
        throw 'mysql failed while creating the restore rehearsal schema'
    }
    Get-Content -LiteralPath $fullDumpPath -Raw | & mysql @mysqlArguments "--database=$restoreSchema"
    if ($LASTEXITCODE -ne 0) {
        throw 'mysql failed while importing full.sql into the restore rehearsal schema'
    }

    $comparisonQuery = @'
SELECT JSON_ARRAYAGG(JSON_OBJECT(
  'name', table_name,
  'rowCount', row_count,
  'minId', min_id,
  'maxId', max_id
)) AS retiredTables
FROM (
  SELECT 'ai_assistant_conversation' AS table_name, COUNT(*) AS row_count, MIN(id) AS min_id, MAX(id) AS max_id FROM ai_assistant_conversation
  UNION ALL SELECT 'ai_assistant_user_config', COUNT(*), MIN(id), MAX(id) FROM ai_assistant_user_config
  UNION ALL SELECT 'approval_countersign', COUNT(*), MIN(id), MAX(id) FROM approval_countersign
  UNION ALL SELECT 'consumable_inventory', COUNT(*), MIN(id), MAX(id) FROM consumable_inventory
  UNION ALL SELECT 'consumable_stock_record', COUNT(*), MIN(id), MAX(id) FROM consumable_stock_record
  UNION ALL SELECT 'course_environment_request', COUNT(*), MIN(id), MAX(id) FROM course_environment_request
  UNION ALL SELECT 'device_runtime_status', COUNT(*), MIN(id), MAX(id) FROM device_runtime_status
  UNION ALL SELECT 'device_status_event', COUNT(*), MIN(id), MAX(id) FROM device_status_event
  UNION ALL SELECT 'environment_template', COUNT(*), MIN(id), MAX(id) FROM environment_template
  UNION ALL SELECT 'host_static_asset', COUNT(*), MIN(id), MAX(id) FROM host_static_asset
  UNION ALL SELECT 'iot_command_log', COUNT(*), MIN(id), MAX(id) FROM iot_command_log
  UNION ALL SELECT 'iot_device', COUNT(*), MIN(id), MAX(id) FROM iot_device
  UNION ALL SELECT 'iot_device_capability', COUNT(*), MIN(id), MAX(id) FROM iot_device_capability
  UNION ALL SELECT 'iot_gateway_config', COUNT(*), MIN(id), MAX(id) FROM iot_gateway_config
  UNION ALL SELECT 'iot_gateway_point', COUNT(*), MIN(id), MAX(id) FROM iot_gateway_point
  UNION ALL SELECT 'iot_telemetry_latest', COUNT(*), MIN(id), MAX(id) FROM iot_telemetry_latest
  UNION ALL SELECT 'lab_activation_code', COUNT(*), MIN(id), MAX(id) FROM lab_activation_code
  UNION ALL SELECT 'lab_software', COUNT(*), MIN(id), MAX(id) FROM lab_software
  UNION ALL SELECT 'lab_terminal', COUNT(*), MIN(id), MAX(id) FROM lab_terminal
  UNION ALL SELECT 'schedule_adjustment', COUNT(*), MIN(id), MAX(id) FROM schedule_adjustment
  UNION ALL SELECT 'student_client', COUNT(*), MIN(id), MAX(id) FROM student_client
  UNION ALL SELECT 'teacher_host', COUNT(*), MIN(id), MAX(id) FROM teacher_host
  UNION ALL SELECT 'device_inventory_record', COUNT(*), MIN(id), MAX(id) FROM device_inventory_record
  UNION ALL SELECT 'device_transfer_record', COUNT(*), MIN(id), MAX(id) FROM device_transfer_record
  UNION ALL SELECT 'module_record', COUNT(*), MIN(id), MAX(id) FROM module_record
  UNION ALL SELECT 'timetable_crawler_config', COUNT(*), MIN(id), MAX(id) FROM timetable_crawler_config
  UNION ALL SELECT 'timetable_academic_credential', COUNT(*), MIN(id), MAX(id) FROM timetable_academic_credential
  UNION ALL SELECT 'timetable_crawler_task', COUNT(*), MIN(id), MAX(id) FROM timetable_crawler_task
) AS restored_table_counts;
'@
    $comparisonJson = & mysql @mysqlArguments "--database=$restoreSchema" '--batch' '--skip-column-names' '--raw' "--execute=$comparisonQuery"
    if ($LASTEXITCODE -ne 0) {
        throw 'mysql failed while comparing restore rehearsal row counts'
    }
    $restored = $comparisonJson | ConvertFrom-Json
    $restoredByName = @{}
    foreach ($item in @($restored.retiredTables)) {
        $restoredByName[$item.name] = $item
    }

    $comparisons = @()
    foreach ($expected in @($manifest.retiredTables)) {
        $actual = $restoredByName[$expected.name]
        $matches = $null -ne $actual -and
            "$($expected.rowCount)" -eq "$($actual.rowCount)" -and
            "$($expected.minId)" -eq "$($actual.minId)" -and
            "$($expected.maxId)" -eq "$($actual.maxId)"
        $comparisons += [ordered]@{
            name = $expected.name
            rowCountMatches = $null -ne $actual -and "$($expected.rowCount)" -eq "$($actual.rowCount)"
            minIdMatches = $null -ne $actual -and "$($expected.minId)" -eq "$($actual.minId)"
            maxIdMatches = $null -ne $actual -and "$($expected.maxId)" -eq "$($actual.maxId)"
            matches = $matches
        }
    }
    $report.comparisons = $comparisons
    if (@($comparisons | Where-Object { -not $_.matches }).Count -gt 0) {
        throw 'Restore rehearsal did not match the backup manifest'
    }

    $report.success = $true
}
catch {
    $report.error = $_.Exception.Message
    throw
}
finally {
    try {
        $report.completedAtUtc = [DateTime]::UtcNow.ToString('o')
        $report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportPath -Encoding utf8
    }
    finally {
        if ($restoreSchema -notmatch '^smart_lab_restore_rehearsal_[0-9a-f]{32}$') {
            throw 'Refusing to drop a schema outside the rehearsal schema pattern'
        }
        & mysql @mysqlArguments "--execute=DROP DATABASE IF EXISTS ``$restoreSchema``"
        if ($LASTEXITCODE -ne 0) {
            throw 'mysql failed while dropping the restore rehearsal schema'
        }
    }
}
