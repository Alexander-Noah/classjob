# 智云实验室桌面客户端

本目录是智云实验室的 Tauri 桌面端工程，采用“一仓双端”结构：

- 教师端：带界面的本地值守客户端，用于设备面板、终端列表、日志和连接设置。
- 学生端：独立安装包，无主界面，启动后常驻 Windows 右下角系统托盘，自动发现教师端并上报本机状态。

学生端逻辑放在客户端工程内，不依赖平台后端打包，便于安装到机房其他主机。

## 技术栈

- Tauri 2
- Rust 2021
- Vue 3
- Naive UI
- Vite
- sysinfo

## 端口与协议

教师端本地网关：

- UDP `8888`：响应学生端发现广播。
- HTTP `5000`：接收学生端状态上报。
- `POST /api/collect`：接收 `deviceId / hostname / ipAddress / metrics / timestamp`。
- `POST /api/devices/{deviceId}/action`：教师端将右键动作转发给学生端。

学生端：

- UDP 广播 `WHO_IS_TEACHER`。
- 收到教师端 `I_AM_TEACHER` 后记录教师端 IP。
- 定时 POST 到 `http://教师端IP:5000/api/collect`。
- HTTP `5001`：接收教师端控制指令。
- `POST /api/action`：支持 `refresh / message / lock / reboot / shutdown`。

学生端控制请求体：

```json
{
  "action": "refresh | message | lock | reboot | shutdown",
  "message": "optional text",
  "requestId": "optional id"
}
```

学生端控制响应：

```json
{
  "ok": true,
  "message": "accepted"
}
```

## 学生端行为

- 无主窗口，不显示普通界面。
- 只在系统托盘显示状态。
- 托盘菜单只保留“状态 / 重连 / 关于”，不提供退出入口。
- 启动时尝试启用开机自启，失败只记录日志，不阻塞运行。
- 找不到教师端时不弹错误窗口，托盘显示“正在查找教师端”。
- 已连接时托盘显示“已连接教师端：IP”。
- 与教师端断开后继续搜索并自动重连。
- 重复启动时如果 `5001` 端口已被占用，托盘显示“学生端已在运行”，避免重复发现和重复上报。

## 开发命令

安装依赖：

```powershell
pnpm install
```

启动教师端开发模式：

```powershell
pnpm tauri dev
```

构建教师端前端：

```powershell
npm.cmd run build
```

Rust 编译检查：

```powershell
cargo check --manifest-path src-tauri/Cargo.toml
```

检查学生端 Tauri 入口：

```powershell
cargo check --manifest-path src-tauri/Cargo.toml --bin tvta-ui --features student-client
```

检查独立学生端 Cargo 二进制：

```powershell
cargo check --manifest-path src-tauri/Cargo.toml --bin zhiyun-student-client
```

## 打包命令

教师端安装包：

```powershell
pnpm tauri build
```

学生端 release 可执行文件：

```powershell
npm.cmd run build:student
```

输出：

```text
src-tauri/target/release/zhiyun-student-client.exe
```

学生端 Tauri 安装包：

```powershell
npm.cmd run bundle:student
```

该命令使用 `src-tauri/tauri.student.conf.json`，产品名为 `zhiyun-student-client`，标识符为 `com.zhiyun.student-client`，并通过 `student-client` feature 将主入口切换为学生端托盘程序。

验证学生端 Tauri 构建链路但跳过生成安装包：

```powershell
npx.cmd tauri build --config src-tauri/tauri.student.conf.json --features student-client --no-bundle
```

成功时会输出：

```text
Built application at: src-tauri/target/release/zhiyun-student-client.exe
```

## 关键文件

```text
src-tauri/src/lib.rs                  教师端网关、学生端后台服务、共享本机状态采集
src-tauri/src/main.rs                 默认教师端入口；student-client feature 下切到学生端
src-tauri/src/student_main.rs         独立学生端 Cargo bin 入口
src-tauri/tauri.conf.json             教师端 Tauri 配置
src-tauri/tauri.student.conf.json     学生端 Tauri 配置
src-tauri/Cargo.toml                  双端 bin、student-client feature、Tauri 托盘 feature
package.json                          教师端与学生端构建脚本
```

## 联调流程

1. 启动教师端，在设备面板开启本地网关。
2. 启动学生端 `zhiyun-student-client.exe`。
3. 学生端托盘显示“已连接教师端：IP”。
4. 教师端设备面板出现学生端设备。
5. 在教师端右键设备执行刷新、发送消息、锁屏、重启或关机。
6. 教师端会通过学生端 IP 转发到 `http://学生端IP:5001/api/action`。

第一版只覆盖 Windows 机房基础闭环。屏幕广播、文件分发、黑屏肃静、禁网、进程白名单等高级能力后续再扩展。
