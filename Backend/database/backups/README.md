# 数据库加密备份

`20261015/smart_lab_basic-20261015.sql.enc` 是当前 `smart_lab_basic` 的完整 MySQL 备份，包含全部表结构、数据、账号密码哈希、角色权限及数据库中保存的配置凭据。未做脱敏。

备份使用 AES-256-GCM 加密、scrypt 派生密钥，每份备份有独立随机盐和 IV。解密密码由维护者单独提供，仓库不保存密码，也不保存明文 SQL。本机 MySQL 连接密码和应用本地密钥文件不属于数据库备份，不在其中。

`20261015/manifest.json` 记录加密文件的 SHA-256、表数量、各表行数及恢复验证结果。导出使用 `--single-transaction --routines --events --triggers --hex-blob`。备份已解密后导入独立临时数据库，核对全部表行数，验证后临时库已移除。

## 解密

需要 Node.js 20 或以上。在仓库根目录运行 PowerShell，通过交互方式输入单独提供的备份密码：

```powershell
New-Item -ItemType Directory -Force .local | Out-Null
$backupSecret = Read-Host '数据库备份解密密码' -AsSecureString
$backupCredential = [System.Management.Automation.PSCredential]::new('backup', $backupSecret)
try {
    $env:DB_BACKUP_PASSWORD = $backupCredential.GetNetworkCredential().Password
    node Backend/database/backups/Decrypt-DatabaseBackup.cjs Backend/database/backups/20261015/smart_lab_basic-20261015.sql.enc .local/smart_lab_basic-restored.sql
} finally {
    Remove-Item Env:DB_BACKUP_PASSWORD -ErrorAction SilentlyContinue
    $backupCredential = $null
    $backupSecret = $null
}
```

解密脚本在写出文件前校验认证标签；密码错误或文件被篡改时不输出 SQL。为防止覆盖，输出文件必须尚不存在。`.local/` 和本目录内的明文 `*.sql` 已被 Git 忽略。

## 恢复到独立数据库

先在 MySQL 中创建新的空数据库，例如 `smart_lab_restore`。使用 MySQL 客户端交互连接并输入数据库连接密码：

```powershell
mysql --host=127.0.0.1 --user=root --password --default-character-set=utf8mb4 smart_lab_restore
```

然后在 MySQL 提示符中执行（请替换为实际 SQL 的绝对路径）：

```sql
SOURCE C:/path/to/repository/.local/smart_lab_basic-restored.sql;
```

备份不包含 `CREATE DATABASE` 或 `USE`，将恢复到客户端选定的数据库。文件包含建表及数据语句，恢复前应确认目标为空库。先核对恢复数据，再将应用数据库连接指向该库。

教务登录凭据的密文依赖原应用的本地加密密钥。如果在另一台机器恢复且未配置相同的应用密钥，需要在页面重新保存教务账号密码。
