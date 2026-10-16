param([string]$DbPassword, [int]$Port = 8080, [switch]$UseRecoveredDatabase)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrEmpty($DbPassword)) {
    $secure = Read-Host '请输入本机 MySQL root 密码' -AsSecureString
    $DbPassword = [System.Net.NetworkCredential]::new('', $secure).Password
}
$env:SMART_LAB_FLYWAY_ENABLED = 'false'
$database = if ($UseRecoveredDatabase) { 'smart_lab_recovered_20261015' } else { 'smart_lab_basic' }
& (Join-Path $PSScriptRoot '../../start-local.ps1') -DbPassword $DbPassword -DbUsername root -DbUrl "jdbc:mysql://127.0.0.1:3306/${database}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai" -Port $Port
