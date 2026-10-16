# 新核心库直接替换

2026-10-15 用户确认旧库没有业务数据，可直接换库。因此实际采用创建新的 `smart_lab_basic`，保留原 `smart_lab_recovered_20261015`，没有对原库执行缩表或重放旧迁移。

新库由独立 `db/core-bootstrap` Flyway 迁移创建，版本 `20261015.01`，包含 15 张核心表及迁移历史表，补齐基础关系外键。复制既有账号、角色、课表配置和任务；未复制退役模块。保持普通应用 Flyway 禁用，不能对新库启用旧 `db/migration` 升级链。

从 `Backend` 目录运行 `./start-core.ps1`。旧的 `database/recovery/Start-Recovered.ps1` 默认也已指向新库；加 `-UseRecoveredDatabase` 可显式连接保留的原重建库。两个入口均按提示读取 MySQL 密码，登录账号密码保持原样。

## 脚本

- `generate-core-bootstrap.cjs`：从历史重建结构提取 15 个核心表定义，移除设备冗余名称并补齐外键。已经应用的迁移文件不能随意重新生成覆盖；后续修改使用新版本。
- `Create-CoreReplacement.cjs`：新库必须不存在，源库除账号和课表初始化记录外必须没有业务数据。完整备份原库并复制核心初始化记录，初始化新库，验证 107 条 SELECT，再启动应用两次。凭据通过 `MYSQL_PWD` 环境传入，禁止在命令行传递。
- `Create-CoreReplacement.cjs --verify-existing`：依据已有部署报告和备份哈希，仅重跑已创建库的两次启动验证，不覆盖库或数据。
- `Smoke-CoreApi.cjs`：登录及 20 个核心只读接口验证；历史抓取任务错误与接口加载失败分别判断。
- `Verify-CoreWrites.cjs`：12 项核心事务写入验证，所有样例回滚并校验前后行数。
- `Verify-CoreForeignKeys.cjs`：检查新库全部外键孤儿。

实际结果见 `deployment-report.json`、`write-verification.json` 和 `foreign-key-verification.json`。备份及本地启动日志位于被忽略的 `.local-archives` 下，含账号哈希及其他私有配置，不能公开分享。原库本身也完整保留。

当前默认没有常驻测试后端进程；验证使用临时端口 18088，结束时仅关闭脚本自己启动的进程。
