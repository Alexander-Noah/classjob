<div align="center">

  <img src="image/login-brand-transparent.png" alt="智云实验室 Logo" width="180" />

  <h1>智云实验室 (Zhiyun Laboratory)</h1>

  <p><strong>基于数据驱动的智慧实验室全流程协同管控平台</strong></p>
  <p>连接空间资源、教学排课、环境准备、设备运维与物联感知，实现全生命周期业务闭环。</p>

  <p><em>数智协同，全域感知，运维如流。</em></p>

  <p>
    <b>中文</b> | <a href="README.en.md">English</a>
  </p>

  <p>
    <a href="https://github.com/Alexander-Noah/classjob/actions/workflows/ci.yml?query=branch%3Amain">
      <img src="https://img.shields.io/github/actions/workflow/status/Alexander-Noah/classjob/ci.yml?branch=main&amp;label=CI&amp;logo=github" alt="main 分支整体 CI 状态" />
    </a>
    <a href="https://github.com/Alexander-Noah/classjob/actions/workflows/ci.yml?query=branch%3Amain">
      <img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2FAlexander-Noah%2Fclassjob%2Fcoverage-badges%2Fcoverage.json&amp;logo=github" alt="最近一次成功 main CI 的实际行覆盖率" />
    </a>
    <a href="https://app.codecov.io/github/Alexander-Noah/classjob"><img src="https://codecov.io/gh/Alexander-Noah/classjob/branch/main/graph/badge.svg" alt="Codecov coverage (including partial-line penalties)" /></a>
    <a href="https://spring.io/projects/spring-boot">
      <img src="https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot" />
    </a>
    <a href="https://vuejs.org/">
      <img src="https://img.shields.io/badge/Vue-3.x-4FC08D?logo=vuedotjs&logoColor=white" alt="Vue 3" />
    </a>
    <a href="https://tauri.app/">
      <img src="https://img.shields.io/badge/Tauri-2.x-24C8DB?logo=tauri&logoColor=white" alt="Tauri" />
    </a>
    <a href="https://www.mysql.com/">
      <img src="https://img.shields.io/badge/MySQL-8.0%2B-4479A1?logo=mysql&logoColor=white" alt="MySQL" />
    </a>
  </p>

</div>

---

## 目录

