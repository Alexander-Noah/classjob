# 智云实验室前后端接口文档（联调版）

> 本文档按当前前端页面的真实调用和展示字段整理。后端联调时将 `VITE_API_MODE=server`，并将 `VITE_API_BASE_URL` 指向后端服务即可。

## 1. 通用约定

- Base URL：`/api`
- 数据格式：`application/json`
- 认证方式：`Authorization: Bearer <token>`
- 时间格式：列表展示优先支持 `yyyy-MM-dd`，日志/创建更新时间优先支持 `yyyy-MM-dd HH:mm:ss`
- 列表响应兼容三种结构，推荐第一种：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": []
  }
}
```

前端也兼容：

```json
{ "code": 200, "message": "success", "data": [] }
```

```json
[]
```

## 2. 前端字段适配规则

- 页面内部为了展示会使用 `code/name/lab/owner` 等短字段，但提交给后端时以数据库语义字段为准，例如 `deviceCode/deviceName/labId/ownerUserId`。
- 下拉框依赖：
  - 实验室下拉：`GET /labs`
  - 责任人/管理员下拉：`GET /users`
  - 报修设备下拉：`GET /devices`
- 批量接口 `/xxx/batch` 主要服务列表整体同步和 mock 兼容；设备新增/编辑/删除已经改为单条接口。

## 3. 认证

### 登录

- `POST /auth/login`

请求：

```json
{
  "username": "admin",
  "password": "123456",
  "role": "labAdmin"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "role": "labAdmin",
    "user": {
      "id": 1,
      "name": "张明",
      "username": "admin",
      "roleName": "实验室管理员"
    }
  }
}
```

### 退出登录

- `POST /auth/logout`

### 当前登录人信息

- `GET /auth/profile`

## 4. 首页看板

### 首页概览

- `GET /dashboard/overview`

响应字段：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "stats": [],
    "shortcuts": [],
    "labUsage": [],
    "deviceStatus": [],
    "notices": []
  }
}
```

## 5. 实验室管理

### 获取实验室列表

- `GET /labs`

查询参数：`keyword`、`status`、`type`、`building`

响应字段：

```json
{
  "id": 1,
  "labCode": "A101",
  "labName": "A101 云计算实验室",
  "building": "信息楼 A 区",
  "floor": "1F",
  "roomNo": "A101",
  "labType": "教学实训",
  "capacity": 72,
  "managerUserId": 2,
  "managerName": "张明",
  "openStatus": "开放",
  "deviceCount": 78,
  "remark": "一期示例实验室",
  "createdAt": "2026-04-23 10:00:00",
  "updatedAt": "2026-04-23 10:00:00"
}
```

前端会映射：

- `labCode -> code`
- `labName -> name`
- `roomNo -> room`
- `labType -> type`
- `openStatus -> status`
- `deviceCount -> devices`

### 新增实验室

- `POST /labs`

请求：

```json
{
  "labCode": "A101",
  "labName": "A101 云计算实验室",
  "building": "信息楼 A 区",
  "floor": "1F",
  "roomNo": "A101",
  "labType": "教学实训",
  "capacity": 72,
  "managerUserId": 2,
  "openStatus": "开放",
  "remark": ""
}
```

### 修改、删除、批量

- `PUT /labs/{id}`：请求字段同新增实验室。
- `DELETE /labs/{id}`
- `PUT /labs/batch`：请求 `{ "resource": "labs", "records": [] }`
- `POST /labs/reset`：可选；恢复初始数据时调用。

## 6. 设备资产管理

### 获取设备列表

- `GET /devices`

查询参数：`keyword`、`lab`、`category`、`status`

响应字段：

```json
{
  "id": 1,
  "deviceCode": "PC-A101-018",
  "deviceName": "图形工作站",
  "category": "计算终端",
  "labId": 1,
  "labName": "A101 云计算实验室",
  "location": "A101 第 2 排",
  "ownerUserId": 2,
  "ownerUsername": "张明",
  "status": "正常",
  "specs": "i7 / 32GB / RTX 4070",
  "purchaseDate": "2024-09-12",
  "createdAt": "2026-04-23 10:00:00",
  "updatedAt": "2026-04-23 10:00:00"
}
```

