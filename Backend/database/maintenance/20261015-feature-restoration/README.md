# 功能页面恢复维护工具

当前功能与验证结论见仓库 `docs/database/2026-10-15-feature-restoration.md`。

- `Restore-FeatureModules.cjs`：完整备份本地 `smart_lab_basic`，通过独立入口追加 22 张功能表，核对核心表行数不变。必须先完成后端打包，并通过进程环境设置 `MYSQL_PWD`；不在命令行传递密码。
- `Restore-DefaultPermissions.cjs`：只升级匹配旧默认权限的账号，保留自定义授权。
- `Verify-RestoredFeatures.cjs`：将现有库复制到随机隔离库，启动独立端口后端，验证查询、角色菜单、权限边界及功能写入；结束后删除该随机库。
- `Verify-RunningProject.cjs`：使用本地凭据检查现有前端和恢复功能查询接口，不创建业务数据。

空库初始化分两步：先运行现有核心库初始化 `.01`，再运行功能恢复工具迁移至 `.02`。普通应用启动不会自动执行本目录工具。

完整备份、验证克隆及后端日志位于 `.local-archives`，默认忽略提交。凭据、令牌及备份内容不应公开。公开 JSON 报告仅保留检查结果及计数。
