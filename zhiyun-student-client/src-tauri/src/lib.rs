//! 独立学生端运行时：发现教师网关、上报本机状态并提供托盘入口。
//! 网络循环与主界面分离；共享状态经 Arc/Mutex 访问，停止信号使用原子变量。
//! 与教师端的发现消息、采集字段和心跳间隔需要协同维护。
use std::{
    collections::HashMap,
    env,
    fs::{self, OpenOptions},
    io::{Read, Write},
    net::{IpAddr, Ipv4Addr, TcpStream, UdpSocket},
    path::PathBuf,
    process::Command,
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc, Mutex,
    },
    thread::{self, JoinHandle},
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use chrono::Local;
use local_ip_address::local_ip;
use mac_address::get_mac_address;
use serde::{Deserialize, Serialize};
use sysinfo::{Disks, System};
use tauri::{
    menu::{Menu, MenuItem},
    tray::TrayIconBuilder,
};
use tauri_plugin_autostart::ManagerExt;
use tiny_http::{Header, Method, Request, Response, Server, StatusCode};
use tungstenite::{connect, Message};

// 8888 用于发现，5000 用于采集，5001 用于学生动作；这些不是云端后端端口。
const DISCOVERY_PORT: u16 = 8888;
const COLLECT_PORT: u16 = 5000;
const STUDENT_ACTION_PORT: u16 = 5001;
const DISCOVERY_MESSAGE: &str = "WHO_IS_TEACHER";
const DISCOVERY_RESPONSE: &str = "I_AM_TEACHER";
const OFFLINE_AFTER_MS: u128 = 30_000;
const HOST_STATUS_REFRESH_MS: u64 = 2_000;
const STUDENT_REPORT_INTERVAL_MS: u64 = 3_000;
const STUDENT_DISCOVERY_INTERVAL_MS: u64 = 2_000;
const STUDENT_TRAY_REFRESH_MS: u64 = 1_000;
const STUDENT_APP_DIR: &str = "ZhiyunStudentClient";
const STUDENT_SETTINGS_FILE: &str = "student-settings.json";
const STUDENT_LOG_FILE: &str = "student.log";
const LAN_STUDENT_RECONNECT_MS: u64 = 3_000;
const LAN_STUDENT_HEARTBEAT_MS: u64 = 5_000;
const DEFAULT_BACKEND_API_BASE_URL: &str = "http://localhost:8080";

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct GatewaySettings {
    cloud_endpoint: String,
    business_system_url: String,
}

