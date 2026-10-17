# 维护运维脚本；执行前确认参数、工作目录和目标环境，不能作为业务请求入口。
# 参数/局部变量包括：Condition、Message、Action、Expected。

#requires -Version 7.0
# Offline behavioral tests. mysql is a function stub; no database is contacted.
$ErrorActionPreference = 'Stop'
$testRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('smart-lab-guards-' + [guid]::NewGuid().ToString('N'))
$previousPassword = $env:MYSQL_PWD
$global:smartLabGuardBadComparison = $false
$global:smartLabGuardMysqlCalls = 0
# mysql的处理逻辑。
function mysql {
    $global:smartLabGuardMysqlCalls++
    $global:LASTEXITCODE = 0
    if (($args -join ' ') -match 'SELECT JSON_ARRAYAGG') {
        $count = if ($global:smartLabGuardBadComparison) { 99 } else { 2 }
        '[{"name":"module_record","rowCount":' + $count + ',"minId":1,"maxId":2}]'
    }
}
# 断言That。
function Assert-That([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}
# Expect失败的处理逻辑。
function Expect-Failure([scriptblock]$Action, [string]$Expected) {
    $caught = $false
    try { & $Action } catch {
        $caught = $true
        Assert-That ($_.Exception.Message -like "*$Expected*") ('Unexpected failure: ' + $_.Exception.Message)
    }
    Assert-That $caught ('Expected rejection: ' + $Expected)
}
try {
    New-Item -ItemType Directory -Path $testRoot | Out-Null
    Set-Content -LiteralPath (Join-Path $testRoot 'full.sql') -Value 'select 1;' -Encoding utf8
    Set-Content -LiteralPath (Join-Path $testRoot 'retired-tables.sql') -Value 'select 2;' -Encoding utf8
    $fullHash = (Get-FileHash (Join-Path $testRoot 'full.sql')).Hash
    $retiredHash = (Get-FileHash (Join-Path $testRoot 'retired-tables.sql')).Hash
    Set-Content (Join-Path $testRoot 'full.sql.sha256') "$fullHash *full.sql"
    Set-Content (Join-Path $testRoot 'retired-tables.sql.sha256') "$retiredHash *retired-tables.sql"
    $manifest = @{
        database = 'smart_lab_basic'; dbHost = '127.0.0.1'; port = 3306
        retiredTables = @(@{ name = 'module_record'; rowCount = 2; minId = 1; maxId = 2 })
        files = @{ fullSql = @{ sha256 = $fullHash }; retiredTablesSql = @{ sha256 = $retiredHash } }
    }
    $manifest | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $testRoot 'manifest.json')
    $env:MYSQL_PWD = 'offline-test-only'
    & (Join-Path $PSScriptRoot 'Test-CoreDatabaseBackup.ps1') -ArchiveDirectory $testRoot
    $report = Get-Content (Join-Path $testRoot 'restore-rehearsal.json') -Raw | ConvertFrom-Json
    Assert-That ($report.success -and @($report.comparisons).Count -eq 1 -and $report.comparisons[0].matches) 'Restored JSON array must produce a real comparison'
    Assert-That ($report.fullSqlSha256 -eq $fullHash) 'Report must bind the verified dump'
    $global:smartLabGuardBadComparison = $true
    Expect-Failure { & (Join-Path $PSScriptRoot 'Test-CoreDatabaseBackup.ps1') -ArchiveDirectory $testRoot } 'did not match'
    $report = Get-Content (Join-Path $testRoot 'restore-rehearsal.json') -Raw | ConvertFrom-Json
    Assert-That (-not $report.success) 'Mismatch must leave a failed report'
    foreach ($phase in @('Expand', 'Shrink')) {
        Expect-Failure { & (Join-Path $PSScriptRoot 'Invoke-CoreDatabaseMigration.ps1') -Phase $phase -ArchiveDirectory $testRoot } 'success: true'
    }
    $global:smartLabGuardBadComparison = $false
    & (Join-Path $PSScriptRoot 'Test-CoreDatabaseBackup.ps1') -ArchiveDirectory $testRoot
    Add-Content (Join-Path $testRoot 'full.sql') 'changed'
    foreach ($phase in @('Expand', 'Shrink')) {
        Expect-Failure { & (Join-Path $PSScriptRoot 'Invoke-CoreDatabaseMigration.ps1') -Phase $phase -ArchiveDirectory $testRoot } 'SHA-256'
    }
    $before = $global:smartLabGuardMysqlCalls
    Expect-Failure { & (Join-Path $PSScriptRoot 'Test-CoreDatabaseBackup.ps1') -ArchiveDirectory $testRoot } 'hashes do not match'
    Assert-That ($before -eq $global:smartLabGuardMysqlCalls) 'Corrupt backup must fail before any SQL'
    Write-Output 'PASS: restore comparisons, failed reports, both phase guards, hash binding and corrupt-backup preflight'
}
finally {
    $env:MYSQL_PWD = $previousPassword
    $resolvedRoot = [System.IO.Path]::GetFullPath($testRoot)
    $tempPrefix = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\') + '\smart-lab-guards-'
    if (-not $resolvedRoot.StartsWith($tempPrefix, [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe temporary test cleanup path' }
    if (Test-Path -LiteralPath $resolvedRoot) { Remove-Item -LiteralPath $resolvedRoot -Recurse -Force }
}