- [当前能力](#当前能力)
- [它如何工作](#它如何工作)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [环境要求](#环境要求)
- [快速上手](#快速上手)
- [客户端如何连上本机 API](#客户端如何连上本机-api)
- [机房一仓双端与终端管控](#机房一仓双端与终端管控)
- [开启真实语音与 AI 智能体](#开启真实语音与-ai-智能体)
- [本机开发后端](#本机开发后端)
- [质量检查与代码规范](#质量检查与代码规范)
- [HTTP 与 WebSocket 通信](#http-与-websocket-通信)
- [环境变量与核心配置](#环境变量与核心配置)
- [云上部署与可观测性](#云上部署与可观测性)
- [CI 与自动化质量保障](#ci-与自动化质量保障)
- [相关入口与参考文档](#相关入口与参考文档)

---

## 当前能力

智云实验室针对高校与科研院所机房、实验室运维繁杂、软硬件脱节、排课协同低效等痛点，构建了涵盖**资源、排课、环境、报修、数据**五大维度的全闭环管控能力：

- 🏢 **基础资源全生命周期管控**
  - **空间与台账管理**：实验室容量、布局、责任人、开放状态多维归档。
  - **设备资产流转**：支持设备录入、批量 Excel 导入、资产码（二维码）生成打印、设备调拨与周期性盘点记录追溯。
  - **耗材库存预警**：出入库双向追踪，低库存自动标记与补给提醒。
- 📅 **教学预约与排课协同**
  - **课表智能采集导入**：内置 Python 爬虫与课表解析器，自动化同步教务排课数据。
  - **冲突检测与预约审批**：实验室空闲档期可视化呈现，支持预约申请、公开扫码预约和审批处理；教师角色当前停用。
  - **调课联动处理**：智能关联课程调整与实验场地重分配，避免教学冲突。
- 🧪 **实验环境与物联感知**
  - **课程环境预配置**：维护课程环境需求与准备状态；教师提报页面保留，教师角色当前停用。
  - **环境模板库沉淀**：维护课程环境模板与所需软件清单，供后续准备复用。
  - **IoT 物联感知控制**：联动智能灯光、空调温控、智能门禁、环境传感器与监控摄像头，支持一键场景切换。
- 🛠️ **故障报修与闭环运维**
  - **全链路工单流转**：异常一键上报 → 管理员智能派单 → 维修人员现场核验修复 → 闭环验收归档。
  - **动态通知公告**：系统重要事件、停电维护、预约结果实时消息下发。
- 🖥️ **一仓双端桌面机房监控（Tauri 2 + Rust）**
  - **教师端（控制网关）**：带界面本地值守，局域网 UDP 自动广播响应，集中监视学生机在线状态与性能指标，支持统一锁屏、消息广播、重启与关机控制。
  - **学生端（托盘守护）**：Windows 轻量级托盘程序，开机自启无感驻留，UDP 自动发现教师端，定时采集 CPU/内存/网络状态并静默上报。
- 🤖 **AI 助手与离线语音识别**
  - **通义千问企业 Agent**：集成 DashScope 大模型，提供实验室调度、报修指引与规则问答。
  - **Vosk 离线语音引擎**：内置轻量级中文声学模型，无需外网即可快速语音指令交互。
  - **WebSocket 全双工通信**：打字机流式回答，支持流式问答与语音消息交互。
- 📊 **运营数据统计与指标分析**
  - 空间利用率、设备故障频次、耗材损耗率多维度 ECharts 图表透视，辅助管理决策。

---

## 它如何工作

智云实验室采用 **B/S 管理端平台 + C/S 桌面管控终端 + 物联感知网关 + 大模型决策引擎** 的一体化分层协同架构：

```mermaid
flowchart TD
    subgraph clients["多端交互接入"]
        WebClient["Web 管理工作台 (Vue 3 + Element Plus)"]
        TeacherClient["机房教师端 (Tauri 2 + Vue 3)"]
        StudentClient["机房学生端守护进程 (Tauri 2 / Rust)"]
    end

    subgraph backend["业务中台 (Spring Boot 4)"]
        APIGateway["RESTful API & JWT 安全过滤"]
        WSHandler["WebSocket 流式推送服务"]
        CoreService["核心业务域<br/>(资源 / 排课 / 预约 / 报修 / 耗材)"]
        AIEngine["AI 助手服务<br/>(通义千问 Agent + Vosk 语音)"]
    end

    subgraph infrastructure["数据与感知层"]
        MySQL[("MySQL 核心业务库")]
        IoTGateway["IoT 硬件网关<br/>(灯光 / 空调 / 门禁 / 传感器)"]
        Prometheus["Prometheus + Grafana 监控体系"]
    end

    WebClient -->|HTTP / JSON| APIGateway
    WebClient <-->|WebSocket| WSHandler
    TeacherClient -->|HTTP 8080 业务 API| APIGateway
    StudentClient <-->|UDP 8888 发现 / HTTP 5000 与 5001| TeacherClient
    StudentClient -->|定时性能数据| TeacherClient
    APIGateway --> CoreService
    WSHandler <--> AIEngine
    CoreService --> MySQL
    CoreService --> IoTGateway
    APIGateway -.->|8081 Actuator 指标| Prometheus
```

### 业务闭环模型

```
[教学排课 / 课表同步] 
        ↓
[课程软件环境提报与确认] 
        ↓
[师生预约审批 / 场地分发] 
        ↓
[物联环境感知自动调节 / 桌面端开机上线] 
        ↓
[设备运行监测 / 故障报修流转] 
        ↓
[大数据分析利用率 / 智能辅助优化决策]
```

---

## 技术栈

| 领域 | 核心技术 / 框架 | 版本 / 说明 |
| :--- | :--- | :--- |
| **后端核心** | Spring Boot | `4.0.6` (MVC + 自动化配置) |
| **ORM / 数据存储** | MyBatis / HikariCP / MySQL | `MySQL 8.0+`，驼峰下划线智能映射 |
| **认证与安全** | JWT / PBKDF2 / Filter 过滤 | 无状态 Token 授权，接口细粒度鉴权与 XSS 防御 |
| **前端管理平台** | Vue 3 + Vite + Element Plus | Composition API、CSS Variables、响应式液态玻璃 UI |
| **数据可视化** | ECharts | 空间利用率、耗材消耗与设备生命周期统计 |
| **桌面客户端** | Tauri 2 + Rust + Naive UI | 一仓双端架构（教师控制端 + 学生端托盘守护） |
| **AI 与语音识别** | 通义千问 Agent + Vosk | DashScope 企业级大模型 + Vosk 离线轻量级中文语音模型 |
| **教务数据爬虫** | Python 3 + Requests + BeautifulSoup | 自动化课表拉取与入库工具 |
| **监控可观测性** | Prometheus + Grafana + Docker Compose | 独立 8081 管理端口，实时 JVM、HikariCP 与接口指标看板 |
| **质量门禁** | JaCoCo + Vitest + GitHub Actions | **96% 全量代码行覆盖率硬性门禁** |

---

## 仓库结构

```text
classjob/
├── .github/                     # GitHub Actions CI 工作流与测试汇总脚本
│   ├── workflows/ci.yml         # 前后端 96% 覆盖率验证门禁
│   └── scripts/coverage-summary.py
├── Backend/                     # 后端工程 (Spring Boot 4 + MyBatis)
│   ├── src/main/java/org/example/backend/
│   │   ├── config/              # Swagger、全局异常处理、IoT 配置等
│   │   ├── controller/          # 实验室、设备、预约、工单等 19+ 控制器
│   │   ├── entity/              # 数据库实体映射类
│   │   ├── mapper/              # MyBatis Mapper 接口
│   │   ├── security/            # JWT 服务、密码加盐与过滤器
│   │   ├── service/             # 业务服务层与实现
│   │   └── websocket/           # AI 助手 WebSocket 处理器
│   ├── src/main/resources/mapper/  # MyBatis SQL XML 映射文件
│   ├── pom.xml                  # Maven 依赖与 JaCoCo 插件配置
│   └── start-local.ps1          # 本地后端快速启动脚本
├── web/                         # 前端管理平台 (Vue 3 + Vite + Element Plus)
│   ├── src/
│   │   ├── api/                 # Axios 实例封装与通用模块服务
│   │   ├── components/          # 液态玻璃风格组件、通用业务表格/弹窗
│   │   ├── composables/         # 设备、实验室组合式逻辑 Hooks
│   │   ├── views/               # 工作台、实验室、设备、报修、AI助手等页面
│   │   └── utils/               # 表格导入导出、二维码生成、地址校验
│   ├── package.json             # 前端依赖配置
│   └── vite.config.js           # Vite 代理与构建配置
├── TvTa-ui-main/                # 桌面客户端工程 (Tauri 2 + Rust + Vue 3)
│   ├── src/                     # 教师端 UI 页面 (Naive UI)
│   ├── src-tauri/               # Rust 核心（教师端 UDP 网关 + 学生端后台托盘）
│   └── package.json             # 客户端打包与构建脚本
├── zhiyun-student-client/       # 独立学生端工程
├── shared/                      # 共用输入与地址校验
├── Python/                      # 教务课表工具脚本
├── vosk-model-small-cn-0.22/    # Vosk 中文轻量级离线语音识别模型
├── monitoring/                  # Prometheus + Grafana 容器化监控编排
│   ├── compose.yaml             # Docker Compose 配置文件
│   └── prometheus/              # Prometheus 采集规则配置
├── docs/                        # 系统架构图、数据库 ER 图与设计文档
├── scripts/                     # ER 图与文档自动生成工具
└── 项目架构说明.md              # 详尽系统架构指南
```

---

## 环境要求

在开始部署或开发前，请确保本地已安装好以下基础运行环境：

| 工具 / 运行时 | 推荐版本 | 用途 |
| :--- | :--- | :--- |
| **JDK** | `Java 17` 或更高 (如 Temurin 17) | 后端 Spring Boot 运行与编译 |
| **Node.js** | `Node.js 24`（CI 使用版本；Web 要求至少 20.19） | Web 前端与桌面客户端构建 |
| **包管理工具** | Web 使用 npm；桌面端使用 `pnpm 10.30.3` | 前端依赖管理 |
| **MySQL** | `MySQL 8.0` 或更高版本 | 业务持久化数据库 |
| **Rust** *(可选)* | 兼容当前 Tauri 依赖的稳定版 Rust（带 Cargo） | 仅在重新编译 Tauri 桌面端时需要 |
| **Python** *(可选)* | `Python 3.10+` | 仅在运行课表抓取爬虫时需要 |
| **Docker** *(可选)* | Docker Desktop (支持 Compose v2) | 运行 Prometheus + Grafana 监控服务 |

---

## 快速上手

### 1. 克隆仓库

```bash
git clone https://github.com/Alexander-Noah/classjob.git
cd classjob
```

### 2. 数据库准备

1. 打开本地 MySQL 客户端，创建数据库 `smart_lab_basic`：
   ```sql
   CREATE DATABASE IF NOT EXISTS smart_lab_basic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. 将 [核心库表结构](Backend/database/smart_lab_basic.sql) 导入该空数据库。此文件仅包含表结构，不含业务数据、角色或用户。脚本含 `DROP TABLE`，只能导入确认可重建的空库，不能覆盖现有业务数据库。恢复与迁移流程见 [数据库恢复说明](Backend/database/recovery/README.md)，其中的历史恢复账号不能作为生产初始账号。

### 3. 启动后端服务 (Backend)

从仓库根目录打开终端，按实际数据库设置环境变量。本地脚本会生成 JWT 和教务凭据密钥到被 Git 忽略的配置文件。

```powershell
cd Backend
# 使用实际开发数据库凭据，不使用文档中的通用口令
$env:SMART_LAB_DB_USERNAME = "你的数据库用户"
$env:SMART_LAB_DB_PASSWORD = "你的数据库密码"
./start-local.ps1
```

后端服务将在 `http://127.0.0.1:8080` 启动，健康检查使用管理端口 `http://127.0.0.1:8081/actuator/health`。

### 4. 启动 Web 管理平台 (Web)

另开终端，从仓库根目录执行：

```bash
cd web
npm ci
npm run dev
```

启动完成后，访问 Vite 打印的地址，通常为 `http://127.0.0.1:5173`；端口被占用时可能顺延。

### 5. 登录账号与验证码

仓库的核心库 SQL 不提供通用演示账号或默认密码。使用已初始化数据库中的有效账号；新环境需要先初始化角色和管理员，并为账号设置独立密码。

当前后端停用 teacher 与 maintenance 角色；有效管理角色为 systemAdmin、labAdmin 和 academic。不要使用旧文档中的 teacher1/repair1 或默认口令作为验收依据。Web 登录必须填写图片验证码，验证码单次有效，5 分钟过期，可点击图片刷新。验证码保存在单个后端 JVM 内，多实例部署需要共享存储或会话保持。

---

## 客户端如何连上本机 API

Web 管理前端通过内置的 Vite 反向代理平滑连通后端接口，桌面端通过局域网广播与直连 HTTP 进行双向通信。

### 1. Web 端连接后端

在开发环境中，`web/vite.config.js` 通过 `loadEnv` 读取环境变量，并配置 `/api` 代理。下例是行为示意，不是完整配置：

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: proxyTarget // 来自 loadEnv；默认 http://127.0.0.1:8080,
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

- 若后端运行在其他主机或端口，仅需在 `web/.env.development` 中修改 `VITE_API_PROXY_TARGET=http://你的IP:8080`。
- 生产环境建议通过 Nginx 进行反向代理转发。

---

## 机房一仓双端与终端管控

针对实验室计算机机房批量运维需求，`TvTa-ui-main` 实现了极具特色的双端协同通信协议：

```mermaid
sequenceDiagram
    autonumber
    participant Student as 机房学生端 (托盘守护)
    participant Teacher as 机房教师端 (本地网关)
    participant Cloud as 智云实验室后端

    Teacher->>Teacher: 启动本地网关 (监听 UDP 8888, HTTP 5000)
    Student->>Student: 启动并常驻 Windows 系统托盘
    Student->>Teacher: UDP 广播 "WHO_IS_TEACHER" (端口 8888)
    Teacher-->>Student: 响应 "I_AM_TEACHER" (携带教师端 IP)
    loop 默认每隔 3 秒上报（HTTP 守护模式）
        Student->>Teacher: POST http://教师端IP:5000/api/collect<br/>上报 CPU、内存、网络、IP、主机名
    end
    Note over Teacher,Cloud: 业务 API 使用后端 8080；HTTP 5000 是教师端局域网接收端口
    alt 教师执行批量控制 (例如锁屏/关机)
        Teacher->>Student: POST http://学生端IP:5001/api/action: {"action": "lock", "message": "上课集中注意力"}
        Student-->>Teacher: {"ok": true, "message": "accepted"}
    end
```

### 端口与通信协议

- **教师端（控制台）**：
  - `UDP 8888`：监听并响应局域网学生端的自动发现广播。
  - `HTTP 5000`：接收学生端 HTTP 守护模式上报的运行指标。
  - `WebSocket 8765`：另一种 LAN 学生连接模式，协议与 HTTP 守护模式不同。
- **学生端（静默守护）**：
  - `HTTP 5001`：提供控制指令接口，支持 `refresh`（刷新）、`message`（弹窗提醒）、`lock`（锁屏）、`reboot`（重启）、`shutdown`（关机）。
- **运行方式**：
  - 首次运行先在 `TvTa-ui-main` 执行 `pnpm install --frozen-lockfile`，再执行 `pnpm tauri dev`。
  - 学生端先在 `TvTa-ui-main` 执行 `pnpm build:student`，再运行 `src-tauri/target/release/zhiyun-student-client.exe`。
  - 独立学生工程及其配置见 [学生端说明](zhiyun-student-client/README.md)。本节说明源码模式，不表示当前环境已经打包客户端。

---

## 开启真实语音与 AI 智能体

系统集成了 **Vosk 离线语音识别** 与 **通义千问大模型 Agent**：

### 1. 配置阿里通义千问 API Key

通过环境变量 `DASHSCOPE_API_KEY` 注入密钥；不要将真实密钥写入仓库。对应的配置层级为：

```yaml
tongyi:
  agent:
    enabled: true
    api-key: ${DASHSCOPE_API_KEY:}
    model: qwen-plus
```

### 2. 离线中文语音识别 (Vosk)

离线识别使用轻量级中文声学模型 `vosk-model-small-cn-0.22/`：
- 默认模型路径为 `../vosk-model-small-cn-0.22/vosk-model-small-cn-0.22`，相对于后端工作目录；也可通过 `VOSK_MODEL_PATH` 指定已安装的模型目录。
- 前端通过 Web Audio API 采集音频分片，经由 WebSocket 流式传送到后端 Vosk 引擎进行实时转文字，音频识别可离线运行；大模型问答仍需要对应模型服务。

### 3. WebSocket 交互地址

- 接口端点：`ws://127.0.0.1:8080/ws/ai-assistant`
- 数据格式：支持发送 JSON 文本消息或 PCM 音频流分片，服务端采用流式事件逐步推流返回。

---

## 本机开发后端

后端采用 Controller、Service 与 Mapper 分层，代码调用链清晰可循：

```text
HTTP Controller  -->  Service Interface  -->  ServiceImpl  -->  MyBatis Mapper (XML)  -->  MySQL
```

### 核心开发规范

1. **统一返回封装**：普通业务 JSON 接口使用非泛型 `Result`；文件下载、代理和流式响应使用各自的 HTTP 响应类型：
   - 成功：`return Result.success("操作成功", data);`
   - 失败：`return Result.error("业务异常信息");`
2. **字段自动映射**：开启了 `map-underscore-to-camel-case: true`，数据库 `device_code` 会自动对应实体类中的 `deviceCode`。
3. **事务强一致性**：涉及资产流转、出入库、审批多表更新的操作，在 Service 方法上标注 `@Transactional`。
4. **日志追踪**：内置 `RequestLoggingFilter` 统一生成 `requestId`，自动记录请求的方法、URI 与处理耗时，异常由统一异常处理器记录。

---

## 质量检查与代码规范

CI 对前后端分别执行测试和行覆盖率检查，**每个模块的门槛均为至少 96%**。门槛是配置要求，实际结果以对应提交的 Actions 报告为准：

### 1. 后端测试与覆盖率检查

```powershell
cd Backend
# 执行默认测试并生成 JaCoCo 覆盖率报告
mvn -Pcoverage verify
```

测试报告生成路径：`Backend/target/site/jacoco/index.html`。本地命令默认不启用需要 MySQL 的专项集成测试；CI 会启用 `reservation.mysql.it` 与 `core.schema.mysql.it` 并连接临时 MySQL，具体参数见 [工作流](.github/workflows/ci.yml)。

### 2. 前端测试与覆盖率检查

```bash
cd web
# 运行生产构建
npm run build

# 执行 Vitest 测试套件并验证 96% 覆盖率门禁
npm run test:coverage
```

测试覆盖率产物：`web/coverage/`。

### 3. 安全防御与校验

- **XSS 防御**：AI 消息解析为受控排版节点，通过 Vue 文本节点显示；打印模板转义动态内容，图片地址使用白名单，并配置 CSP。详见 [HTML 注入与 XSS 修复说明](docs/HTML注入与XSS修复说明.md)。
- **输入校验**：前后端检查字段类型、长度、数值范围和请求结构。SQL 参数绑定与业务授权仍需独立执行；输入校验不能替代权限检查。详见 [输入校验规则](docs/输入校验规则与验证说明.md)。

---

## HTTP 与 WebSocket 通信

### HTTP RESTful 规范

- 采用标准 HTTP 谓词：`GET`（查询）、`POST`（创建）、`PUT`（更新）、`DELETE`（删除）。
- 鉴权机制：受保护的业务接口请求头需携带 Bearer Token；登录、验证码、公开扫码预约等匿名入口以服务端白名单为准：
  ```http
  Authorization: Bearer <your_jwt_token>
  ```
- 响应体格式：
  ```json
  {
    "code": 200,
    "message": "list devices success",
    "data": []
  }
  ```

### WebSocket 实时通道

- `/ws/ai-assistant`：AI 智能体问答交互流式双向传输。

---

## 环境变量与核心配置

可通过系统环境变量或修改各模块配置文件进行自定义：

| 模块 | 变量名 | 默认值 | 作用说明 |
| :--- | :--- | :--- | :--- |
| **Backend** | `SMART_LAB_DB_URL` | 必填；本地脚本可提供开发值 | MySQL 连接串 |
| **Backend** | `SMART_LAB_DB_USERNAME` | 必填；本地脚本默认 root | 数据库用户名 |
| **Backend** | `SMART_LAB_DB_PASSWORD` | 按实际数据库配置 | 数据库密码 |
| **Backend** | `SMART_LAB_JWT_SECRET` | 无通用默认值 | JWT 签名密钥；本地启动脚本可生成，生产环境必须独立配置 |
| **Backend** | `DASHSCOPE_API_KEY` | *(空)* | 阿里通义千问 API 密钥 |
| **Web** | `VITE_API_MODE` | `server` | 接口模式（`server` 真实联调 / `mock` 假数据） |
| **Web** | `VITE_API_PROXY_TARGET` | `http://127.0.0.1:8080` | 开发环境后端代理目标地址 |
| **Monitoring** | `GRAFANA_ADMIN_PASSWORD` | *(需配置)* | Grafana 监控后台管理员密码 |

---

## 云上部署与可观测性

### 1. 后端与 Web 端生产打包

```bash
# 1. 打包后端 Jar
cd Backend
mvn clean package

# 2. 构建前端静态资源
cd ../web
npm ci
npm run build
```

前端生成的 `dist/` 目录配合 Nginx 托管，下面仅示范路由；生产域名、TLS 与部署路径需独立配置。WebSocket 代理保留 `/ws/ai-assistant`，API 代理移除前端 `/api/` 前缀。

```nginx
server {
    listen 80;
    server_name smartlab.your-school.edu.cn;

    location / {
        root /var/www/smartlab/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
    }
}
```

### 2. Prometheus + Grafana 容器化监控

项目自带开箱即用的监控配置（详见 `monitoring/README.md`）：

```powershell
cd monitoring
Copy-Item .env.example .env
# 先编辑 .env，替换 GRAFANA_ADMIN_PASSWORD，再启动监控容器组
docker compose up -d
```

- 后端核心指标暴露于独立管理端口 `8081`（`/actuator/prometheus`）。
- 访问 Grafana：`http://localhost:3000`（账号：`admin`），默认内置 `zhiyun-backend` 仪表盘，实时观测 **QPS、P95 响应时延、5xx 错误率、JVM 堆内存、线程池与 HikariCP 数据库连接池活跃度**。

---

## CI 与自动化质量保障

项目采用 GitHub Actions（见 `.github/workflows/ci.yml`）实现自动化质量守护：

```mermaid
flowchart LR
    Push[git push / PR] --> Matrix{CI 并行任务}
    Matrix --> JobA["后端编译打包 & JaCoCo 96% 覆盖率验证"]
    Matrix --> JobB["前端生产打包 & Vitest 96% 覆盖率验证"]
    JobA --> Check["Required Checks 双门禁必须全绿通过"]
    JobB --> Check
    Check --> Pass[通过 CI 校验]
```

- **容器级服务集成**：后端 CI 自动启动临时 MySQL 8.4 容器执行真实数据流集成测试。
- **状态汇总**：Required checks 任务仅在前后端任务都成功时通过。是否强制阻止合并还取决于 GitHub 分支保护设置，工作流本身不等于已启用分支保护。
- **动态覆盖率徽章**：main 的前后端测试均成功后，CI 从 JaCoCo XML 与 Vitest JSON 读取实际行数，将 `(后端已覆盖行 + 前端已覆盖行) / (后端总行 + 前端总行)` 发布到专用 `coverage-badges` 分支。顶部徽章通过 Shields endpoint 读取该结果，不需要 Codecov 账号或令牌。两个模块仍分别执行 96% 门槛，合并值不能掩盖单个模块未达标。
- **更新与限制**：徽章表示最近一次成功 main 运行的结果；失败运行不发布新值，需同时查看 CI 状态。首次成功发布前可能显示不可用，发布后有数分钟缓存。分支只存放生成的覆盖率数据，不是开发分支。PR 无写权限；如果仓库策略禁止 Actions 写入，发布任务会失败，不会回填固定百分比。
- **查看真实结果**：打开 [main 分支 Actions](https://github.com/Alexander-Noah/classjob/actions/workflows/ci.yml?query=branch%3Amain)，选择对应提交的运行，在 Summary 中查看 backend/frontend 的覆盖行数、总行数、百分比及门槛结果。Artifacts 中可下载 backend-test-reports 与 frontend-test-reports；报告保留 14 天。缺失报告或失败运行不能视为覆盖率达标。

---

## 相关入口与参考文档

- 🌐 **Web 管理平台开发入口**：`http://localhost:5173`
- 📡 **后端业务 API 地址**：`http://localhost:8080`
- 📑 **Swagger / OpenAPI 接口文档**：默认关闭；本地显式设置 `SMART_LAB_OPENAPI_ENABLED=true` 和 `SMART_LAB_SWAGGER_UI_ENABLED=true` 后访问 `http://localhost:8080/swagger-ui.html`
- 📈 **Prometheus 采集看板**：`http://localhost:9090`
- 📊 **Grafana 综合监控面板**：`http://localhost:3000/d/zhiyun-backend`
- 📖 **架构设计详述文档**：[项目架构说明.md](项目架构说明.md)
- 🎨 **界面与交互设计规范**：[RDESIGN.md](RDESIGN.md)
- 📦 **产品定义与用户定位**：[PRODUCT.md](PRODUCT.md)
- 🗂️ **快速文件检索与资产指南**：[文件索引-快速查找指南.md](文件索引-快速查找指南.md)

---

## English documentation

完整英文说明见 [README.en.md](README.en.md)，包括安装、配置、终端通信、测试、部署和覆盖率徽章。