impl Default for GatewaySettings {
    /// 构造当前类型的默认状态。
    /// 返回：Self。
    fn default() -> Self {
        Self {
            cloud_endpoint: String::new(),
            business_system_url: "http://127.0.0.1:5173".to_string(),
        }
    }
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct CloudStatus {
    ok: bool,
    message: String,
    last_push_at: Option<String>,
    last_batch_size: usize,
}

impl Default for CloudStatus {
    /// 构造当前类型的默认状态。
    /// 返回：Self。
    fn default() -> Self {
        Self {
            ok: false,
            message: "cloud forwarding not configured".to_string(),
            last_push_at: None,
            last_batch_size: 0,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
struct DeviceMetrics {
    cpu_usage: f64,
    memory_usage: f64,
    disk_usage: f64,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
struct CollectPayload {
    device_id: Option<String>,
    hostname: Option<String>,
    ip_address: Option<String>,
    timestamp: Option<i64>,
    metrics: Option<DeviceMetrics>,
}

#[derive(Debug, Clone)]
struct DeviceEntry {
    device_id: String,
    hostname: String,
    timestamp: i64,
    last_seen_ms: u128,
    ip_address: String,
    metrics: DeviceMetrics,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct DeviceSnapshot {
    device_id: String,
    hostname: String,
    timestamp: i64,
    last_seen: u128,
    ip_address: String,
    metrics: DeviceMetrics,
    online: bool,
    warning: bool,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct GatewaySnapshot {
    running: bool,
    last_error: String,
    gateway_ip: String,
    connected_count: usize,
    total_count: usize,
    cloud: CloudStatus,
    devices: Vec<DeviceSnapshot>,
    settings: GatewaySettings,
    logs: Vec<String>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct LocalHostStatus {
    host_name: String,
    ip_address: String,
    status: String,
    last_seen: String,
    metrics: DeviceMetrics,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[serde(default)]
struct StudentSettings {
    teacher_ip: Option<String>,
    lab_id: Option<u64>,
    token: Option<String>,
    api_base_url: String,
    discovery_enabled: bool,
    report_interval_ms: u64,
}

impl Default for StudentSettings {
    /// 构造当前类型的默认状态。
    /// 返回：Self。
    fn default() -> Self {
        Self {
            teacher_ip: None,
            lab_id: None,
            token: None,
            api_base_url: DEFAULT_BACKEND_API_BASE_URL.to_string(),
            discovery_enabled: true,
            report_interval_ms: STUDENT_REPORT_INTERVAL_MS,
        }
    }
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct StudentStatusSnapshot {
    running: bool,
    teacher_ip: Option<String>,
    lan_teacher_url: Option<String>,
    lab_id: Option<u64>,
    api_base_url: String,
    configured_teacher_ip: Option<String>,
    discovery_enabled: bool,
    last_report_at: Option<String>,
    last_error: String,
    local: LocalHostStatus,
    settings_path: String,
    log_path: String,
}

struct GatewayRuntime {
    stop_flag: Arc<AtomicBool>,
    udp_handle: JoinHandle<()>,
    http_handle: JoinHandle<()>,
}

struct GatewayInner {
    running: bool,
    last_error: String,
    settings: GatewaySettings,
    devices: HashMap<String, DeviceEntry>,
    logs: Vec<String>,
    cloud: CloudStatus,
    runtime: Option<GatewayRuntime>,
}

impl Default for GatewayInner {
    /// 构造当前类型的默认状态。
    /// 返回：Self。
    fn default() -> Self {
        Self {
            running: false,
            last_error: String::new(),
            settings: GatewaySettings::default(),
            devices: HashMap::new(),
            logs: Vec::new(),
            cloud: CloudStatus::default(),
            runtime: None,
        }
    }
}

#[derive(Clone)]
struct AppState {
    gateway: Arc<Mutex<GatewayInner>>,
    host_status: Arc<Mutex<LocalHostStatus>>,
}

#[derive(Clone)]
struct StudentRuntimeState {
    host_status: Arc<Mutex<LocalHostStatus>>,
    teacher_ip: Arc<Mutex<Option<String>>>,
    lan_teacher_url: Arc<Mutex<Option<String>>>,
    settings: Arc<Mutex<StudentSettings>>,
    last_report_ms: Arc<Mutex<Option<u128>>>,
    last_report_at: Arc<Mutex<Option<String>>>,
    last_error: Arc<Mutex<String>>,
    logs: Arc<Mutex<Vec<String>>>,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
struct StudentActionRequest {
    action: String,
    message: Option<String>,
    request_id: Option<String>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct StudentCollectPayload {
    device_id: String,
    hostname: String,
    ip_address: String,
    timestamp: i64,
    metrics: DeviceMetrics,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
struct BackendResult<T> {
    data: Option<T>,
    #[serde(rename = "message")]
    _message: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
struct TeacherHostLookup {
    lab_id: u64,
    host_ip: String,
    port: u16,
    status: String,
}

/// 生成当前时间的毫秒表示。
/// 返回：u128。
fn now_millis() -> u128 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.as_millis())
        .unwrap_or_default()
}

/// 生成日志使用的当前时间文本。
/// 返回：String。
fn now_log_text() -> String {
    Local::now().format("%Y/%m/%d %H:%M:%S").to_string()
}

/// 学生数据目录的处理逻辑。
/// 返回：PathBuf。
fn student_data_dir() -> PathBuf {
    env::var_os("LOCALAPPDATA")
        .map(PathBuf::from)
        .or_else(/** 处理 or_else 调用中的回调逻辑。 */ || env::var_os("APPDATA").map(PathBuf::from))
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || env::current_dir().unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ |_| PathBuf::from(".")))
        .join(STUDENT_APP_DIR)
}

/// 学生设置路径的处理逻辑。
/// 返回：PathBuf。
fn student_settings_path() -> PathBuf {
    student_data_dir().join(STUDENT_SETTINGS_FILE)
}

/// 学生log路径的处理逻辑。
/// 返回：PathBuf。
fn student_log_path() -> PathBuf {
    student_data_dir().join(STUDENT_LOG_FILE)
}

/// 确保满足学生数据目录。
fn ensure_student_data_dir() {
    let _ = fs::create_dir_all(student_data_dir());
}

/// 加载学生设置。
/// 返回：StudentSettings。
fn load_student_settings() -> StudentSettings {
    ensure_student_data_dir();
    let path = student_settings_path();

    match fs::read_to_string(&path) {
        Ok(content) => match serde_json::from_str::<StudentSettings>(&content) {
            Ok(settings) => settings,
            Err(_) => {
                let settings = StudentSettings::default();
                save_student_settings(&settings);
                settings
            }
        },
        Err(_) => {
            let settings = StudentSettings::default();
            save_student_settings(&settings);
            settings
        }
    }
}

/// 保存学生设置。
/// 入参：settings（设置）。
fn save_student_settings(settings: &StudentSettings) {
    ensure_student_data_dir();
    if let Ok(content) = serde_json::to_string_pretty(settings) {
        let _ = fs::write(student_settings_path(), content);
    }
}

/// 推送学生log。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）；message（消息）。
fn push_student_log(state: &StudentRuntimeState, message: impl Into<String>) {
    let line = format!("{} {}", now_log_text(), message.into());

    if let Ok(mut logs) = state.logs.lock() {
        logs.push(line.clone());
        if logs.len() > 200 {
            let keep_from = logs.len().saturating_sub(200);
            *logs = logs.split_off(keep_from);
        }
    }

    ensure_student_data_dir();
    if let Ok(mut file) = OpenOptions::new()
        .create(true)
        .append(true)
        .open(student_log_path())
    {
        let _ = writeln!(file, "{}", line);
    }
}

/// 本地ip文本的处理逻辑。
/// 返回：String。
fn local_ip_text() -> String {
    local_ip()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.to_string())
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ |_| "127.0.0.1".to_string())
}

/// 提供默认本地主机状态。
/// 返回：LocalHostStatus。
fn default_local_host_status() -> LocalHostStatus {
    LocalHostStatus {
        host_name: System::host_name().unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || "UNKNOWN".to_string()),
        ip_address: local_ip_text(),
        status: "offline".to_string(),
        last_seen: "not collected yet".to_string(),
        metrics: DeviceMetrics::default(),
    }
}

/// 收集本地主机状态。
/// 入参：system（system）；disks（disks）。
/// 返回：LocalHostStatus。
fn collect_local_host_status(system: &mut System, disks: &mut Disks) -> LocalHostStatus {
    system.refresh_cpu();
    system.refresh_memory();
    disks.refresh_list();

    let cpu_usage = f64::from(system.global_cpu_info().cpu_usage());
    let memory_usage = if system.total_memory() > 0 {
        (system.used_memory() as f64 / system.total_memory() as f64) * 100.0
    } else {
        0.0
    };
    let disk_usage = disks
        .list()
        .iter()
        .find(/** 判断当前条目是否为查找目标。 入参：disk。 */ |disk| disk.mount_point().to_string_lossy().starts_with("C:"))
        .or_else(/** 处理 or_else 调用中的回调逻辑。 */ || disks.list().first())
        .map(/** 将当前条目转换为目标结果。 入参：disk。 */ |disk| {
            let total = disk.total_space();
            if total == 0 {
                0.0
            } else {
                ((total - disk.available_space()) as f64 / total as f64) * 100.0
            }
        })
        .unwrap_or_default();
    let status = if cpu_usage >= 85.0 || memory_usage >= 85.0 || disk_usage >= 90.0 {
        "warning"
    } else {
        "online"
    };

    LocalHostStatus {
        host_name: System::host_name().unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || "UNKNOWN".to_string()),
        ip_address: local_ip_text(),
        status: status.to_string(),
        last_seen: now_log_text(),
        metrics: DeviceMetrics {
            cpu_usage: (cpu_usage * 10.0).round() / 10.0,
            memory_usage: (memory_usage * 10.0).round() / 10.0,
            disk_usage: (disk_usage * 10.0).round() / 10.0,
        },
    }
}

/// spawn主机状态probe的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：cache（缓存）。
fn spawn_host_status_probe(cache: Arc<Mutex<LocalHostStatus>>) {
    thread::spawn(/** 执行新工作线程的任务。 */ move || {
        let mut system = System::new_all();
        let mut disks = Disks::new_with_refreshed_list();

        loop {
            let status = collect_local_host_status(&mut system, &mut disks);
            if let Ok(mut cached_status) = cache.lock() {
                *cached_status = status;
            }
            thread::sleep(Duration::from_millis(HOST_STATUS_REFRESH_MS));
        }
    });
}

/// 获取本地macaddress。
/// 返回：Result<String, String>；通过 Result 区分成功值与错误值。
#[tauri::command]
fn get_local_mac_address() -> Result<String, String> {
    let mac_address = get_mac_address()
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("failed to read local MAC address: {}", error))?
        .ok_or_else(/** 处理 ok_or_else 调用中的回调逻辑。 */ || "local MAC address not found".to_string())?;

    Ok(mac_address.to_string().to_uppercase())
}

/// 获取本地主机状态。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Result<LocalHostStatus, String>；通过 Result 区分成功值与错误值。
#[tauri::command]
fn get_local_host_status(state: tauri::State<'_, AppState>) -> Result<LocalHostStatus, String> {
    state
        .host_status
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：status。 */ |status| status.clone())
        .map_err(/** 处理 map_err 调用中的回调逻辑。 */ |_| "local host status lock error".to_string())
}

/// 推送log。
/// 入参：inner（inner）；message（消息）。
fn push_log(inner: &mut GatewayInner, message: impl Into<String>) {
    let line = format!("{} {}", now_log_text(), message.into());
    inner.logs.push(line);
    if inner.logs.len() > 200 {
        let keep_from = inner.logs.len().saturating_sub(200);
        inner.logs = inner.logs.split_off(keep_from);
    }
}

/// 构建快照。
/// 入参：inner（inner）。
/// 返回：GatewaySnapshot。
fn build_snapshot(inner: &GatewayInner) -> GatewaySnapshot {
    let now = now_millis();
    let mut devices = inner
        .devices
        .values()
        .cloned()
        .map(/** 将当前条目转换为目标结果。 入参：item。 */ |item| {
            let online = now.saturating_sub(item.last_seen_ms) <= OFFLINE_AFTER_MS;
            let warning = item.metrics.cpu_usage >= 85.0
                || item.metrics.memory_usage >= 85.0
                || item.metrics.disk_usage >= 90.0;

            DeviceSnapshot {
                device_id: item.device_id,
                hostname: item.hostname,
                timestamp: item.timestamp,
                last_seen: item.last_seen_ms,
                ip_address: item.ip_address,
                metrics: item.metrics,
                online,
                warning,
            }
        })
        .collect::<Vec<_>>();

    devices.sort_by(/** 处理 sort_by 调用中的回调逻辑。 入参：left、right。 */ |left, right| left.hostname.cmp(&right.hostname));

    GatewaySnapshot {
        running: inner.running,
        last_error: inner.last_error.clone(),
        gateway_ip: if inner.running {
            local_ip_text()
        } else {
            "--".to_string()
        },
        connected_count: devices.iter().filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ |item| item.online).count(),
        total_count: devices.len(),
        cloud: inner.cloud.clone(),
        devices,
        settings: inner.settings.clone(),
        logs: inner.logs.clone(),
    }
}

/// JSON请求头的处理逻辑。
/// 返回：Header。
fn json_header() -> Header {
    Header::from_bytes("Content-Type", "application/json; charset=utf-8").unwrap()
}

/// corsorigin请求头的处理逻辑。
/// 返回：Header。
fn cors_origin_header() -> Header {
    Header::from_bytes("Access-Control-Allow-Origin", "*").unwrap()
}

/// cors方法请求头的处理逻辑。
/// 返回：Header。
fn cors_methods_header() -> Header {
    Header::from_bytes("Access-Control-Allow-Methods", "GET, POST, OPTIONS").unwrap()
}

/// cors请求头请求头的处理逻辑。
/// 返回：Header。
fn cors_headers_header() -> Header {
    Header::from_bytes("Access-Control-Allow-Headers", "Content-Type").unwrap()
}

/// respondJSON的处理逻辑。
/// 入参：request（请求）；payload（载荷）；status（状态）。
fn respond_json<T: Serialize>(request: Request, payload: &T, status: u16) {
    match serde_json::to_string(payload) {
        Ok(body) => {
            let response = Response::from_string(body)
                .with_status_code(StatusCode(status))
                .with_header(json_header())
                .with_header(cors_origin_header())
                .with_header(cors_methods_header())
                .with_header(cors_headers_header());
            let _ = request.respond(response);
        }
        Err(_) => {
            let response = Response::from_string("{\"ok\":false,\"message\":\"serialize failed\"}")
                .with_status_code(StatusCode(500))
                .with_header(json_header())
                .with_header(cors_origin_header())
                .with_header(cors_methods_header())
                .with_header(cors_headers_header());
            let _ = request.respond(response);
        }
    }
}

/// respond选项的处理逻辑。
/// 入参：request（请求）。
fn respond_options(request: Request) {
    let response = Response::empty(StatusCode(204))
        .with_header(cors_origin_header())
        .with_header(cors_methods_header())
        .with_header(cors_headers_header());
    let _ = request.respond(response);
}

/// percentdecode路径segment的处理逻辑。
/// 入参：value（值）。
/// 返回：String。
fn percent_decode_path_segment(value: &str) -> String {
    let bytes = value.as_bytes();
    let mut output = Vec::with_capacity(bytes.len());
    let mut index = 0;

    while index < bytes.len() {
        if bytes[index] == b'%' && index + 2 < bytes.len() {
            let hex = &value[index + 1..index + 3];
            if let Ok(decoded) = u8::from_str_radix(hex, 16) {
                output.push(decoded);
                index += 3;
                continue;
            }
        }
        output.push(bytes[index]);
        index += 1;
    }

    String::from_utf8_lossy(&output).to_string()
}

/// 判断是否为routable学生ip。
/// 入参：value（值）。
/// 返回：bool。
fn is_routable_student_ip(value: &str) -> bool {
    let value = value.trim();
    if value.is_empty() || value == "--" {
        return false;
    }

    match value.parse::<IpAddr>() {
        Ok(IpAddr::V4(ip)) => !ip.is_unspecified() && !ip.is_loopback() && !ip.is_broadcast(),
        Ok(IpAddr::V6(ip)) => !ip.is_unspecified() && !ip.is_loopback(),
        Err(_) => false,
    }
}

/// 学生ip来源collect的处理逻辑。
/// 入参：reported_ip（reportedip）；remote_ip（远程ip）。
/// 返回：String。
fn student_ip_from_collect(reported_ip: Option<&str>, remote_ip: Option<&str>) -> String {
    [reported_ip, remote_ip]
        .into_iter()
        .flatten()
        .map(str::trim)
        .find(/** 判断当前条目是否为查找目标。 入参：value。 */ |value| is_routable_student_ip(value))
        .map(ToString::to_string)
        .or_else(/** 处理 or_else 调用中的回调逻辑。 */ || {
            remote_ip
                .map(str::trim)
                .filter(/** 判断当前条目是否符合筛选条件。 入参：value。 */ |value| !value.is_empty())
                .map(ToString::to_string)
        })
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || "--".to_string())
}

/// discoverybroadcast目标forip的处理逻辑。
/// 入参：local_ip（本地ip）。
/// 返回：Vec<String>。
fn discovery_broadcast_targets_for_ip(local_ip: &str) -> Vec<String> {
    let mut targets = vec!["255.255.255.255".to_string()];

    if let Ok(ip) = local_ip.parse::<Ipv4Addr>() {
        if !ip.is_unspecified() && !ip.is_loopback() {
            let octets = ip.octets();
            let subnet_broadcast = Ipv4Addr::new(octets[0], octets[1], octets[2], 255).to_string();
            if !targets.iter().any(/** 处理 any 调用中的回调逻辑。 入参：target。 */ |target| target == &subnet_broadcast) {
                targets.push(subnet_broadcast);
            }
        }
    }

    targets
}

/// discoverybroadcast目标的处理逻辑。
/// 返回：Vec<String>。
fn discovery_broadcast_targets() -> Vec<String> {
    discovery_broadcast_targets_for_ip(&local_ip_text())
}

/// postJSON目标主机的处理逻辑。
/// 入参：host（主机）；port（端口）；path（路径）；body（正文）。
/// 返回：Result<(), String>；通过 Result 区分成功值与错误值。
fn post_json_to_host(host: &str, port: u16, path: &str, body: &str) -> Result<(), String> {
    let mut stream = TcpStream::connect((host, port))
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("connect {}:{} failed: {}", host, port, error))?;
    let timeout = Duration::from_secs(3);
    let _ = stream.set_read_timeout(Some(timeout));
    let _ = stream.set_write_timeout(Some(timeout));