前端会映射：

- `deviceCode -> code`
- `deviceName -> name`
- `labName -> lab`
- `ownerUsername/ownerName -> owner`

前端展示扩展字段 `health`、`online`、`inventoryDate`、`warranty`、`usageHours`、`maintenance` 后端可不返回。

### 新增设备

- `POST /devices`

请求：

```json
{
  "deviceCode": "PC-A101-018",
  "deviceName": "图形工作站",
  "category": "计算终端",
  "labId": 1,
  "location": "A101 第 2 排",
  "ownerUserId": 2,
  "status": "正常",
  "specs": "i7 / 32GB / RTX 4070",
  "purchaseDate": "2024-09-12"
}
```

### 修改、删除、批量

- `PUT /devices/{id}`：请求字段同新增设备。
- `DELETE /devices/{id}`
- `PUT /devices/batch`：可选，当前设备新增/编辑/删除页面不依赖。
- `POST /devices/reset`：可选。

### 设备弹窗依赖

- `GET /labs`：用于“所属实验室”下拉，至少返回 `id` 和 `labName/name`。
- `GET /users`：用于“责任人”下拉，至少返回 `id`、`username`、`realName`、`roleName`。

## 7. 实验室预约

### 获取预约列表

- `GET /reservations`

响应字段建议：

```json
{
  "id": 1,
  "applicant": "软件工程 2 班",
  "department": "软件学院",
  "contact": "刘老师",
  "lab": "A101 云计算实验室",
  "date": "2026-04-23",
  "timeRange": "08:00-10:00",
  "scene": "课程",
  "attendees": 68,
  "reason": "云计算平台实训课程",
  "status": "待审核",
  "reviewer": "待审核",
  "note": "需确认 GPU 服务器空闲情况"
}
```

也兼容后端语义字段：

- `applicantName -> applicant`
- `labName -> lab`
- `reservationDate -> date`
- `attendeeCount -> attendees`
- `reviewerName -> reviewer`
- `reviewRemark -> note`

### 新增、修改、删除

- `POST /reservations`
- `PUT /reservations/{id}`
- `DELETE /reservations/{id}`
- `PUT /reservations/batch`：当前页面保存仍可使用批量同步。
- `POST /reservations/reset`：可选。

新增/编辑请求建议使用页面字段：

```json
{
  "applicant": "软件工程 2 班",
  "department": "软件学院",
  "contact": "刘老师",
  "lab": "A101 云计算实验室",
  "date": "2026-04-23",
  "timeRange": "08:00-10:00",
  "scene": "课程",
  "attendees": 68,
  "reason": "云计算平台实训课程",
  "note": "待管理员审核。"
}
```

### 审批

- `POST /reservations/{id}/approve`
- `POST /reservations/{id}/reject`

请求：

```json
{
  "reviewerName": "张明",
  "reviewRemark": "通过"
}
```

## 8. 故障报修

### 获取工单列表

- `GET /repairs`

响应字段建议：

```json
{
  "id": "R20260423001",
  "ticket": "R20260423001",
  "reporter": "张明",
  "contact": "132****2210",
  "lab": "B203 网络安全实验室",
  "device": "核心交换机",
  "faultType": "网络故障",
  "priority": "高",
  "status": "待派单",
  "assignee": "待分配",
  "deadline": "今日 18:00",
  "description": "交换机端口告警",
  "result": "待维修人员接单。"
}
```

也兼容后端语义字段：

- `orderNo -> ticket`
- `reporterName -> reporter`
- `labName -> lab`
- `deviceName -> device`
- `priorityLevel -> priority`
- `assigneeName -> assignee`
- `resultDesc -> result`

### 新增、修改、删除

- `POST /repairs`
- `PUT /repairs/{id}`
- `DELETE /repairs/{id}`
- `PUT /repairs/batch`：当前页面保存仍可使用批量同步。
- `POST /repairs/reset`：可选。

新增/编辑请求建议使用页面字段：

```json
{
  "reporter": "张明",
  "contact": "132****2210",
  "lab": "B203 网络安全实验室",
  "device": "核心交换机",
  "faultType": "网络故障",
  "priority": "高",
  "deadline": "今日 18:00",
  "description": "交换机端口告警"
}
```

