# 核心数据库精简设计

**日期：** 2026-10-10
**状态：** 待用户复核
**目标：** 在不损伤核心业务功能和已有核心数据的前提下，将当前 39 张业务表精简为 15 张业务表，并使用 1 张 Flyway 迁移历史表管理后续结构演进。

## 1. 背景与现状

当前 `smart_lab_basic` 数据库共有 39 张业务表，其中 20 张为空表。主要问题如下：

- 物联网、终端接入、设备实时状态等尚未投入使用的模块创建了大量空表。
- 设备盘点、设备调拨分别建表，但二者都是设备生命周期事件。
- 课表抓取配置、凭据和任务分散在三张表中，配置与凭据具有相同的一对一生命周期。
- `device`、运行状态和物联网表重复保存实验室名称、实验室编号、设备名称等当前状态快照。
- `module_record` 同时承担审计事件、模块草稿和非核心业务数据，职责不清。
- 多个 Mapper 和 `DatabaseSchemaInitializer` 在应用运行期执行 DDL，结构不可审计，也可能让已清理的表重新出现。

本次优化以“表尽可能少，但不牺牲约束、查询能力和可维护性”为原则。不会把所有业务塞入一张万能 JSON 表，也不会直接在现有数据库上试错。

## 2. 已确认范围

### 2.1 必须保留的核心功能

- 登录、用户和角色权限。
- 实验室管理。
- 设备资产管理，包括盘点和调拨。
- 预约管理，包括手机扫码预约、审核、取消和课表冲突检测。
- 课表导入、查询、定时抓取和导入任务记录。
- 报修工单和维修记录。
- 通知发布、接收、阅读和归档。
- 使用记录。
- 仪表盘和统计分析中基于上述核心数据的功能。

核心功能的现有 HTTP 路径、主要请求字段和响应结构保持兼容。存储切换不要求前端调用方同步修改核心业务契约。

### 2.2 明确移除的非核心功能

- 物联网设备、能力、网关、点位、命令和遥测。
- 实验室终端接入、激活码、教师端和学生端在线状态。
- 独立设备实时状态和状态事件。
- AI 助手会话及个人 AI 配置。
- 耗材库存。
- 课程环境申请。
- 软件环境与环境模板。
- 调课。
- 基于 `module_record` 保存的通用模块草稿和系统设置页面。
- 当前无前端入口、无业务数据的通用加签流程。
- 依赖上述模块的页面、路由、接口、服务和自动建表逻辑。

被移除模块的现有数据必须先独立导出归档，再删除原表。归档 SQL 不重新导入目标库，除非执行回滚或历史取证。

## 3. 目标结构

目标库包含 15 张业务表和 1 张技术元数据表：

| 领域 | 表 | 职责 |
| --- | --- | --- |
| 身份权限 | `sys_user` | 用户身份、账号和人员资料 |
| 身份权限 | `role` | 角色定义 |
| 身份权限 | `user_role` | 用户与角色多对多关系 |
| 实验室资产 | `lab` | 实验室当前状态 |
| 实验室资产 | `device` | 设备资产当前状态 |
| 设备历史 | `device_event` | 盘点、调拨等设备生命周期事件 |
| 教学预约 | `class_timetable` | 导入后的课程课表 |
| 教学预约 | `lab_reservation` | 实验室预约及审核状态 |
| 课表接入 | `timetable_integration_config` | 抓取配置和加密凭据 |
| 课表接入 | `timetable_import_job` | 抓取/导入任务及结果 |
| 运行维护 | `repair_order` | 报修和维修闭环 |
| 运行维护 | `usage_record` | 人员、实验室实际使用记录 |
| 通知 | `notice` | 通知正文和发布状态 |
| 通知 | `notice_recipient` | 接收人及个人阅读/归档状态 |
| 审计 | `audit_event` | 核心业务操作审计 |
| 迁移元数据 | `flyway_schema_history` | Flyway 自动维护的迁移版本与校验值 |

`flyway_schema_history` 不存放业务数据，不允许业务 Mapper 查询或写入。

## 4. 关键表设计

### 4.1 `device_event`

合并原 `device_inventory_record` 和 `device_transfer_record`。核心字段：

- `id bigint`：主键。
- `device_id bigint`：关联 `device.id`，非空。
- `event_type varchar(32)`：`inventory` 或 `transfer`。
- `occurred_at datetime`：业务发生时间。
- `operator_user_id bigint null`：关联 `sys_user.id`，人员删除时置空。
- `operator_name varchar(100) null`：历史显示快照。
- `result_status varchar(40) null`：盘点或调拨结果。
- `before_state json null`：变更前的实验室、责任人、位置和状态。
- `after_state json null`：变更后的结构化状态。
- `remark varchar(500) null`：原因或说明。
- `created_at datetime`：入库时间。

索引至少包含 `(device_id, occurred_at)` 和 `(event_type, occurred_at)`。设备采用逻辑删除，因此设备事件不级联删除。