    let request = format!(
        "POST {} HTTP/1.1\r\nHost: {}:{}\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
        path,
        host,
        port,
        body.as_bytes().len(),
        body
    );
    stream
        .write_all(request.as_bytes())
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("write request failed: {}", error))?;

    let mut response = String::new();
    stream
        .read_to_string(&mut response)
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("read response failed: {}", error))?;

    if response.starts_with("HTTP/1.1 2") || response.starts_with("HTTP/1.0 2") {
        Ok(())
    } else {
        let status_line = response.lines().next().unwrap_or("empty response");
        Err(format!("student rejected action: {}", status_line))
    }
}

/// 解析HTTP基础URL。
/// 入参：api_base_url（API基础URL）。
/// 返回：Result<(String, u16, String), String>；通过 Result 区分成功值与错误值。
fn parse_http_base_url(api_base_url: &str) -> Result<(String, u16, String), String> {
    let trimmed = api_base_url.trim().trim_end_matches('/');
    let without_scheme = trimmed
        .strip_prefix("http://")
        .ok_or_else(/** 处理 ok_or_else 调用中的回调逻辑。 */ || "only http backend api base url is supported".to_string())?;
    let (host_port, prefix) = without_scheme
        .split_once('/')
        .map(/** 将当前条目转换为目标结果。 */ |(host, path)| (host, format!("/{}", path.trim_matches('/'))))
        .unwrap_or((without_scheme, String::new()));
    if host_port.trim().is_empty() {
        return Err("backend api host is empty".to_string());
    }

    let (host, port) = match host_port.rsplit_once(':') {
        Some((host, port)) if port.chars().all(/** 处理 all 调用中的回调逻辑。 入参：value。 */ |value| value.is_ascii_digit()) => {
            let parsed_port = port
                .parse::<u16>()
                .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("invalid backend api port: {}", error))?;
            (host.to_string(), parsed_port)
        }
        _ => (host_port.to_string(), 80),
    };

    Ok((host, port, prefix))
}

/// HTTP请求目标API的处理逻辑。
/// 入参：api_base_url（API基础URL）；method（方法）；path（路径）；body（正文）。
/// 返回：Result<String, String>；通过 Result 区分成功值与错误值。
fn http_request_to_api(
    api_base_url: &str,
    method: &str,
    path: &str,
    body: Option<&str>,
) -> Result<String, String> {
    let (host, port, prefix) = parse_http_base_url(api_base_url)?;
    let request_path = format!("{}{}", prefix, path);
    let mut stream = TcpStream::connect((host.as_str(), port))
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("connect backend {}:{} failed: {}", host, port, error))?;
    let timeout = Duration::from_secs(3);
    let _ = stream.set_read_timeout(Some(timeout));
    let _ = stream.set_write_timeout(Some(timeout));

    let body = body.unwrap_or_default();
    let request = if method == "POST" {
        format!(
            "POST {} HTTP/1.1\r\nHost: {}:{}\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
            request_path,
            host,
            port,
            body.as_bytes().len(),
            body
        )
    } else {
        format!(
            "GET {} HTTP/1.1\r\nHost: {}:{}\r\nConnection: close\r\n\r\n",
            request_path, host, port
        )
    };

    stream
        .write_all(request.as_bytes())
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("write backend request failed: {}", error))?;

    let mut response = String::new();
    stream
        .read_to_string(&mut response)
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("read backend response failed: {}", error))?;

    if response.starts_with("HTTP/1.1 2") || response.starts_with("HTTP/1.0 2") {
        Ok(response
            .split("\r\n\r\n")
            .nth(1)
            .unwrap_or_default()
            .to_string())
    } else {
        let status_line = response.lines().next().unwrap_or("empty response");
        Err(format!("backend rejected request: {}", status_line))
    }
}

/// 获取来源API。
/// 入参：api_base_url（API基础URL）；path（路径）。
/// 返回：Result<String, String>；通过 Result 区分成功值与错误值。
fn get_from_api(api_base_url: &str, path: &str) -> Result<String, String> {
    http_request_to_api(api_base_url, "GET", path, None)
}

/// postJSON目标API的处理逻辑。
/// 入参：api_base_url（API基础URL）；path（路径）；body（正文）。
/// 返回：Result<String, String>；通过 Result 区分成功值与错误值。
fn post_json_to_api(api_base_url: &str, path: &str, body: &str) -> Result<String, String> {
    http_request_to_api(api_base_url, "POST", path, Some(body))
}