### 工单动作

- `POST /repairs/{id}/assign`
- `POST /repairs/{id}/submit-acceptance`
- `POST /repairs/{id}/complete`

请求示例：

```json
{
  "assignee": "周工",
  "status": "处理中",
  "resultDesc": "已派单处理"
}
```

### 报修弹窗依赖

- `GET /labs`：用于“所属实验室”下拉。
- `GET /devices`：用于“故障设备”下拉；前端会读取 `deviceName/name/deviceCode/code`。

## 9. 使用记录

### 获取使用记录列表

- `GET /usage-records`

响应字段：

```json
{
  "id": 1,
  "person": "软件工程 2 班",
  "resource": "A101 云计算实验室",
  "scene": "课程",
  "time": "2026-04-26 08:00:00",
  "status": "正常"
}
```

### 批量、动作

- `PUT /usage-records/batch`
- `POST /usage-records/{id}/review`
- `POST /usage-records/{id}/abnormal`
- `POST /usage-records/{id}/archive`
- `POST /usage-records/reset`：可选。

## 10. 用户权限

### 用户列表

- `GET /users`

响应字段：

```json
{
  "id": 1,
  "username": "labadmin",
  "realName": "张明",
  "roleName": "实验室管理员",
  "roleCode": "labAdmin",
  "phone": "13800000001",
  "email": "labadmin@smartlab.com",
  "status": 1
}
```

前端下拉框至少依赖：

- `id`
- `username`
- `realName`
- `roleName`

### 新增、修改、启停

- `POST /users`
- `PUT /users/{id}`
- `POST /users/{id}/status`

新增请求：

```json
{
  "username": "teacher01",
  "password": "123456",
  "realName": "李老师",
  "phone": "13800000003",
  "email": "teacher01@smartlab.com",
  "status": 1,
  "roleCode": "teacher"
}
```

## 11. 通用业务模块

通用页面统一走模块接口：

- `GET /modules/{moduleName}`
- `PUT /modules/{moduleName}/batch`
- `POST /modules/{moduleName}/reset`：可选。

环境管理页面当前保留的通用模块名：

- 软件环境：`lab-software-environments`
- 课程环境：`course-environment-requests`

请求：

```json
{
  "resource": "module-name",
  "records": []
}
```

说明：

- 通用弹窗会把 `status/type/category/priority/role/owner/assignee/manager/lab/scope` 等字段自动渲染为下拉框。
- 后端只需要原样保存并返回 records；不需要理解每个模块的业务含义。

## 12. 环境模板

环境模板已经从通用模块接口拆出，前端不再调用 `/modules/environment-templates`。

### 数据表

表名：`environment_template`

核心字段：

- `id`
- `name`：模板名称
- `course`：适用课程
- `os`：操作系统
- `softwareList` / `software_list`：软件清单
- `labs`：适用实验室
- `status`：状态，例如 `可复用`、`待更新`、`试运行`
- `tagType` / `tag_type`：前端标签类型，例如 `success`、`warning`、`primary`

### 接口

- `GET /environment-templates`：获取环境模板列表
- `POST /environment-templates`：新增环境模板
- `PUT /environment-templates/{id}`：修改环境模板
- `DELETE /environment-templates/{id}`：删除环境模板
- `PUT /environment-templates/batch`：批量覆盖环境模板
- `POST /environment-templates/reset`：恢复默认环境模板

新增/修改请求：

```json
{
  "name": "Java Web 标准环境",
  "course": "Java Web 程序设计",
  "os": "Windows 11",
  "softwareList": "JDK、IDEA、Tomcat、MySQL",
  "labs": "A101、A102",
  "status": "可复用",
  "tagType": "success"
}
```

## 13. 前端接口入口

- `src/api/config.js`
- `src/api/endpoints.js`
- `src/api/http.js`
- `src/api/services/collectionService.js`
- `src/api/services/auth.js`
- `src/api/services/basicResources.js`
- `src/api/services/users.js`

后端联调步骤：

1. 将 `.env.example` 复制为 `.env`
2. 设置 `VITE_API_MODE=server`
3. 设置 `VITE_API_BASE_URL`
4. 按本文档实现接口
