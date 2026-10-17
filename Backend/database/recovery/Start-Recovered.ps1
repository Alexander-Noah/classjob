# Start-Recovered运维脚本；执行前确认参数、工作目录和目标环境，不能作为业务请求入口。
# 参数/局部变量包括：DbPassword、Port、UseRecoveredDatabase。

param([string]$DbPassword, [int]$Port = 8080, [switch]$UseRecoveredDatabase)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrEmpty($DbPassword)) {
    $secure = Read-Host '请输入本机 MySQL root 密码' -AsSecureString
    $DbPassword = [System.Net.NetworkCredential]::new('', $secure).Password
}
$env:SMART_LAB_FLYWAY_ENABLED = 'false'
$database = if ($UseRecoveredDatabase) { 'smart_lab_recovered_20261015' } else { 'smart_lab_basic' }
& (Join-Path $PSScriptRoot '../../start-local.ps1') -DbPassword $DbPassword -DbUsername root -DbUrl "jdbc:mysql://127.0.0.1:3306/${database}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai" -Port $Port