/// 处理collect请求。
/// 涉及：访问互斥保护的共享状态。
/// 入参：gateway（网关）；request（请求）。
fn handle_collect_request(gateway: &Arc<Mutex<GatewayInner>>, mut request: Request) {
    let mut body = String::new();
    if request.as_reader().read_to_string(&mut body).is_err() {
        respond_json(
            request,
            &serde_json::json!({ "ok": false, "message": "invalid json" }),
            400,
        );
        return;
    }

    let payload = match serde_json::from_str::<CollectPayload>(&body) {
        Ok(value) => value,
        Err(_) => {
            respond_json(
                request,
                &serde_json::json!({ "ok": false, "message": "invalid json" }),
                400,
            );
            return;
        }
    };

    let device_id = payload.device_id.unwrap_or_default().trim().to_string();
    if device_id.is_empty() {
        respond_json(
            request,
            &serde_json::json!({ "ok": false, "message": "deviceId required" }),
            400,
        );
        return;
    }

    let hostname = payload
        .hostname
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || "UNKNOWN".to_string())
        .trim()
        .to_string();
    let remote_ip = request
        .remote_addr()
        .map(/** 将当前条目转换为目标结果。 入参：address。 */ |address| address.ip().to_string())
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || "--".to_string());
    let ip_address = student_ip_from_collect(payload.ip_address.as_deref(), Some(&remote_ip));

    let entry = DeviceEntry {
        device_id: device_id.clone(),
        hostname: if hostname.is_empty() {
            "UNKNOWN".to_string()
        } else {
            hostname
        },
        timestamp: payload.timestamp.unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || now_millis() as i64),
        last_seen_ms: now_millis(),
        ip_address: ip_address.clone(),
        metrics: payload.metrics.unwrap_or_default(),
    };

    if let Ok(mut inner) = gateway.lock() {
        let is_new_device = !inner.devices.contains_key(&device_id);
        inner.devices.insert(device_id.clone(), entry);

        if is_new_device {
            push_log(
                &mut inner,
                format!("student device {} connected from {}", device_id, ip_address),
            );
        }
    }

    respond_json(request, &serde_json::json!({ "ok": true }), 200);
}

/// 处理设备action请求。
/// 涉及：访问互斥保护的共享状态。
/// 入参：gateway（网关）；device_id（设备标识）；request（请求）。
fn handle_device_action_request(
    gateway: &Arc<Mutex<GatewayInner>>,
    device_id: String,
    mut request: Request,
) {
    let mut body = String::new();
    if request.as_reader().read_to_string(&mut body).is_err() {
        respond_json(
            request,
            &serde_json::json!({ "ok": false, "message": "invalid json" }),
            400,
        );
        return;
    }

    let action = match serde_json::from_str::<StudentActionRequest>(&body) {
        Ok(value) => value,
        Err(_) => {
            respond_json(
                request,
                &serde_json::json!({ "ok": false, "message": "invalid json" }),
                400,
            );
            return;
        }
    };

    let student_ip = {
        let inner = match gateway.lock() {
            Ok(value) => value,
            Err(_) => {
                respond_json(
                    request,
                    &serde_json::json!({ "ok": false, "message": "gateway lock error" }),
                    500,
                );
                return;
            }
        };

        match inner.devices.get(&device_id) {
            Some(device) if !device.ip_address.trim().is_empty() && device.ip_address != "--" => {
                device.ip_address.clone()
            }
            Some(_) => {
                respond_json(
                    request,
                    &serde_json::json!({ "ok": false, "message": "student ip unavailable" }),
                    400,
                );
                return;
            }
            None => {
                respond_json(
                    request,
                    &serde_json::json!({ "ok": false, "message": "device not found" }),
                    404,
                );
                return;
            }
        }
    };

    let forward_body = match serde_json::to_string(&action) {
        Ok(value) => value,
        Err(_) => {
            respond_json(
                request,
                &serde_json::json!({ "ok": false, "message": "serialize failed" }),
                500,
            );
            return;
        }
    };

    match post_json_to_host(
        &student_ip,
        STUDENT_ACTION_PORT,
        "/api/action",
        &forward_body,
    ) {
        Ok(()) => {
            if let Ok(mut inner) = gateway.lock() {
                push_log(
                    &mut inner,
                    format!(
                        "{} sent to student {} at {}",
                        action.action, device_id, student_ip
                    ),
                );
            }
            respond_json(
                request,
                &serde_json::json!({ "ok": true, "message": "sent" }),
                200,
            );
        }
        Err(error) => {
            if let Ok(mut inner) = gateway.lock() {
                push_log(
                    &mut inner,
                    format!("send {} to {} failed: {}", action.action, device_id, error),
                );
            }
            respond_json(
                request,
                &serde_json::json!({ "ok": false, "message": error }),
                502,
            );
        }
    }
}

/// udploop的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：gateway（网关）；stop_flag（stop标记）；socket（socket）。
fn udp_loop(gateway: Arc<Mutex<GatewayInner>>, stop_flag: Arc<AtomicBool>, socket: UdpSocket) {
    let _ = socket.set_read_timeout(Some(Duration::from_millis(500)));
    let mut buffer = [0_u8; 1024];

    while !stop_flag.load(Ordering::Relaxed) {
        match socket.recv_from(&mut buffer) {
            Ok((size, address)) => {
                let message = String::from_utf8_lossy(&buffer[..size]).trim().to_string();
                if message == DISCOVERY_MESSAGE {
                    let _ = socket.send_to(DISCOVERY_RESPONSE.as_bytes(), address);
                }
            }
            Err(error) if error.kind() == std::io::ErrorKind::WouldBlock => {}
            Err(error) if error.kind() == std::io::ErrorKind::TimedOut => {}
            Err(error) => {
                if let Ok(mut inner) = gateway.lock() {
                    let message = format!("UDP {} unavailable: {}", DISCOVERY_PORT, error);
                    inner.last_error = message.clone();
                    push_log(&mut inner, message);
                    inner.running = false;
                }
                break;
            }
        }
    }
}

/// HTTPloop的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：gateway（网关）；stop_flag（stop标记）；server（服务端）。
fn http_loop(gateway: Arc<Mutex<GatewayInner>>, stop_flag: Arc<AtomicBool>, server: Server) {
    while !stop_flag.load(Ordering::Relaxed) {
        match server.recv_timeout(Duration::from_millis(500)) {
            Ok(Some(request)) => {
                let path = request.url().to_string();
                let method = request.method().clone();

                if method == Method::Options {
                    respond_options(request);
                } else if method == Method::Get && path == "/api/status" {
                    if let Ok(inner) = gateway.lock() {
                        let snapshot = build_snapshot(&inner);
                        respond_json(request, &snapshot, 200);
                    } else {
                        respond_json(
                            request,
                            &serde_json::json!({ "ok": false, "message": "gateway lock error" }),
                            500,
                        );
                    }
                } else if method == Method::Post && path == "/api/collect" {
                    handle_collect_request(&gateway, request);
                } else if method == Method::Post
                    && path.starts_with("/api/devices/")
                    && path.ends_with("/action")
                {
                    let encoded_device_id = path
                        .trim_start_matches("/api/devices/")
                        .trim_end_matches("/action")
                        .trim_matches('/');
                    handle_device_action_request(
                        &gateway,
                        percent_decode_path_segment(encoded_device_id),
                        request,
                    );
                } else {
                    respond_json(
                        request,
                        &serde_json::json!({ "ok": false, "message": "not found" }),
                        404,
                    );
                }
            }
            Ok(None) => {}
            Err(error) => {
                if let Ok(mut inner) = gateway.lock() {
                    let message = format!("HTTP {} unavailable: {}", COLLECT_PORT, error);
                    inner.last_error = message.clone();
                    push_log(&mut inner, message);
                    inner.running = false;
                }
                break;
            }
        }
    }
}

/// 获取网关快照。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：GatewaySnapshot。
#[tauri::command]
fn get_gateway_snapshot(state: tauri::State<'_, AppState>) -> GatewaySnapshot {
    let inner = state.gateway.lock().unwrap();
    build_snapshot(&inner)
}

/// 更新网关设置。
/// 涉及：访问互斥保护的共享状态。
/// 入参：settings（设置）；state（状态）。
/// 返回：GatewaySnapshot。
#[tauri::command]
fn update_gateway_settings(
    settings: GatewaySettings,
    state: tauri::State<'_, AppState>,
) -> GatewaySnapshot {
    let mut inner = state.gateway.lock().unwrap();
    inner.settings = settings;
    inner.cloud = if inner.settings.cloud_endpoint.trim().is_empty() {
        CloudStatus::default()
    } else {
        CloudStatus {
            ok: true,
            message: "cloud forwarding configured".to_string(),
            last_push_at: None,
            last_batch_size: 0,
        }
    };
    push_log(&mut inner, "gateway settings updated");
    build_snapshot(&inner)
}

