# Invoke-CoreDatabaseMigration运维脚本；执行前确认参数、工作目录和目标环境，不能作为业务请求入口。
# 参数/局部变量包括：Phase、ArchiveDirectory、DbHost、Port、User、Path、Parent、Name、HashFile、ArchivePath。

#requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('Expand', 'Shrink')][string]$Phase,
    [Parameter(Mandatory)][string]$ArchiveDirectory,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root'
)

$ErrorActionPreference = 'Stop'

# 解析归档目录。
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

# 断言归档Child。
function Assert-ArchiveChild {
    param(
        [string]$Parent,
        [string]$Name
    )

    $parentPath = [System.IO.Path]::GetFullPath($Parent).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $childPath = [System.IO.Path]::GetFullPath((Join-Path $parentPath $Name))
    $prefix = $parentPath + [System.IO.Path]::DirectorySeparatorChar
    if (-not $childPath.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Archive input must remain inside ArchiveDirectory'
    }
    return $childPath
}

# 获取预期Hash。
function Get-ExpectedHash {
    param([string]$HashFile)

    $line = Get-Content -LiteralPath $HashFile -Raw
    if ($line -notmatch '^([A-Fa-f0-9]{64})\s+\*?[^\r\n]+') {
        throw "Invalid SHA-256 file: $HashFile"
    }
    return $Matches[1].ToUpperInvariant()
}

# 断言DumpHashes匹配。
function Assert-DumpHashesMatch {
    param([string]$ArchivePath)

    foreach ($name in @('full.sql', 'retired-tables.sql')) {
        $dumpPath = Assert-ArchiveChild -Parent $ArchivePath -Name $name
        $hashPath = Assert-ArchiveChild -Parent $ArchivePath -Name "$name.sha256"
        if (-not (Test-Path -LiteralPath $dumpPath -PathType Leaf) -or -not (Test-Path -LiteralPath $hashPath -PathType Leaf)) {
            throw "Shrink requires $name and $name.sha256 in ArchiveDirectory"
        }
        $actualHash = (Get-FileHash -LiteralPath $dumpPath -Algorithm SHA256).Hash.ToUpperInvariant()
        $expectedHash = Get-ExpectedHash -HashFile $hashPath
        if ($actualHash -ne $expectedHash) {
            throw "Shrink refused because $name does not match its SHA-256 file"
        }
    }
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
$target = if ($Phase -eq 'Expand') { '20261010.2' } else { '20261010.3' }
if ($Phase -eq 'Shrink') {
    Assert-DumpHashesMatch -ArchivePath $archivePath
    $rehearsalPath = Assert-ArchiveChild -Parent $archivePath -Name 'restore-rehearsal.json'
    if (-not (Test-Path -LiteralPath $rehearsalPath -PathType Leaf)) {
        throw 'Shrink requires restore-rehearsal.json'
    }
    $rehearsal = Get-Content -LiteralPath $rehearsalPath -Raw | ConvertFrom-Json
    if ($rehearsal.success -ne $true) {
        throw 'Shrink requires restore-rehearsal.json to report success: true'
    }
}

$backendRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..\..')).Path
$maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
$processInfo = [System.Diagnostics.ProcessStartInfo]::new()
$processInfo.FileName = $maven
$processInfo.WorkingDirectory = $backendRoot
$processInfo.UseShellExecute = $false
$processInfo.ArgumentList.Add("-Dspring-boot.run.arguments=--spring.main.web-application-type=none --spring.flyway.target=$target")
$processInfo.ArgumentList.Add('spring-boot:run')

# These settings exist only in the Maven child process and are never written to disk.
$processInfo.Environment['SMART_LAB_FLYWAY_ENABLED'] = 'true'
$processInfo.Environment['SMART_LAB_DB_URL'] = "jdbc:mysql://{0}:{1}/smart_lab_basic" -f $DbHost, $Port
$processInfo.Environment['SMART_LAB_DB_USERNAME'] = $User
$processInfo.Environment['SMART_LAB_DB_PASSWORD'] = $env:MYSQL_PWD

$process = [System.Diagnostics.Process]::Start($processInfo)
$process.WaitForExit()
if ($process.ExitCode -ne 0) {
    throw "Flyway $Phase phase failed with exit code $($process.ExitCode)"
}
