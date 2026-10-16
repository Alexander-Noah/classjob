# 从代码重建数据库

**2026-10-15 更新：已直接换为精简核心库 `smart_lab_basic`（15 张业务表 + Flyway 历史表）。原 37 表重建库完整保留。`Start-Recovered.ps1` 默认启动新核心库，传入 `-UseRecoveredDatabase` 才连接原重建库。也可使用 `../../start-core.ps1`。账号密码保持原样。详见 `../maintenance/20261015-recovered-consolidation/README.md`。以下重建文件与说明保留作历史及回退参考。**

目标：本机 MySQL 8.4 的 `smart_lab_recovered_20261015`。新建独立数据库，不覆盖其他库。

这是根据当前 `Backend` 重建的可运行表结构，不是原数据库备份。基础表的长度、默认值、索引由实体和 Mapper 推断；物联网、课表、审计等表优先采用代码中已有 DDL。未伪造历史用户、预约、资产、课表、通知和维修数据。

## 文件

- `01-schema.sql`：37 张表的完整建库 SQL，含当前版本字段、索引和已有 DDL 中的外键；数据库已存在时会报错。导入失败时必须停止检查，不要使用客户端的 `--force` 继续执行。
- `02-bootstrap.sql`：四种有效角色和一个恢复管理员，密码使用后端兼容的 PBKDF2-HMAC-SHA256（120000 次）哈希。仅执行一次。
- `03-smoke-check.sql`：实际业务写入验证，测试记录全部回滚。
- `04-recovered-snapshot.sql`：建库完成后通过 mysqldump 导出的实际快照，包含表结构、角色和恢复管理员哈希。恢复到另一个空库时可导入此文件；它不包含 CREATE DATABASE/USE，需要先选定目标空库。不要再重复导入 `01-schema.sql` 和 `02-bootstrap.sql`。
- `.local-admin-credentials.txt`：新管理员的本地登录信息，已通过本目录 `.gitignore` 排除。
- `generate-recovery.cjs`：根据当前本地代码重新生成建库文件，不连接或修改数据库。
- `validate-recovery.cjs`：静态核对插入字段和带表别名字段引用；不是完整 SQL 解析器。
- `check-mapper-queries.py`、`verify-mysql.cjs`：生成并在 MySQL 中 EXPLAIN 校验 Mapper SELECT 的默认动态分支。报告见 `mysql-query-verification.json`；不覆盖所有动态条件组合。

## 启动后端

安装 Java 17 和 Maven 并加入 PATH 后，在本目录执行：

```powershell
.\Start-Recovered.ps1
```

按提示输入 MySQL 密码。脚本调用项目原有 `start-local.ps1`，生成本地 JWT 和教务凭据密钥，并连接恢复数据库。

此库已经直接包含 2026-10-10 后的结构，必须保持 `SMART_LAB_FLYWAY_ENABLED=false`。不要对它再次执行旧升级或数据搬迁 SQL，也不要把它冒充已有 Flyway 历史的原库。

系统账号见本地登录信息文件，登录后修改密码。历史教务系统加密凭据需要重新填写。

## 验证与范围

已通过本机 MySQL 实际建表、126 条 Mapper SELECT 默认分支的 EXPLAIN 检查，以及实验室、设备、预约、维修、使用记录和耗材的事务写入验证；测试记录均已回滚。完整后端启动、HTTP 登录和业务流程仍需 Java/Maven 环境进行联调。旧 SQL 中的数据搬迁源表不会被创建，因为当前版本已使用合并后的新表。

`实验室数据清档` 中的两个 Excel 仍可用于后续补录资产和实验室信息，本次未自动导入，避免把规范要求误当作实际库存。原来的教师 CSV 也未自动导入旧密码。