/// 启动网关。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Result<GatewaySnapshot, String>；通过 Result 区分成功值与错误值。
#[tauri::command]
fn start_gateway(state: tauri::State<'_, AppState>) -> Result<GatewaySnapshot, String> {
    let gateway = state.gateway.clone();

    {
        let mut inner = gateway
            .lock()
            .map_err(/** 处理 map_err 调用中的回调逻辑。 */ |_| "gateway lock error".to_string())?;
        if inner.running {
            return Ok(build_snapshot(&inner));
        }
        inner.last_error.clear();
    }

    let udp_socket = UdpSocket::bind(("0.0.0.0", DISCOVERY_PORT))
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("UDP {} bind failed: {}", DISCOVERY_PORT, error))?;
    let server = Server::http(("0.0.0.0", COLLECT_PORT))
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("HTTP {} bind failed: {}", COLLECT_PORT, error))?;

    let stop_flag = Arc::new(AtomicBool::new(false));

    let udp_handle = {
        let gateway = gateway.clone();
        let stop_flag = stop_flag.clone();
        thread::spawn(/** 执行新工作线程的任务。 */ move || udp_loop(gateway, stop_flag, udp_socket))
    };

    let http_handle = {
        let gateway = gateway.clone();
        let stop_flag = stop_flag.clone();
        thread::spawn(/** 执行新工作线程的任务。 */ move || http_loop(gateway, stop_flag, server))
    };

    let mut inner = gateway
        .lock()
        .map_err(/** 处理 map_err 调用中的回调逻辑。 */ |_| "gateway lock error".to_string())?;
    inner.running = true;
    inner.cloud = if inner.settings.cloud_endpoint.trim().is_empty() {
        CloudStatus::default()
    } else {
        CloudStatus {
            ok: true,
            message: "cloud forwarding configured".to_string(),
            last_push_at: None,
            last_batch_size: 0,
        }
    };
    push_log(
        &mut inner,
        format!(
            "teacher gateway started at {}:{}",
            local_ip_text(),
            COLLECT_PORT
        ),
    );
    inner.runtime = Some(GatewayRuntime {
        stop_flag,
        udp_handle,
        http_handle,
    });

    Ok(build_snapshot(&inner))
}

/// 停止网关。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Result<GatewaySnapshot, String>；通过 Result 区分成功值与错误值。
#[tauri::command]
fn stop_gateway(state: tauri::State<'_, AppState>) -> Result<GatewaySnapshot, String> {
    let runtime = {
        let mut inner = state
            .gateway
            .lock()
            .map_err(/** 处理 map_err 调用中的回调逻辑。 */ |_| "gateway lock error".to_string())?;
        inner.running = false;
        push_log(&mut inner, "teacher gateway stopped");
        inner.runtime.take()
    };

    if let Some(runtime) = runtime {
        runtime.stop_flag.store(true, Ordering::Relaxed);
        let _ = runtime.udp_handle.join();
        let _ = runtime.http_handle.join();
    }

    let inner = state
        .gateway
        .lock()
        .map_err(/** 处理 map_err 调用中的回调逻辑。 */ |_| "gateway lock error".to_string())?;
    Ok(build_snapshot(&inner))
}

/// 本地设备标识的处理逻辑。
/// 返回：String。
fn local_device_id() -> String {
    get_mac_address()
        .ok()
        .flatten()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.to_string().to_uppercase())
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || System::host_name().unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || "UNKNOWN".to_string()))
}

/// 构建学生collect载荷。
/// 入参：host_status（主机状态）。
/// 返回：StudentCollectPayload。
fn build_student_collect_payload(host_status: &LocalHostStatus) -> StudentCollectPayload {
    StudentCollectPayload {
        device_id: local_device_id(),
        hostname: host_status.host_name.clone(),
        ip_address: host_status.ip_address.clone(),
        timestamp: now_millis() as i64,
        metrics: host_status.metrics.clone(),
    }
}

/// 学生集合错误的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）；message（消息）。
fn student_set_error(state: &StudentRuntimeState, message: impl Into<String>) {
    let message = message.into();
    if let Ok(mut last_error) = state.last_error.lock() {
        *last_error = message.clone();
    }

    if !message.is_empty() {
        push_student_log(state, format!("error: {}", message));
    }
}

/// 学生教师ip的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Option<String>。
fn student_teacher_ip(state: &StudentRuntimeState) -> Option<String> {
    state.teacher_ip.lock().ok().and_then(/** 处理 and_then 调用中的回调逻辑。 入参：value。 */ |value| value.clone())
}

/// 学生集合教师ip的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）；ip（ip）。
fn student_set_teacher_ip(state: &StudentRuntimeState, ip: Option<String>) {
    let old_ip = student_teacher_ip(state);
    if let Ok(mut teacher_ip) = state.teacher_ip.lock() {
        *teacher_ip = ip.clone();
    }

    if old_ip != ip {
        match ip {
            Some(value) => push_student_log(state, format!("teacher connected: {}", value)),
            None => push_student_log(state, "teacher disconnected"),
        }
    }
}

/// 学生lan教师URL的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Option<String>。
fn student_lan_teacher_url(state: &StudentRuntimeState) -> Option<String> {
    state
        .lan_teacher_url
        .lock()
        .ok()
        .and_then(/** 处理 and_then 调用中的回调逻辑。 入参：value。 */ |value| value.clone())
}

/// 学生集合lan教师URL的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）；url（URL）。
fn student_set_lan_teacher_url(state: &StudentRuntimeState, url: Option<String>) {
    let old_url = student_lan_teacher_url(state);
    if let Ok(mut lan_teacher_url) = state.lan_teacher_url.lock() {
        *lan_teacher_url = url.clone();
    }
    if old_url != url {
        match url {
            Some(value) => {
                push_student_log(state, format!("LAN teacher websocket connected: {}", value))
            }
            None => push_student_log(state, "LAN teacher websocket disconnected"),
        }
    }
}

/// configured教师ip的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Option<String>。
fn configured_teacher_ip(state: &StudentRuntimeState) -> Option<String> {
    state
        .settings
        .lock()
        .ok()
        .and_then(/** 处理 and_then 调用中的回调逻辑。 入参：settings。 */ |settings| settings.teacher_ip.clone())
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.trim().to_string())
        .filter(/** 判断当前条目是否符合筛选条件。 入参：value。 */ |value| !value.is_empty())
}

/// configured实验室标识的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Option<u64>。
fn configured_lab_id(state: &StudentRuntimeState) -> Option<u64> {
    state
        .settings
        .lock()
        .ok()
        .and_then(/** 处理 and_then 调用中的回调逻辑。 入参：settings。 */ |settings| settings.lab_id)
}

/// configured令牌的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Option<String>。
fn configured_token(state: &StudentRuntimeState) -> Option<String> {
    state
        .settings
        .lock()
        .ok()
        .and_then(/** 处理 and_then 调用中的回调逻辑。 入参：settings。 */ |settings| settings.token.clone())
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.trim().to_string())
        .filter(/** 判断当前条目是否符合筛选条件。 入参：value。 */ |value| !value.is_empty())
}

/// configuredAPI基础URL的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：String。
fn configured_api_base_url(state: &StudentRuntimeState) -> String {
    state
        .settings
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：settings。 */ |settings| {
            let trimmed = settings.api_base_url.trim();
            if trimmed.is_empty() {
                DEFAULT_BACKEND_API_BASE_URL.to_string()
            } else {
                trimmed.trim_end_matches('/').to_string()
            }
        })
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ |_| DEFAULT_BACKEND_API_BASE_URL.to_string())
}

/// 学生reportintervalms的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：u64。
fn student_report_interval_ms(state: &StudentRuntimeState) -> u64 {
    state
        .settings
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：settings。 */ |settings| settings.report_interval_ms.max(1_000))
        .unwrap_or(STUDENT_REPORT_INTERVAL_MS)
}

/// 学生discovery已启用的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：bool。
fn student_discovery_enabled(state: &StudentRuntimeState) -> bool {
    state
        .settings
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：settings。 */ |settings| settings.discovery_enabled)
        .unwrap_or(true)
}

/// 应用configured教师ip。
/// 入参：state（状态）。
fn apply_configured_teacher_ip(state: &StudentRuntimeState) {
    if let Some(ip) = configured_teacher_ip(state) {
        student_set_teacher_ip(state, Some(ip));
    }
}

/// 重新加载学生设置。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
fn reload_student_settings(state: &StudentRuntimeState) {
    let settings = load_student_settings();
    if let Ok(mut cached_settings) = state.settings.lock() {
        *cached_settings = settings;
    }
    apply_configured_teacher_ip(state);
    push_student_log(state, "settings reloaded");
}

