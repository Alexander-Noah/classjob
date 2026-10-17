# Backup-CoreDatabase运维脚本；执行前确认参数、工作目录和目标环境，不能作为业务请求入口。
# 参数/局部变量包括：Database、DbHost、Port、User、ArchiveRoot、Path、Parent、Child。

[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Database,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [Parameter(Mandatory)][string]$ArchiveRoot
)

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

function Resolve-ArchiveRoot {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw 'ArchiveRoot must not be blank'
    }

    $absolutePath = [System.IO.Path]::GetFullPath($Path)
    New-Item -ItemType Directory -Force -Path $absolutePath | Out-Null
    return (Resolve-Path -LiteralPath $absolutePath).Path
}

function Assert-ChildPath {
    param(
        [string]$Parent,
        [string]$Child
    )

    $parentPath = [System.IO.Path]::GetFullPath($Parent).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $childPath = [System.IO.Path]::GetFullPath($Child)
    $prefix = $parentPath + [System.IO.Path]::DirectorySeparatorChar
    if (-not $childPath.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Archive output must be inside ArchiveRoot'
    }
    return $childPath
}

if ([string]::IsNullOrWhiteSpace($Database)) {
    throw 'Database must not be blank'
}
if ($Database -ne 'smart_lab_basic') {
    throw 'Database must be smart_lab_basic'
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

$archiveRootPath = Resolve-ArchiveRoot -Path $ArchiveRoot
$timestamp = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')
$archiveDirectory = Assert-ChildPath -Parent $archiveRootPath -Child (Join-Path $archiveRootPath "$timestamp-$([Guid]::NewGuid().ToString('N'))")
New-Item -ItemType Directory -Path $archiveDirectory | Out-Null

$fullDumpPath = Assert-ChildPath -Parent $archiveRootPath -Child (Join-Path $archiveDirectory 'full.sql')
$retiredDumpPath = Assert-ChildPath -Parent $archiveRootPath -Child (Join-Path $archiveDirectory 'retired-tables.sql')
$manifestPath = Assert-ChildPath -Parent $archiveRootPath -Child (Join-Path $archiveDirectory 'manifest.json')
$fullHashPath = Assert-ChildPath -Parent $archiveRootPath -Child (Join-Path $archiveDirectory 'full.sql.sha256')
$retiredHashPath = Assert-ChildPath -Parent $archiveRootPath -Child (Join-Path $archiveDirectory 'retired-tables.sql.sha256')

$commonDumpArguments = @(
    "--host=$DbHost",
    "--port=$Port",
    "--user=$User",
    '--single-transaction',
    '--routines',
    '--triggers',
    $Database
)

& mysqldump @commonDumpArguments "--result-file=$fullDumpPath"
if ($LASTEXITCODE -ne 0) {
    throw 'mysqldump failed while creating full.sql'
}

& mysqldump @commonDumpArguments @($retiredTables) "--result-file=$retiredDumpPath"
if ($LASTEXITCODE -ne 0) {
    throw 'mysqldump failed while creating retired-tables.sql'
}

$manifestQuery = @'
SELECT JSON_OBJECT(
  'database', DATABASE(),
  'retiredTables', JSON_ARRAYAGG(JSON_OBJECT(
    'name', table_name,
    'rowCount', row_count,
    'minId', min_id,
    'maxId', max_id
  ))
) AS manifest
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
) AS retired_table_counts;
'@

$mysqlArguments = @(
    "--host=$DbHost",
    "--port=$Port",
    "--user=$User",
    "--database=$Database",
    '--batch',
    '--skip-column-names',
    '--raw',
    "--execute=$manifestQuery"
)
$manifestJson = & mysql @mysqlArguments
if ($LASTEXITCODE -ne 0) {
    throw 'mysql failed while collecting manifest row counts'
}
$manifestData = $manifestJson | ConvertFrom-Json

$fullHash = (Get-FileHash -LiteralPath $fullDumpPath -Algorithm SHA256).Hash
$retiredHash = (Get-FileHash -LiteralPath $retiredDumpPath -Algorithm SHA256).Hash
Set-Content -LiteralPath $fullHashPath -Value "$fullHash *full.sql" -Encoding ascii
Set-Content -LiteralPath $retiredHashPath -Value "$retiredHash *retired-tables.sql" -Encoding ascii

$manifest = [ordered]@{
    database = $Database
    createdAtUtc = [DateTime]::UtcNow.ToString('o')
    retiredTables = @($manifestData.retiredTables)
    files = [ordered]@{
        fullSql = [ordered]@{ name = 'full.sql'; sha256 = $fullHash }
        retiredTablesSql = [ordered]@{ name = 'retired-tables.sql'; sha256 = $retiredHash }
    }
}
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding utf8

Write-Output $archiveDirectory
