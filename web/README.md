# 智云实验室前端

智云实验室前端是基于 Vue 3、Vite 和 Element Plus 构建的实验室协同管控平台管理端。系统面向实验室管理员、教师、运维人员和系统管理员，覆盖实验室资源、设备资产、预约审核、使用记录、故障报修、耗材库存、统计分析、AI 智能助手和物联网扩展等业务。

## 功能模块

- 工作台：实验室运行概览、待办提醒、业务闭环指标。
- 实验室管理：空间资源、实验室状态、资产预览。
- 设备资产：设备台账、状态维护、盘点和故障流转。
- 环境管理：课程环境、软件依赖、环境模板。
- 预约管理：预约申请、审核、使用记录联动。
- 故障报修：异常上报、维修派单、处理归档。
- 使用记录：门禁、预约、借用、巡检和异常复核记录。
- 统计分析：预约、使用、设备、维修和资源利用分析。
- AI 智能助手：文字/语音问答，调用后端企业智能体服务。
- 物联网扩展：实验室灯光、空调温控、门禁、摄像头、传感器和场景模式控制。
- 系统管理：角色入口、权限与用户管理等后台配置能力。

## 技术栈

- Vue 3
- Vite 8
- Vue Router
- Element Plus
- ECharts
- Axios

## 环境要求

- Node.js 18 或更高版本
- npm 9 或更高版本
- 后端服务默认运行在 `http://127.0.0.1:8080`

## 快速启动

```bash
npm install
npm run dev
```

开发服务默认地址由 Vite 输出，一般为：

```text
http://127.0.0.1:5173
```

## 环境变量

复制 `.env.example` 为 `.env.development`，按部署环境调整：

```env
VITE_API_MODE=server
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://127.0.0.1:8080
VITE_API_TIMEOUT=10000
```

说明：

- `VITE_API_MODE=server` 表示调用真实后端接口。
- `VITE_API_BASE_URL=/api` 配合 Vite 代理使用。
- `VITE_API_PROXY_TARGET` 指向后端服务地址。
- 生产部署时可将 `VITE_API_BASE_URL` 改为网关或后端服务地址。

## 构建与预览

```bash
npm run build
npm run preview
```

构建产物输出到 `dist/`，可部署到 Nginx、学校内网 Web 服务器或静态资源服务。

## 推荐部署方式

Nginx 示例：

```nginx
server {
  listen 80;
  server_name smart-lab.example.edu.cn;

  root /opt/smart-lab/web/dist;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

## 目录结构

```text
src/
  api/                 接口封装
  components/          通用与业务组件
  composables/         页面业务组合逻辑
  constants/           路由、模块、静态配置
  router/              路由配置
  views/               页面模块
```

## 开发注意事项

- 不要在前端仓库提交真实密钥、后端令牌或学校内网账号。
- 物联网控制、AI 智能体、语音识别等能力均通过后端接口代理，不在浏览器端保存密钥。
- 前端页面应优先使用 `src/api` 中的接口封装，避免在组件内直接拼接后端地址。

## 安全与账号规则

- 登录成功后的 token 只通过 `Authorization: Bearer <token>` 发送给后端。
- AI 智能助手 WebSocket 会在连接地址中附带登录 token，后端握手阶段会校验 token，后端日志会自动脱敏该字段。
- 新增账号必须填写初始密码，密码至少 8 位，并同时包含字母和数字。
- 编辑账号时密码留空表示不修改原密码。
- 生产环境不要把 `.env.development` 中的局域网地址直接复制到线上配置。

## 加签处理联动

预约管理页面已接入通用加签能力：

- 列表和右侧详情都提供“加签”入口。
- 发起加签后，预约状态变为 `加签中`，通过/驳回按钮会暂时锁定。
- 右侧详情面板会显示当前待处理加签人、加签原因，并提供“同意加签”“退回”“撤销”。
- 同意加签后预约回到 `待审核`；退回加签后预约变为 `已驳回`；撤销后回到 `待审核`。

前端接口封装位置：

```text
src/api/services/approvalCountersigns.js
```

接口地址统一维护在：

```text
src/api/endpoints.js
```

## 验证命令

本次安全与加签处理相关改动至少需要通过：

```bash
npm run build
```

后端在 `Backend` 仓库执行：

```powershell
mvn.cmd -DskipTests compile
```

## 前端二次开发与修改指南

本节用于后续维护时快速定位“要改哪里、怎么改”。前端修改一般遵循：

```text
接口地址 -> 接口封装 -> composable 业务逻辑 -> views 页面展示
```

### 1. 常用目录说明

```text
src/api/
  http.js                         Axios 实例、token、错误处理
  endpoints.js                    所有后端接口地址
  services/                       业务接口封装

src/composables/
  useDeviceManagement.js          设备管理业务逻辑
  useReservationManagement.js     预约管理业务逻辑
  useUsageRecords.js              使用记录业务逻辑
  useRoomControl.js               物联网控制业务逻辑

src/views/
  login/                          登录页
  dashboard/                      工作台
  labs/                           实验室与课表相关页面
  devices/                        设备资产页面
  reservations/                   预约管理页面
  records/                        使用记录页面
  repairs/                        故障报修页面
  statistics/                     统计分析页面
  ai-assistant/                   AI 助手页面
  notices/                        通知公告页面
  users/                          用户权限页面