/// 构建学生状态快照。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：StudentStatusSnapshot。
fn build_student_status_snapshot(state: &StudentRuntimeState) -> StudentStatusSnapshot {
    let settings = state
        .settings
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .unwrap_or_default();
    let local = state
        .host_status
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ |_| default_local_host_status());
    let last_error = state
        .last_error
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .unwrap_or_default();
    let last_report_at = state
        .last_report_at
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .unwrap_or_default();

    StudentStatusSnapshot {
        running: true,
        teacher_ip: student_teacher_ip(state),
        lan_teacher_url: student_lan_teacher_url(state),
        lab_id: settings.lab_id,
        api_base_url: if settings.api_base_url.trim().is_empty() {
            DEFAULT_BACKEND_API_BASE_URL.to_string()
        } else {
            settings
                .api_base_url
                .trim()
                .trim_end_matches('/')
                .to_string()
        },
        configured_teacher_ip: settings.teacher_ip,
        discovery_enabled: settings.discovery_enabled,
        last_report_at,
        last_error,
        local,
        settings_path: student_settings_path().to_string_lossy().to_string(),
        log_path: student_log_path().to_string_lossy().to_string(),
    }
}

/// 复制本机状态快照后发送到教师的 /api/collect，网络请求期间不持有主机状态锁。
/// 只有 HTTP 上报成功后才更新最后上报时间；失败通过 Result 交由后台循环记录。
fn student_report_once(state: &StudentRuntimeState) -> Result<(), String> {
    let teacher_ip = student_teacher_ip(state).ok_or_else(/** 处理 ok_or_else 调用中的回调逻辑。 */ || "teacher not found".to_string())?;
    let host_status = state
        .host_status
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .map_err(/** 处理 map_err 调用中的回调逻辑。 */ |_| "host status lock error".to_string())?;
    let payload = build_student_collect_payload(&host_status);
    let body = serde_json::to_string(&payload).map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| error.to_string())?;

    post_json_to_host(&teacher_ip, COLLECT_PORT, "/api/collect", &body)?;

    if let Ok(mut last_report_ms) = state.last_report_ms.lock() {
        *last_report_ms = Some(now_millis());
    }
    if let Ok(mut last_report_at) = state.last_report_at.lock() {
        *last_report_at = Some(now_log_text());
    }
    student_set_error(state, String::new());
    Ok(())
}

/// percentencode查询的处理逻辑。
/// 入参：value（值）。
/// 返回：String。
fn percent_encode_query(value: &str) -> String {
    value
        .bytes()
        .flat_map(/** 处理 flat_map 调用中的回调逻辑。 入参：byte。 */ |byte| match byte {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                vec![byte as char]
            }
            _ => format!("%{:02X}", byte).chars().collect(),
        })
        .collect()
}

/// 当前学生身份的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Result<(String, String, String), String>；通过 Result 区分成功值与错误值。
fn current_student_identity(
    state: &StudentRuntimeState,
) -> Result<(String, String, String), String> {
    let host_status = state
        .host_status
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .map_err(/** 处理 map_err 调用中的回调逻辑。 */ |_| "host status lock error".to_string())?;
    Ok((
        local_device_id(),
        host_status.host_name,
        host_status.ip_address,
    ))
}

/// 查询当前教师主机。
/// 入参：api_base_url（API基础URL）；lab_id（实验室标识）。
/// 返回：Result<Option<TeacherHostLookup>, String>；通过 Result 区分成功值与错误值。
fn query_current_teacher_host(
    api_base_url: &str,
    lab_id: u64,
) -> Result<Option<TeacherHostLookup>, String> {
    let body = get_from_api(
        api_base_url,
        &format!("/api/teacher-hosts/current?labId={}", lab_id),
    )?;
    let result = serde_json::from_str::<BackendResult<TeacherHostLookup>>(&body)
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("parse teacher host response failed: {}", error))?;
    let Some(host) = result.data else {
        return Ok(None);
    };
    if host.lab_id != lab_id || !"online".eq_ignore_ascii_case(&host.status) {
        return Ok(None);
    }
    if host.host_ip.trim().is_empty() || host.port == 0 {
        return Ok(None);
    }
    Ok(Some(host))
}

/// 通知学生客户端backend。
/// 入参：state（状态）；status（状态）。
/// 返回：Result<(), String>；通过 Result 区分成功值与错误值。
fn notify_student_client_backend(state: &StudentRuntimeState, status: &str) -> Result<(), String> {
    let lab_id =
        configured_lab_id(state).ok_or_else(/** 处理 ok_or_else 调用中的回调逻辑。 */ || "student labId is not configured".to_string())?;
    let token =
        configured_token(state).ok_or_else(/** 处理 ok_or_else 调用中的回调逻辑。 */ || "student token is not configured".to_string())?;
    let api_base_url = configured_api_base_url(state);
    let (student_device_id, host_name, ip_address) = current_student_identity(state)?;
    let path = if status == "offline" {
        "/api/student-clients/offline"
    } else {
        "/api/student-clients/online"
    };
    let body = serde_json::json!({
        "labId": lab_id,
        "studentDeviceId": student_device_id,
        "hostName": host_name,
        "ipAddress": ip_address,
        "status": status,
        "token": token
    })
    .to_string();
    post_json_to_api(&api_base_url, path, &body).map(/** 将当前条目转换为目标结果。 */ |_| ())
}

/// 构建lan学生消息。
/// 入参：state（状态）；message_type（消息类型）；lab_id（实验室标识）；token（令牌）。
/// 返回：Result<String, String>；通过 Result 区分成功值与错误值。
fn build_lan_student_message(
    state: &StudentRuntimeState,
    message_type: &str,
    lab_id: u64,
    token: &str,
) -> Result<String, String> {
    let (student_device_id, host_name, ip_address) = current_student_identity(state)?;
    Ok(serde_json::json!({
        "type": message_type,
        "studentDeviceId": student_device_id,
        "hostName": host_name,
        "ipAddress": ip_address,
        "labId": lab_id,
        "token": token,
        "status": "online"
    })
    .to_string())
}

/// 运行学生lan会话。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）；host（主机）；lab_id（实验室标识）；token（令牌）。
/// 返回：Result<(), String>；通过 Result 区分成功值与错误值。
fn run_student_lan_session(
    state: &StudentRuntimeState,
    host: TeacherHostLookup,
    lab_id: u64,
    token: String,
) -> Result<(), String> {
    let ws_url = format!(
        "ws://{}:{}/ws/student?labId={}&token={}",
        host.host_ip,
        host.port,
        lab_id,
        percent_encode_query(&token)
    );
    let (mut websocket, _) = connect(ws_url.as_str())
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("connect LAN teacher websocket failed: {}", error))?;
    student_set_lan_teacher_url(state, Some(ws_url.clone()));
    student_set_teacher_ip(state, Some(host.host_ip.clone()));

    let hello = build_lan_student_message(state, "HELLO", lab_id, &token)?;
    websocket
        .send(Message::Text(hello.into()))
        .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("send HELLO failed: {}", error))?;
    let _ = notify_student_client_backend(state, "online");
    student_set_error(state, String::new());

    loop {
        thread::sleep(Duration::from_millis(LAN_STUDENT_HEARTBEAT_MS));
        let heartbeat = build_lan_student_message(state, "HEARTBEAT", lab_id, &token)?;
        websocket
            .send(Message::Text(heartbeat.into()))
            .map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| format!("send HEARTBEAT failed: {}", error))?;
        let _ = notify_student_client_backend(state, "online");
        if let Ok(mut last_report_ms) = state.last_report_ms.lock() {
            *last_report_ms = Some(now_millis());
        }
        if let Ok(mut last_report_at) = state.last_report_at.lock() {
            *last_report_at = Some(now_log_text());
        }
    }
}

/// spawn学生lanconnector的处理逻辑。
/// 入参：state（状态）。
fn spawn_student_lan_connector(state: StudentRuntimeState) {
    thread::spawn(/** 执行新工作线程的任务。 */ move || loop {
        let lab_id = configured_lab_id(&state);
        let token = configured_token(&state);
        let api_base_url = configured_api_base_url(&state);

        match (lab_id, token) {
            (Some(lab_id), Some(token)) => {
                match query_current_teacher_host(&api_base_url, lab_id) {
                    Ok(Some(host)) => {
                        let result = run_student_lan_session(&state, host, lab_id, token);
                        student_set_lan_teacher_url(&state, None);
                        let _ = notify_student_client_backend(&state, "offline");
                        if let Err(error) = result {
                            student_set_error(&state, error);
                        }
                    }
                    Ok(None) => {
                        student_set_lan_teacher_url(&state, None);
                        student_set_teacher_ip(&state, configured_teacher_ip(&state));
                        student_set_error(
                        &state,
                        "\u{5f53}\u{524d}\u{5b9e}\u{9a8c}\u{5ba4}\u{6559}\u{5e08}\u{7aef}\u{672a}\u{542f}\u{52a8}",
                    );
                    }
                    Err(error) => {
                        student_set_lan_teacher_url(&state, None);
                        student_set_error(&state, error);
                    }
                }
            }
            _ => {
                student_set_lan_teacher_url(&state, None);
                student_set_error(&state, "student labId/token is not configured");
            }
        }

        thread::sleep(Duration::from_millis(LAN_STUDENT_RECONNECT_MS));
    });
}

