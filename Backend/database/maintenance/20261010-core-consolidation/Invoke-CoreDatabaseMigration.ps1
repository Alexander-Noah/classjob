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

foreach ($file in @($manifest.files.full, $manifest.files.retired)) {
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
    SMART_LAB_DB_URL = "jdbc:mysql://${DbHost}:$Port/$Database"
    SMART_LAB_DB_USERNAME = $User
    SMART_LAB_DB_PASSWORD = $env:MYSQL_PWD
}
$arguments = @(
    '-pl', 'legacy-app', '-DskipTests', 'spring-boot:run',
    "-Dspring-boot.run.arguments=--spring.flyway.target=$target --spring.main.web-application-type=none"
)
$process = Start-Process -FilePath $maven -ArgumentList $arguments -WorkingDirectory $backendRoot `
    -Environment $childEnvironment -NoNewWindow -Wait -PassThru
if ($process.ExitCode -ne 0) { throw "Flyway $Phase migration failed with exit code $($process.ExitCode)" }
