# 智云实验室学生端

这是从教师端工程拆出的独立学生端安装包工程。学生端没有主窗口，启动后常驻 Windows 系统托盘，自动发现同一局域网内的教师端，并定时上报本机状态。

## 功能说明

- 启动后自动加入系统托盘，不显示普通窗口。
- 自动广播查找教师端，发现后向教师端上报本机信息。
- 定时采集并上报主机名、IP、CPU、内存、磁盘使用率。
- 接收教师端下发的控制指令。
- 支持开机自启。
- 重复启动时会检测 `5001` 端口占用，避免多个学生端实例重复上报。
- 支持本地配置固定教师端 IP，适合虚拟机、跨网段或广播不可用的环境。
- 提供本地状态接口和日志文件，方便现场排障。
- 托盘菜单支持重连、立即上报、重载配置、打开配置、打开日志、打开数据目录和退出。

## 通信端口

学生端会使用以下端口和协议：

```text
UDP 8888   发送 WHO_IS_TEACHER 广播，发现教师端
HTTP 5000  向教师端 POST /api/collect 上报状态
HTTP 5001  学生端本地监听 POST /api/action，接收教师端控制指令
```

教师端需要先启动本地网关，否则学生端托盘会显示正在查找教师端。

## 本地配置

学生端首次启动会自动创建配置文件：

```text
%LOCALAPPDATA%/ZhiyunStudentClient/student-settings.json
```

默认内容：

```json
{
  "teacherIp": null,
  "discoveryEnabled": true,
  "reportIntervalMs": 3000
}
```

配置说明：

```text
teacherIp          固定教师端 IP。为空时自动广播发现教师端。
discoveryEnabled   是否启用 UDP 广播发现。
reportIntervalMs   上报间隔，单位毫秒，最低按 1000ms 处理。
```

如果虚拟机里广播发现不到教师端，可以把 `teacherIp` 改成教师机 IP，例如：

```json
{
  "teacherIp": "192.168.1.10",
  "discoveryEnabled": true,
  "reportIntervalMs": 3000
}
```

修改配置后，可以在托盘菜单点击“重载配置”，不用重启学生端。

## 本地状态接口

学生端启动后会在本机 `5001` 端口提供排障接口：

```text
GET  http://127.0.0.1:5001/api/status
GET  http://127.0.0.1:5001/api/logs
POST http://127.0.0.1:5001/api/reload-settings
```

`/api/status` 会返回当前教师端 IP、固定教师端 IP、本机状态、最后上报时间、配置路径和日志路径。

`/api/logs` 会返回最近 200 条运行日志。

`/api/reload-settings` 会重新读取本地配置文件。

## 托盘菜单

右下角托盘图标菜单包含：

```text
状态           显示当前连接状态
重连           清空当前连接并重新连接教师端
立即上报       立刻向教师端上报一次本机状态
重载配置       重新读取 student-settings.json
打开配置       用记事本打开配置文件
打开日志       用记事本打开日志文件
打开数据目录   打开配置和日志所在目录
关于           显示学生端名称
退出           退出学生端进程
```

## 控制指令

教师端可以向学生端 `http://学生端IP:5001/api/action` 发送控制请求：

```json
{
  "action": "refresh | message | lock | reboot | shutdown",
  "message": "可选消息内容",
  "requestId": "可选请求 ID"
}
```

学生端支持的动作：

```text
refresh   立即重新上报一次本机状态
message   弹出教师端消息
lock      锁定 Windows 工作站
reboot    重启电脑
shutdown  关闭电脑
```

## 目录结构

```text
zhiyun-student-client/
├─ package.json                 学生端构建命令
├─ student-dist/index.html      Tauri 所需的最小前端占位文件
├─ src-tauri/
│  ├─ Cargo.toml                学生端 Rust/Tauri 配置
│  ├─ tauri.conf.json           学生端安装包配置
│  ├─ src/main.rs               学生端入口
│  ├─ src/lib.rs                学生端后台逻辑
│  ├─ icons/                    应用图标
│  └─ license/agreement.txt     安装协议
```

## 开发环境

需要安装：

```text
Node.js
pnpm
Rust
Tauri Windows 构建依赖
NSIS
```

如果教师端工程已经可以正常 `tauri build`，通常说明本机 Tauri/Rust/NSIS 环境已经准备好。

## 打包安装包

在学生端目录执行：

```powershell
cd C:\Users\zhibi\Desktop\智云实验室——基于数据驱动的实验室协同管控平台\zhiyun-student-client
pnpm install
pnpm build
```

安装包输出位置：

```text
src-tauri/target/release/bundle/nsis/智云实验室学生端_0.1.0_x64-setup.exe
```

只验证可执行文件构建、不生成安装包：

```powershell
pnpm build:no-bundle
```

可执行文件输出位置：

```text
src-tauri/target/release/zhiyun-student-client.exe
```

## 安装与运行

1. 将 `智云实验室学生端_0.1.0_x64-setup.exe` 复制到学生机。
2. 双击安装。
3. 安装完成后启动学生端。
4. 右下角系统托盘出现智云图标。
5. 教师端启动本地网关后，学生端会自动连接并上报。

学生端没有主窗口。如果看到浏览器式窗口提示 `localhost 拒绝连接`，说明运行的不是这个独立学生端安装包，而是教师端包或旧的混合包。

## 联调检查

教师端：

```text
1. 启动教师端。
2. 在设备/网关页面启动本地网关。
3. 确认教师端监听 UDP 8888 和 HTTP 5000。
```

学生端：

```text
1. 启动学生端。
2. 查看系统托盘状态。
3. 状态应从“正在查找教师端”变为“已连接教师端：IP”。
4. 教师端设备列表应出现该学生机。
```

## 常见问题

### 学生端一直显示正在查找教师端

检查：

```text
1. 教师端是否已经启动本地网关。
2. 教师机和学生机是否在同一局域网。
3. 虚拟机网络是否使用桥接模式或能访问宿主机局域网。
4. Windows 防火墙是否允许 UDP 8888、HTTP 5000、HTTP 5001。
```

也可以直接配置固定教师端 IP：

```text
1. 右键托盘图标，点击“打开配置”。
2. 将 teacherIp 改成教师机 IP。
3. 保存文件。
4. 右键托盘图标，点击“重载配置”。
5. 点击“立即上报”或“重连”。
```

### 学生端安装后没有窗口

这是正常行为。学生端是托盘常驻程序，没有主界面。

### 运行后出现 localhost 拒绝连接

这不是学生端独立包的预期行为。请确认安装的是：

```text
zhiyun-student-client/src-tauri/target/release/bundle/nsis/智云实验室学生端_0.1.0_x64-setup.exe
```

不要使用教师端目录下旧的可执行文件或旧安装包测试学生端。

### 重新安装后仍然像旧版本

先退出或卸载旧学生端，再安装新包。学生端会开机自启，测试前可以从任务管理器结束旧的 `zhiyun-student-client.exe` 进程。

## 构建脚本

`package.json` 中包含：

```json
{
  "scripts": {
    "check": "cargo check --manifest-path src-tauri/Cargo.toml",
    "build": "tauri build",
    "build:no-bundle": "tauri build --no-bundle"
  }
}
```

日常修改后建议先执行：

```powershell
pnpm check
```

确认无误后再执行：

```powershell
pnpm build
```