/// spawn学生discovery的处理逻辑。
/// 入参：state（状态）。
fn spawn_student_discovery(state: StudentRuntimeState) {
    thread::spawn(/** 执行新工作线程的任务。 */ move || {
        let socket = match UdpSocket::bind(("0.0.0.0", 0)) {
            Ok(value) => value,
            Err(error) => {
                student_set_error(&state, format!("discovery unavailable: {}", error));
                return;
            }
        };

        let _ = socket.set_broadcast(true);
        let _ = socket.set_read_timeout(Some(Duration::from_millis(500)));
        let mut buffer = [0_u8; 512];

        loop {
            if student_teacher_ip(&state).is_none() {
                apply_configured_teacher_ip(&state);
            }

            if student_teacher_ip(&state).is_none() && student_discovery_enabled(&state) {
                for target in discovery_broadcast_targets() {
                    let _ = socket.send_to(
                        DISCOVERY_MESSAGE.as_bytes(),
                        (target.as_str(), DISCOVERY_PORT),
                    );
                }
            }

            match socket.recv_from(&mut buffer) {
                Ok((size, address)) => {
                    let message = String::from_utf8_lossy(&buffer[..size]).trim().to_string();
                    if message == DISCOVERY_RESPONSE && configured_teacher_ip(&state).is_none() {
                        student_set_teacher_ip(&state, Some(address.ip().to_string()));
                        student_set_error(&state, String::new());
                    }
                }
                Err(error)
                    if error.kind() == std::io::ErrorKind::TimedOut
                        || error.kind() == std::io::ErrorKind::WouldBlock => {}
                Err(error) => student_set_error(&state, format!("discovery error: {}", error)),
            }

            thread::sleep(Duration::from_millis(STUDENT_DISCOVERY_INTERVAL_MS));
        }
    });
}

/// 启动进程存续期间的上报循环，不返回可单独取消的 JoinHandle。
/// 失败时清除自动发现的教师地址以触发重新发现，但保留用户显式配置的地址。
/// 间隔以毫秒计，实际两次尝试之间还包含前一次网络请求耗时。
fn spawn_student_reporter(state: StudentRuntimeState) {
    thread::spawn(/** 执行新工作线程的任务。 */ move || loop {
        if student_teacher_ip(&state).is_some() {
            if let Err(error) = student_report_once(&state) {
                student_set_error(&state, error);
                if configured_teacher_ip(&state).is_none() {
                    student_set_teacher_ip(&state, None);
                }
            }
        }
        thread::sleep(Duration::from_millis(student_report_interval_ms(&state)));
    });
}

/// 执行学生action。
/// 入参：action（action）；state（状态）。
fn execute_student_action(action: StudentActionRequest, state: StudentRuntimeState) {
    match action.action.as_str() {
        "refresh" => {
            if let Err(error) = student_report_once(&state) {
                student_set_error(&state, error);
            }
        }
        "message" => {
            let message = action.message.unwrap_or_else(/** 在当前结果缺失或失败时生成回退值。 */ || "教师端消息".to_string());
            let script = format!(
                "Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('{}','智云学生端')",
                message.replace('\'', "''")
            );
            let _ = Command::new("powershell.exe")
                .args([
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Command",
                    &script,
                ])
                .spawn();
        }
        "lock" => {
            let _ = Command::new("rundll32.exe")
                .args(["user32.dll,LockWorkStation"])
                .spawn();
        }
        "reboot" => {
            let _ = Command::new("shutdown.exe").args(["/r", "/t", "0"]).spawn();
        }
        "shutdown" => {
            let _ = Command::new("shutdown.exe").args(["/s", "/t", "0"]).spawn();
        }
        _ => student_set_error(&state, format!("unsupported action: {}", action.action)),
    }
}

/// 处理学生action请求。
/// 入参：state（状态）；request（请求）。
fn handle_student_action_request(state: &StudentRuntimeState, mut request: Request) {
    let mut body = String::new();
    if request.as_reader().read_to_string(&mut body).is_err() {
        respond_json(
            request,
            &serde_json::json!({ "ok": false, "message": "invalid json" }),
            400,
        );
        return;
    }

    let action = match serde_json::from_str::<StudentActionRequest>(&body) {
        Ok(value) => value,
        Err(_) => {
            respond_json(
                request,
                &serde_json::json!({ "ok": false, "message": "invalid json" }),
                400,
            );
            return;
        }
    };

    let state_for_action = state.clone();
    thread::spawn(/** 执行新工作线程的任务。 */ move || execute_student_action(action, state_for_action));
    respond_json(
        request,
        &serde_json::json!({ "ok": true, "message": "accepted" }),
        200,
    );
}

/// 启动学生action服务端。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：Result<(), String>；通过 Result 区分成功值与错误值。
fn start_student_action_server(state: StudentRuntimeState) -> Result<(), String> {
    let server = Server::http(("0.0.0.0", STUDENT_ACTION_PORT)).map_err(/** 处理 map_err 调用中的回调逻辑。 入参：error。 */ |error| {
        format!(
            "student action port {} unavailable: {}",
            STUDENT_ACTION_PORT, error
        )
    })?;

    thread::spawn(/** 执行新工作线程的任务。 */ move || loop {
        match server.recv_timeout(Duration::from_millis(500)) {
            Ok(Some(request)) => {
                let path = request.url().to_string();
                let method = request.method().clone();
                if method == Method::Options {
                    respond_options(request);
                } else if method == Method::Get && path == "/api/status" {
                    respond_json(request, &build_student_status_snapshot(&state), 200);
                } else if method == Method::Get && path == "/api/logs" {
                    let logs = state
                        .logs
                        .lock()
                        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
                        .unwrap_or_default();
                    respond_json(request, &serde_json::json!({ "logs": logs }), 200);
                } else if method == Method::Post && path == "/api/reload-settings" {
                    reload_student_settings(&state);
                    respond_json(request, &build_student_status_snapshot(&state), 200);
                } else if method == Method::Post && path == "/api/action" {
                    handle_student_action_request(&state, request);
                } else {
                    respond_json(
                        request,
                        &serde_json::json!({ "ok": false, "message": "not found" }),
                        404,
                    );
                }
            }
            Ok(None) => {}
            Err(error) => student_set_error(&state, format!("action server error: {}", error)),
        }
    });

    Ok(())
}

/// 当前学生状态文本的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：String。
#[allow(dead_code)]
fn current_student_status_text(state: &StudentRuntimeState) -> String {
    let last_error = state
        .last_error
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .unwrap_or_default();

    if last_error.contains("student action port") {
        return "学生端已在运行".to_string();
    }

    if let Some(teacher_ip) = student_teacher_ip(state) {
        return format!("已连接教师端：{}", teacher_ip);
    }

    let recently_reported = state
        .last_report_ms
        .lock()
        .ok()
        .and_then(/** 处理 and_then 调用中的回调逻辑。 入参：value。 */ |value| *value)
        .map(/** 将当前条目转换为目标结果。 入参：last_seen。 */ |last_seen| now_millis().saturating_sub(last_seen) <= OFFLINE_AFTER_MS)
        .unwrap_or(false);

    if recently_reported {
        "未连接教师端".to_string()
    } else {
        "正在查找教师端".to_string()
    }
}

/// setup学生tray的处理逻辑。
/// 入参：app（app）；state（状态）。
/// 返回：tauri::Result<()>。
#[allow(dead_code)]
fn setup_student_tray(app: &tauri::App, state: StudentRuntimeState) -> tauri::Result<()> {
    let status_item = MenuItem::with_id(app, "status", "正在查找教师端", false, None::<&str>)?;
    let reconnect_item = MenuItem::with_id(app, "reconnect", "重连", true, None::<&str>)?;
    let about_item = MenuItem::with_id(app, "about", "关于：智云学生端", false, None::<&str>)?;
    let menu = Menu::with_items(app, &[&status_item, &reconnect_item, &about_item])?;
    let icon = app
        .default_window_icon()
        .expect("application icon missing")
        .clone();

    let state_for_menu = state.clone();
    let tray = TrayIconBuilder::with_id("student-status")
        .tooltip("正在查找教师端")
        .icon(icon)
        .menu(&menu)
        .show_menu_on_left_click(true)
        .on_menu_event(/** 处理 on_menu_event 调用中的回调逻辑。 入参：_app、event。 */ move |_app, event| {
            if event.id().as_ref() == "reconnect" {
                student_set_teacher_ip(&state_for_menu, None);
                let state_for_report = state_for_menu.clone();
                thread::spawn(/** 执行新工作线程的任务。 */ move || {
                    let _ = student_report_once(&state_for_report);
                });
            }
        })
        .build(app)?;

    thread::spawn(/** 执行新工作线程的任务。 */ move || loop {
        let status = current_student_status_text(&state);
        let _ = status_item.set_text(&status);
        let _ = tray.set_tooltip(Some(&status));
        thread::sleep(Duration::from_millis(STUDENT_TRAY_REFRESH_MS));
    });

    Ok(())
}