src/router/index.js               路由与角色页面入口
src/constants/auth.js             角色、默认首页、登录跳转
```

### 2. 修改页面文案、表格、按钮

1. 先到 `src/views/模块名/` 找页面。
2. 修改 `<template>` 中的展示结构。
3. 修改 `<style scoped>` 中的样式。
4. 如果按钮涉及接口，继续查看对应 `composables` 或 `api/services`。
5. 最后执行：

```bash
npm run build
```

示例：

```text
修改统计分析页：
src/views/statistics/DataStatistics.vue

修改预约列表：
src/views/reservations/ReservationManagement.vue
src/views/reservations/components/ReservationListTable.vue

修改设备详情：
src/views/devices/components/DeviceDetailPanel.vue
```

### 3. 新增或修改接口调用

新增接口时不要直接在页面中写完整 URL，按下面顺序处理：

1. 在 `src/api/endpoints.js` 增加接口地址。
2. 如果接口会被多个页面复用，在 `src/api/services/` 新增或修改 service。
3. 在页面或 composable 中调用 service。

示例：

```js
// src/api/endpoints.js
export const API_ENDPOINTS = {
  labs: {
    list: '/labs',
    detail: (id) => `/labs/${id}`,
  },
}
```

页面中优先使用封装，不建议这样写：

```js
http.get('http://127.0.0.1:8080/labs')
```

建议这样写：

```js
http.get(API_ENDPOINTS.labs.list)
```

### 4. 新增前端页面

1. 在 `src/views/` 下创建页面目录和 `.vue` 文件。
2. 在 `src/router/index.js` 注册路由。
3. 如果需要角色默认入口，修改 `src/constants/auth.js`。
4. 如果需要菜单入口，查看对应角色门户或布局页面。
5. 如果页面需要数据，先在 `src/api/endpoints.js` 增加接口地址。
6. 执行 `npm run build`。

### 5. 修改统计分析页

统计分析页核心文件：

```text
src/views/statistics/DataStatistics.vue
```

当前实验室管理员统计来源：

```text
/labs
/devices
/reservations
/usage-records
/class-timetables?limit=5000
/class-timetables/crawler-config
/repairs
/course-environment-requests
/environment-templates
```

关键函数：

```text
loadBusinessStatistics       加载统计所需接口
buildLinkedStatistics        汇总统计主逻辑
createLabResolver            将记录匹配到实验室基础表
createLabUsageRows           生成实验室使用对比数据
expandTimetableOccurrences   将课表周次展开成实际日期
buildTrendOption             使用趋势图配置
buildCompareOption           实验室使用对比柱状图配置
```

统计口径注意事项：

- 实验室统计必须以 `/labs` 返回的实验室基础表为准。
- 课表中的 `classroom` 不一定是实验室，可能是操场或普通教室，必须经过 `createLabResolver` 匹配。
- 摘要卡、趋势图、柱状图、明细表要使用同一批过滤后的数据，避免数量对不上。
- 预约只统计已通过、通过、已确认、预约成功、正常等状态。
- 近 7 天、本月、本学期的筛选依赖记录日期和课表学期起始日期。

如果图表文字展示不全，优先检查：

```text
buildCompareOption -> xAxis.axisLabel
wrapAxisLabel
chart-box 高度
grid.bottom
```

### 6. 修改登录、权限和默认首页

常用文件：

```text
src/views/login/Login.vue
src/router/index.js
src/constants/auth.js
src/api/services/auth.js
```

常见修改：

- 登录成功跳转页面：看 `AUTH_ROLE_DEFAULT_PATH`。
- 新增角色：改 `constants/auth.js` 和路由 meta。
- 登录接口字段变化：改 `src/api/services/auth.js` 和 `Login.vue`。
- token 失效处理：看 `src/api/http.js`。

### 7. 修改预约管理

常用文件：

```text
src/views/reservations/ReservationManagement.vue
src/views/reservations/components/
src/composables/useReservationManagement.js
src/api/endpoints.js
src/api/services/approvalCountersigns.js
```

预约通过、驳回、加签等动作一般在 composable 中处理。页面组件主要负责展示和触发事件。

### 8. 修改设备管理

常用文件：

```text
src/views/devices/DeviceManagement.vue
src/views/devices/components/
src/composables/useDeviceManagement.js
src/api/endpoints.js
```

新增字段时要同步修改：

- 列表列
- 编辑弹窗表单
- 详情面板
- 导入/导出字段
- 后端实体和 Mapper SQL

### 9. 前端排错顺序

页面没有数据时按顺序检查：

1. 浏览器 Network 是否发出请求。
2. 请求地址是否是 `/api/xxx`。
3. Vite 代理是否指向 `VITE_API_PROXY_TARGET`。
4. 后端是否启动在 `8080`。
5. 后端返回结构是否是 `{ code, message, data }`。
6. 页面读取字段是否和后端返回字段一致。
7. 控制台是否有 Axios 或 Vue 报错。

### 10. 提交前检查

至少执行：

```bash
npm run build
```

如果改了接口联调，还要打开页面实际操作一遍，并检查浏览器 Network 响应。