盘点和调拨的现有接口保持不变，Service 将请求转换为统一事件；查询响应继续映射为现有前端需要的盘点或调拨视图。

### 4.2 `timetable_integration_config`

合并原 `timetable_crawler_config` 和 `timetable_academic_credential`。核心字段：

- `id bigint`：主键。
- `config_key varchar(80)`：唯一业务键。
- `schedule_enabled tinyint(1)`：是否启用定时抓取。
- `cron_expression varchar(120)`：Cron 表达式。
- `semester varchar(40)`：学期。
- `semester_start_date date`：学期开始日期，不再使用字符串日期。
- `credential_username_cipher text null`：加密账号。
- `credential_password_cipher text null`：加密密码。
- `updated_at datetime`：更新时间。

凭据继续使用现有加密服务处理。日志、API 和异常信息不得输出密文或解密后的凭据。

### 4.3 `timetable_import_job`

由 `timetable_crawler_task` 迁移并规范命名。保留原任务 ID、触发方式、状态、范围、来源、成功账号、尝试次数、更新条数、起止时间、错误摘要、输出和日志路径。新增 `config_id` 外键指向 `timetable_integration_config.id`；历史记录无法确定配置时允许为空。

### 4.4 `audit_event`

承接 `module_record` 中 `module_name = 'operation-events'` 的有效审计数据。核心字段：

- `id bigint`：主键。
- `aggregate_type varchar(60)`：业务类型，例如 `reservation`、`repair`、`device`。
- `aggregate_id varchar(80) null`：业务记录 ID。
- `action varchar(60)`：动作。
- `result_status varchar(40) null`：动作结果。
- `actor_user_id bigint null`：关联 `sys_user.id`。
- `actor_name varchar(100) null`：历史显示快照。
- `detail_json json null`：动作附加信息，不保存密码、令牌和密钥。
- `occurred_at datetime`：发生时间。
- `created_at datetime`：入库时间。

索引至少包含 `(aggregate_type, aggregate_id, occurred_at)`、`(actor_user_id, occurred_at)` 和 `(occurred_at)`。审计事件只追加，不提供常规物理删除接口。

### 4.5 现有核心表规范化

- `device` 删除冗余 `lab_name`，实验室名称统一通过 `lab_id` 查询；保留设备自身编号和名称。
- `usage_record` 增加 `user_id`、`lab_id`、`source_type`、`source_id`。能够映射的旧数据回填外键；无法可靠匹配的数据保留原 `person`、`resource` 文本，避免信息丢失。
- `lab_reservation` 保留匿名申请人的姓名、部门和联系方式快照，因为扫码预约允许没有系统用户；登录用户预约仍优先写入 `applicant_user_id`。
- `notice_recipient` 保持独立，因为阅读、归档和删除状态属于“通知—用户”关系，不能安全放进 `notice` 的 JSON 字段。
- 当前状态表不复制实验室名称、用户名称等可通过外键获取的字段；历史事件表可保存必要的人类可读快照。

## 5. 旧表映射

| 当前表 | 处理方式 | 目标 |
| --- | --- | --- |
| `sys_user`、`role`、`user_role` | 保留并校验外键 | 原表 |
| `lab`、`device` | 保留并规范化 | 原表 |
| `device_inventory_record` | 转换为 `inventory` 事件 | `device_event` |
| `device_transfer_record` | 转换为 `transfer` 事件 | `device_event` |
| `class_timetable`、`lab_reservation` | 保留 | 原表 |
| `timetable_crawler_config` | 合并配置 | `timetable_integration_config` |
| `timetable_academic_credential` | 按业务键合并密文 | `timetable_integration_config` |
| `timetable_crawler_task` | 保留 ID 后迁移 | `timetable_import_job` |
| `repair_order`、`usage_record` | 保留并规范化 | 原表 |
| `notice`、`notice_recipient` | 保留 | 原表 |
| `module_record` 的 `operation-events` | 解析并结构化迁移 | `audit_event` |
| `module_record` 的其他模块 | 独立归档后移除 | 归档 SQL |
| 所有非核心模块表 | 独立归档后移除 | 归档 SQL |

非核心表完整清单：

- `ai_assistant_conversation`
- `ai_assistant_user_config`
- `approval_countersign`
- `consumable_inventory`
- `consumable_stock_record`
- `course_environment_request`
- `device_runtime_status`
- `device_status_event`
- `environment_template`
- `host_static_asset`
- `iot_command_log`
- `iot_device`
- `iot_device_capability`
- `iot_gateway_config`
- `iot_gateway_point`
- `iot_telemetry_latest`
- `lab_activation_code`
- `lab_software`
- `lab_terminal`
- `schedule_adjustment`
- `student_client`
- `teacher_host`

此外，完成映射和验证后删除被替代的 `device_inventory_record`、`device_transfer_record`、`module_record`、`timetable_crawler_config`、`timetable_academic_credential` 和 `timetable_crawler_task`。

## 6. 迁移流程

迁移必须在维护窗口内执行，后端停止写入。

### 阶段 A：预检和备份

