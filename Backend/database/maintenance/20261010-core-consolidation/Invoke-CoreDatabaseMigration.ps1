[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('Expand', 'Shrink')][string]$Phase,
    [Parameter(Mandatory)][string]$ArchiveDirectory,
    [string]$Database = 'smart_lab_basic',
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Database -ne 'smart_lab_basic') { throw 'Database must be smart_lab_basic' }
if ([string]::IsNullOrWhiteSpace($env:MYSQL_PWD)) {
    throw 'MYSQL_PWD must be configured in the process environment'
}
$archive = (Resolve-Path -LiteralPath $ArchiveDirectory).Path
$manifest = Get-Content -LiteralPath (Join-Path $archive 'manifest.json') -Raw | ConvertFrom-Json

foreach ($file in @($manifest.files.full, $manifest.files.retired) + @($manifest.files.tableArchives)) {
    $path = Join-Path $archive $file.name
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $file.sha256) { throw "$($file.name) SHA-256 does not match manifest.json" }
}

$target = '20261010.2'
if ($Phase -eq 'Shrink') {
    $target = '20261010.3'
    $rehearsalPath = Join-Path $archive 'restore-rehearsal.json'
    if (-not (Test-Path -LiteralPath $rehearsalPath)) { throw 'restore-rehearsal.json is required before Shrink' }
    $rehearsal = Get-Content -LiteralPath $rehearsalPath -Raw | ConvertFrom-Json
    if ($rehearsal.success -ne $true) { throw 'Restore rehearsal must report success before Shrink' }
}

$backendRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..\..')).Path
$maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
$childEnvironment = @{
    SMART_LAB_FLYWAY_ENABLED = 'true'
    SMART_LAB_FLYWAY_TARGET = $target
    SPRING_FLYWAY_ENABLED = 'true'
    SPRING_FLYWAY_TARGET = $target
    SPRING_MAIN_WEB_APPLICATION_TYPE = 'none'
    SMART_LAB_DB_URL = "jdbc:mysql://${DbHost}:$Port/$Database"
    SMART_LAB_DB_USERNAME = $User
    SMART_LAB_DB_PASSWORD = $env:MYSQL_PWD
}
$arguments = @(
    '-pl', 'legacy-app', '-DskipTests',
    '-Dspring-boot.run.main-class=org.example.backend.database.CoreDatabaseMigrationRunner',
    'spring-boot:run'
)
$process = Start-Process -FilePath $maven -ArgumentList $arguments -WorkingDirectory $backendRoot `
    -Environment $childEnvironment -NoNewWindow -Wait -PassThru
if ($process.ExitCode -ne 0) { throw "Flyway $Phase migration failed with exit code $($process.ExitCode)" }

$expectedVersion = if ($target -eq '20261010.2') { '20261010.02' } else { '20261010.03' }
$mysql = (Get-Command mysql -ErrorAction Stop).Source
$historyExistsSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$Database' AND table_name='flyway_schema_history'"
$historyExistsOutput = @(& $mysql "--host=$DbHost" "--port=$Port" "--user=$User" '--batch' '--skip-column-names' "--execute=$historyExistsSql")
$historyExistsExitCode = if (Test-Path variable:LASTEXITCODE) { $LASTEXITCODE } else { 0 }
$historyExists = $historyExistsOutput | Select-Object -First 1
if ($historyExistsExitCode -ne 0 -or [int]$historyExists -ne 1) {
    throw "Flyway $Phase exited successfully but did not create flyway_schema_history"
}
$versionSql = "SELECT COUNT(*) FROM ``$Database``.flyway_schema_history WHERE version='$expectedVersion' AND success=1"
$versionCheckOutput = @(& $mysql "--host=$DbHost" "--port=$Port" "--user=$User" '--batch' '--skip-column-names' "--execute=$versionSql")
$versionCheckExitCode = if (Test-Path variable:LASTEXITCODE) { $LASTEXITCODE } else { 0 }
$versionCheck = $versionCheckOutput | Select-Object -First 1
if ($versionCheckExitCode -ne 0 -or [int]$versionCheck -ne 1) {
    throw "Flyway $Phase exited successfully but did not record version $expectedVersion"
}
