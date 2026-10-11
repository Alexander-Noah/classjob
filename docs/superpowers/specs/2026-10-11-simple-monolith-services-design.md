# 规范化单体服务整理设计

日期：2026-10-11

## 背景

Backend 已合并为一个 Maven Spring Boot 工程，源码也已归入传统顶层包。但实验室、课表、预约三条业务链仍保留较多来源于旧模块架构的接口、转发 Service 和集中式适配 Bean。工程在部署结构上已经是单体，部分职责和依赖关系仍不够清晰，增加了维护成本。

## 目标

按照主流 Spring Boot 分层单体规范整理实验室、课表、预约三条链路，使职责、依赖方向、事务边界和异常处理清晰一致，同时保持现有 HTTP 接口、请求响应格式、数据库表、Mapper SQL、权限规则、并发控制和业务结果不变。

## 范围

本次只处理：

- 实验室目录与可用性查询。
- 教学课表与占用查询。
- 预约列表、创建、审核与冲突检查。
- 上述功能之间的业务协作和 Spring Bean 装配。

本次不处理：

- 设备网关目录中的 `GatewayAdapter` 等硬件协议抽象。
- Python、Web、数据库维护脚本及其他用户已有修改。
- API 路径、JSON 字段、数据库 schema、Mapper XML SQL。
- 新功能或新的通用框架。

## 选定方案

采用规范分层与必要抽象并存的方案。

- Controller 只负责协议适配、参数接收和响应，只依赖表达业务用例的 Service。
- Service 负责权限、事务和业务规则，并依赖 Mapper 或职责明确的其他业务 Service。
- Mapper 只负责持久化，不承载业务规则。
- 删除只为转发调用而存在的接口及适配 Bean。
- 跨功能逻辑继续放在 Service 层，不进入 Controller。
- 当存在多个实现、外部依赖隔离或稳定业务边界时，可以保留小型接口；接口是否保留由职责和可替换性决定，不以名称机械判断。
- 类型命名必须表达业务职责；不再沿用已经失去模块边界意义的 `Bridge`、`Module` 或 Adapter 套层。

不采用仅重命名方案，因为它不能改善职责和依赖关系；也不采用 Controller 直接调用 Mapper，因为这会破坏分层、业务规则、权限和事务边界。

## 目标结构

### 实验室链路

- `LabQueryController` 调用实验室查询 Service。
- 实验室查询 Service 直接使用 `LabRecordReadMapper` 和现有元数据 Service。
- 实验室可用性由普通 Service 提供，预约功能直接调用该 Service。

### 课表链路

- `TeachingScheduleQueryController` 调用课表查询 Service。
- 课表查询和占用检查 Service 直接使用 `TeachingScheduleReadMapper`。
- 预约冲突检查通过普通课表占用 Service 完成。

### 预约链路

- `ReservationController` 调用预约查询、创建和审核 Service。
- 预约持久化操作由普通 `ReservationPersistenceService` 封装 Mapper。
- 创建和审核 Service 直接依赖持久化、实验室和课表 Service。
- `@Transactional` 和 `Isolation.SERIALIZABLE` 保持在原有预约业务 Service 上。

### 装配

- 可由组件扫描发现的 Service 使用 `@Service`，不在配置类重复声明。
- `FeatureConfiguration` 只保留确实需要条件装配、组合装配或外部能力缺省实现的 Bean。
- 删除把一个业务接口转换为另一个近似接口的 lambda Bean。

## 数据流与行为约束

1. Controller 从现有安全上下文构造调用参数。
2. Service 执行权限、输入校验和业务规则。
3. Mapper 继续使用现有 XML 访问原表。
4. Service 将现有 View/Response 返回 Controller。

以下行为必须保持：

- 实验室管理员只能访问或操作其管理范围内的实验室。
- 教师和系统管理员的现有预约权限不变。
- 预约创建继续检查实验室容量、课表占用和预约重叠。
- 审核继续使用状态机、乐观条件更新和审核后权威回读。
- 预约创建继续使用 `SERIALIZABLE` 隔离级别。
- 能力不可用、数据异常和并发状态变化的错误语义保持不变。

## 测试策略

实施采用测试驱动方式：

1. 先扩展结构测试，要求三条目标链路遵循单向分层依赖，并且不存在无业务价值的转发接口或适配 Bean；确认测试按预期失败。
2. 分别迁移实验室、课表和预约链路，每次只做使测试通过的最小改动。
3. 每条链路运行其 Controller、Service、Mapper XML 和事务测试。
4. 最后运行 `mvn.cmd -f Backend/pom.xml clean test`，预期 104 个现有测试加新增结构测试全部通过；本地条件不足时仅允许既有两个 MySQL 集成测试跳过。
5. 运行包路径、Mapper namespace 和 `git diff --check` 检查。

## 完成标准

- 三条目标链路遵循清楚的 `Controller → Service → Mapper` 依赖方向，各层职责明确。
- 目标链路中不再出现只起转发作用的接口、Adapter 或 Bridge 抽象；确有可替换性或稳定边界的接口可以保留。
- `FeatureConfiguration` 只保留有明确装配价值的配置，不承担普通业务实现。
- 权限校验和业务规则位于 Service，事务边界位于业务 Service，持久化细节位于 Mapper 或持久化 Service。
- 外部接口、持久化结构和业务结果无变化。
- 完整测试与静态结构检查通过。