1. 验证数据库名称、MySQL 版本、字符集和当前应用版本。
2. 记录 39 张表的行数、主键最小值/最大值、关键唯一键数量和外键孤儿数量。
3. 使用 `mysqldump --single-transaction --routines --triggers` 生成完整备份。
4. 为所有待移除表生成独立归档 SQL。
5. 在临时数据库中实际恢复完整备份，确认备份可用。
6. 任一检查失败时终止迁移，不修改生产/本地正式库。

### 阶段 B：扩展和迁移

1. 引入 Flyway 基线，禁止 Mapper 和 ApplicationRunner 执行 DDL。
2. 创建四张新业务表并给 `usage_record` 增加关联字段。
3. 迁移设备事件、课表配置、课表任务和审计事件。
4. 回填能够可靠识别的 `usage_record.user_id` 和 `usage_record.lab_id`。
5. 比对源表与目标表行数、业务键和抽样内容。
6. 此阶段不删除旧表。

### 阶段 C：应用切换

1. 将核心 Mapper 和 Service 切换到目标表。
2. 保持核心 Controller 路径和 DTO/响应兼容。
3. 移除非核心前端路由、页面入口、API 调用和后端模块。
4. 运行后端测试、前端测试和数据库集成测试。
5. 启动完整应用并执行核心页面冒烟测试。

### 阶段 D：收缩和验收

1. 再次停止写入并确认迁移后没有旧表增量。
2. 执行最终备份检查点。
3. 按外键依赖顺序删除非核心表和已被替代的旧表。
4. 删除冗余列并补齐目标外键、唯一约束和索引。
5. 验证 15 张业务表和 `flyway_schema_history` 是唯一剩余表。
6. 重启应用，确认启动过程不重新创建旧表。

## 7. 错误处理与回滚

- 每个 Flyway 脚本只承担一个可验证的结构或数据迁移职责。
- 数据迁移 DML 在事务中执行；MySQL 隐式提交的 DDL 单独形成检查点。
- 脚本执行前验证前置表、列和约束状态，状态不符时明确失败，不使用静默忽略。
- 删除表前必须存在完整备份、独立归档和验证报告。
- 阶段 B 或 C 失败时，切回旧代码并继续使用旧表。
- 阶段 D 失败时，停止服务，从完整备份恢复；如只影响单个非核心表，可从独立归档恢复。
- 回滚后重新执行行数、外键、核心 API 和扫码预约检查，不能只以“服务能启动”作为恢复成功标准。

## 8. 测试与验收矩阵

### 8.1 数据库结构测试

- 表集合严格等于 15 张业务表加 `flyway_schema_history`。
- 不存在由业务 Mapper、Controller 或 ApplicationRunner 执行的 `CREATE TABLE`、`ALTER TABLE` 或 `DROP TABLE`。
- 所有目标外键无孤立记录。
- 用户名、实验室编号、设备编号和配置键等唯一约束无重复。
- 凭据字段仍为密文，日志中没有凭据内容。
- 常用列表、冲突检测和统计查询通过 `EXPLAIN` 使用预期索引。

### 8.2 核心后端回归

- 登录成功、失败、角色权限和越权拒绝。
- 实验室及设备的新增、查询、修改和逻辑删除。
- 设备盘点、设备调拨及历史查询，接口响应与迁移前兼容。
- 扫码预约、后台新增预约、通过、驳回、取消、容量冲突和课表冲突。
- 课表配置保存、加密凭据读取、手动导入、定时任务和任务历史。
- 报修创建、处理、完成以及维修记录查询。
- 通知发布、接收、已读、归档和删除。
- 使用记录新增、查询、状态更新，以及预约/课表合成记录。
- 仪表盘和统计接口基于核心表正常返回。

### 8.3 前端与真实环境验收

- 每个保留页面完成加载、筛选、详情、提交和状态流转。
- 手机通过局域网访问扫码预约，CORS、提交和结果提示正常。
- 后端日志没有核心接口 500、SQL 语法错误或缺表错误。
- 连续重启后端两次，数据库表集合和 Flyway 版本保持稳定。
- 从完整备份恢复到临时库并启动应用，验证恢复路径可用。

## 9. 成功标准

- 核心功能和核心 API 契约保持可用。
- 所有核心历史数据均保留，迁移报告不存在无法解释的行数差异。
- 数据库最终为 15 张业务表加 1 张迁移历史表。
- 非核心表、非核心代码入口和运行时建表逻辑全部移除。
- 业务 Mapper 和启动初始化器不修改数据库结构；结构变更只能由已审核的 Flyway 版本化迁移执行。
- 完整备份、独立归档、迁移报告和回滚说明可供后续维护人员使用。

## 10. 非目标

- 不重新设计核心页面视觉样式。
- 不改变扫码预约的公开访问方式。
- 不在本次工作中新增业务模块。
- 不为追求更少表数而取消角色多对多关系、通知接收关系或把核心业务全部改为 JSON 存储。
- 不直接删除任何尚未备份并验证可恢复的数据。