/// 当前学生状态文本v2的处理逻辑。
/// 涉及：访问互斥保护的共享状态。
/// 入参：state（状态）。
/// 返回：String。
fn current_student_status_text_v2(state: &StudentRuntimeState) -> String {
    let last_error = state
        .last_error
        .lock()
        .map(/** 将当前条目转换为目标结果。 入参：value。 */ |value| value.clone())
        .unwrap_or_default();

    if last_error.contains("student action port") {
        return "学生端已在运行".to_string();
    }

    if let Some(teacher_ip) = student_teacher_ip(state) {
        return format!("已连接教师端：{}", teacher_ip);
    }

    let recently_reported = state
        .last_report_ms
        .lock()
        .ok()
        .and_then(/** 处理 and_then 调用中的回调逻辑。 入参：value。 */ |value| *value)
        .map(/** 将当前条目转换为目标结果。 入参：last_seen。 */ |last_seen| now_millis().saturating_sub(last_seen) <= OFFLINE_AFTER_MS)
        .unwrap_or(false);

    if recently_reported {
        "未连接教师端".to_string()
    } else {
        "正在查找教师端".to_string()
    }
}

/// 打开学生设置文件。
fn open_student_settings_file() {
    let settings = load_student_settings();
    save_student_settings(&settings);
    let _ = Command::new("notepad.exe")
        .arg(student_settings_path())
        .spawn();
}

/// 打开学生log文件。
fn open_student_log_file() {
    ensure_student_data_dir();
    let _ = OpenOptions::new()
        .create(true)
        .append(true)
        .open(student_log_path());
    let _ = Command::new("notepad.exe").arg(student_log_path()).spawn();
}

/// 打开学生数据folder。
fn open_student_data_folder() {
    ensure_student_data_dir();
    let _ = Command::new("explorer.exe").arg(student_data_dir()).spawn();
}

/// setup学生trayv2的处理逻辑。
/// 入参：app（app）；state（状态）。
/// 返回：tauri::Result<()>。
fn setup_student_tray_v2(app: &tauri::App, state: StudentRuntimeState) -> tauri::Result<()> {
    let status_item = MenuItem::with_id(app, "status", "正在查找教师端", false, None::<&str>)?;
    let reconnect_item = MenuItem::with_id(app, "reconnect", "重连", true, None::<&str>)?;
    let report_item = MenuItem::with_id(app, "report", "立即上报", true, None::<&str>)?;
    let reload_settings_item =
        MenuItem::with_id(app, "reload-settings", "重载配置", true, None::<&str>)?;
    let open_settings_item =
        MenuItem::with_id(app, "open-settings", "打开配置", true, None::<&str>)?;
    let open_log_item = MenuItem::with_id(app, "open-log", "打开日志", true, None::<&str>)?;
    let open_folder_item =
        MenuItem::with_id(app, "open-folder", "打开数据目录", true, None::<&str>)?;
    let about_item = MenuItem::with_id(app, "about", "关于：智云学生端", false, None::<&str>)?;
    let exit_item = MenuItem::with_id(app, "exit", "退出", true, None::<&str>)?;
    let menu = Menu::with_items(
        app,
        &[
            &status_item,
            &reconnect_item,
            &report_item,
            &reload_settings_item,
            &open_settings_item,
            &open_log_item,
            &open_folder_item,
            &about_item,
            &exit_item,
        ],
    )?;
    let icon = app
        .default_window_icon()
        .expect("application icon missing")
        .clone();

    let state_for_menu = state.clone();
    let tray = TrayIconBuilder::with_id("student-status")
        .tooltip("正在查找教师端")
        .icon(icon)
        .menu(&menu)
        .show_menu_on_left_click(true)
        .on_menu_event(/** 处理 on_menu_event 调用中的回调逻辑。 入参：app、event。 */ move |app, event| match event.id().as_ref() {
            "reconnect" => {
                student_set_teacher_ip(&state_for_menu, None);
                apply_configured_teacher_ip(&state_for_menu);
                let state_for_report = state_for_menu.clone();
                thread::spawn(/** 执行新工作线程的任务。 */ move || {
                    let _ = student_report_once(&state_for_report);
                });
            }
            "report" => {
                let state_for_report = state_for_menu.clone();
                thread::spawn(/** 执行新工作线程的任务。 */ move || {
                    let _ = student_report_once(&state_for_report);
                });
            }
            "reload-settings" => reload_student_settings(&state_for_menu),
            "open-settings" => open_student_settings_file(),
            "open-log" => open_student_log_file(),
            "open-folder" => open_student_data_folder(),
            "exit" => app.exit(0),
            _ => {}
        })
        .build(app)?;

    thread::spawn(/** 执行新工作线程的任务。 */ move || loop {
        let status = current_student_status_text_v2(&state);
        let _ = status_item.set_text(&status);
        let _ = tray.set_tooltip(Some(&status));
        thread::sleep(Duration::from_millis(STUDENT_TRAY_REFRESH_MS));
    });

    Ok(())
}

/// 启用学生autostart。
/// 入参：app（app）。
fn enable_student_autostart(app: &tauri::App) {
    if let Err(error) = app.autolaunch().enable() {
        eprintln!("student autostart enable failed: {}", error);
    }
}

/// 运行当前操作。
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let host_status = Arc::new(Mutex::new(default_local_host_status()));
    spawn_host_status_probe(host_status.clone());

    tauri::Builder::default()
        .manage(AppState {
            gateway: Arc::new(Mutex::new(GatewayInner::default())),
            host_status,
        })
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_autostart::init(
            tauri_plugin_autostart::MacosLauncher::LaunchAgent,
            Some(vec![]),
        ))
        .plugin(tauri_plugin_os::init())
        .invoke_handler(tauri::generate_handler![
            get_local_mac_address,
            get_local_host_status,
            get_gateway_snapshot,
            update_gateway_settings,
            start_gateway,
            stop_gateway
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

/// 运行学生。
pub fn run_student() {
    let host_status = Arc::new(Mutex::new(default_local_host_status()));
    spawn_host_status_probe(host_status.clone());

    let settings = load_student_settings();
    let student_state = StudentRuntimeState {
        host_status,
        teacher_ip: Arc::new(Mutex::new(None)),
        lan_teacher_url: Arc::new(Mutex::new(None)),
        settings: Arc::new(Mutex::new(settings)),
        last_report_ms: Arc::new(Mutex::new(None)),
        last_report_at: Arc::new(Mutex::new(None)),
        last_error: Arc::new(Mutex::new(String::new())),
        logs: Arc::new(Mutex::new(Vec::new())),
    };
    apply_configured_teacher_ip(&student_state);
    push_student_log(&student_state, "student client started");

    tauri::Builder::default()
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_autostart::init(
            tauri_plugin_autostart::MacosLauncher::LaunchAgent,
            Some(vec![]),
        ))
        .plugin(tauri_plugin_os::init())
        .setup(/** 处理 setup 调用中的回调逻辑。 入参：app。 */ move |app| {
            enable_student_autostart(app);
            match start_student_action_server(student_state.clone()) {
                Ok(()) => {
                    spawn_student_lan_connector(student_state.clone());
                    spawn_student_discovery(student_state.clone());
                    spawn_student_reporter(student_state.clone());
                    setup_student_tray_v2(app, student_state.clone())?;
                }
                Err(error) if error.contains("student action port") => {
                    push_student_log(&student_state, "another student client is already running");
                    app.handle().exit(0);
                }
                Err(error) => {
                    student_set_error(&student_state, error);
                    setup_student_tray_v2(app, student_state.clone())?;
                }
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running student client");
}

#[cfg(test)]
mod tests {
    use super::*;

    /// 收集载荷reads学生reportedipaddress。
    #[test]
    fn collect_payload_reads_student_reported_ip_address() {
        let payload: CollectPayload = serde_json::from_str(
            r#"{"deviceId":"student-1","hostname":"PC-01","ipAddress":"192.168.56.101"}"#,
        )
        .expect("collect payload should parse");

        assert_eq!(payload.ip_address.as_deref(), Some("192.168.56.101"));
    }

    /// 收集ipprefers学生reportedroutableaddress。
    #[test]
    fn collect_ip_prefers_student_reported_routable_address() {
        let selected = student_ip_from_collect(Some("192.168.56.101"), Some("172.16.0.20"));

        assert_eq!(selected, "192.168.56.101");
    }

    /// discovery目标includesubnetbroadcast的处理逻辑。
    #[test]
    fn discovery_targets_include_subnet_broadcast() {
        let targets = discovery_broadcast_targets_for_ip("192.168.56.42");

        assert_eq!(targets, vec!["255.255.255.255", "192.168.56.255"]);
    }
}
