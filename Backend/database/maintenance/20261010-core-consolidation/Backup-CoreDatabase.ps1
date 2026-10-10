[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Database,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [Parameter(Mandatory)][string]$ArchiveRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$retiredTables = @(
    'ai_assistant_conversation', 'ai_assistant_user_config', 'approval_countersign',
    'consumable_inventory', 'consumable_stock_record', 'course_environment_request',
    'device_runtime_status', 'device_status_event', 'environment_template', 'host_static_asset',
    'iot_command_log', 'iot_device', 'iot_device_capability', 'iot_gateway_config',
    'iot_gateway_point', 'iot_telemetry_latest', 'lab_activation_code', 'lab_software',
    'lab_terminal', 'schedule_adjustment', 'student_client', 'teacher_host',
    'device_inventory_record', 'device_transfer_record', 'module_record',
    'timetable_crawler_config', 'timetable_academic_credential', 'timetable_crawler_task'
)

if ($Database -ne 'smart_lab_basic') { throw 'Database must be smart_lab_basic' }
if ([string]::IsNullOrWhiteSpace($env:MYSQL_PWD)) {
    throw 'MYSQL_PWD must be configured in the process environment'
}
if ([string]::IsNullOrWhiteSpace($ArchiveRoot)) { throw 'ArchiveRoot is required' }

$resolvedRoot = [System.IO.Path]::GetFullPath($ArchiveRoot)
New-Item -ItemType Directory -Path $resolvedRoot -Force | Out-Null
$archiveDirectory = Join-Path $resolvedRoot (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
New-Item -ItemType Directory -Path $archiveDirectory | Out-Null
$resolvedArchive = (Resolve-Path -LiteralPath $archiveDirectory).Path
if (-not $resolvedArchive.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Archive directory escaped ArchiveRoot'
}

$fullDump = Join-Path $resolvedArchive 'full.sql'
$retiredDump = Join-Path $resolvedArchive 'retired-tables.sql'
$mysqlCommon = @("--host=$DbHost", "--port=$Port", "--user=$User", '--batch', '--skip-column-names')

$fullArgs = @(
    "--host=$DbHost", "--port=$Port", "--user=$User",
    '--single-transaction', '--routines', '--triggers', '--hex-blob',
    "--result-file=$fullDump", $Database
)
& mysqldump @fullArgs
if ($LASTEXITCODE -ne 0) { throw "Full mysqldump failed with exit code $LASTEXITCODE" }

$tableListSql = "SELECT table_name FROM information_schema.tables WHERE table_schema='$Database' ORDER BY table_name"
$databaseTables = @(& mysql @mysqlCommon "--execute=$tableListSql")
if ($LASTEXITCODE -ne 0) { throw "Unable to list database tables (exit $LASTEXITCODE)" }
$existingRetiredTables = @($retiredTables | Where-Object { $databaseTables -contains $_ })

if ($existingRetiredTables.Count -gt 0) {
    $retiredArgs = @(
        "--host=$DbHost", "--port=$Port", "--user=$User",
        '--single-transaction', '--hex-blob', '--no-create-db',
        "--result-file=$retiredDump", $Database
    ) + $existingRetiredTables
    & mysqldump @retiredArgs
    if ($LASTEXITCODE -ne 0) { throw "Retired-table mysqldump failed with exit code $LASTEXITCODE" }
} else {
    Set-Content -LiteralPath $retiredDump -Value '-- No retired tables were present.' -Encoding utf8
}

$tableManifest = foreach ($table in $databaseTables) {
    if ($table -notmatch '^[A-Za-z0-9_]+$') { throw "Unsafe table name returned by MySQL: $table" }
    $hasIdSql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$Database' AND table_name='$table' AND column_name='id'"
    $hasId = [int]((& mysql @mysqlCommon "--execute=$hasIdSql") | Select-Object -First 1)
    $aggregateSql = if ($hasId -eq 1) {
        "SELECT COUNT(*), COALESCE(MIN(id), ''), COALESCE(MAX(id), '') FROM ``$Database``.``$table``"
    } else {
        "SELECT COUNT(*), '', '' FROM ``$Database``.``$table``"
    }
    $aggregate = ((& mysql @mysqlCommon "--execute=$aggregateSql") | Select-Object -First 1) -split "`t", 3
    if ($LASTEXITCODE -ne 0) { throw "Unable to inventory table $table (exit $LASTEXITCODE)" }
    [ordered]@{ name = $table; rowCount = [long]$aggregate[0]; minId = $aggregate[1]; maxId = $aggregate[2] }
}

$fullHash = (Get-FileHash -LiteralPath $fullDump -Algorithm SHA256).Hash.ToLowerInvariant()
$retiredHash = (Get-FileHash -LiteralPath $retiredDump -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$fullDump.sha256" -Value "$fullHash  full.sql" -Encoding ascii
Set-Content -LiteralPath "$retiredDump.sha256" -Value "$retiredHash  retired-tables.sql" -Encoding ascii

$manifest = [ordered]@{
    database = $Database
    createdAt = (Get-Date).ToUniversalTime().ToString('o')
    retiredTableScope = $retiredTables
    retiredTablesPresent = $existingRetiredTables
    files = [ordered]@{
        full = [ordered]@{ name = 'full.sql'; sha256 = $fullHash }
        retired = [ordered]@{ name = 'retired-tables.sql'; sha256 = $retiredHash }
    }
    tables = @($tableManifest)
}
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $resolvedArchive 'manifest.json') -Encoding utf8

# The archive path is intentionally the script's only success output.
$resolvedArchive
