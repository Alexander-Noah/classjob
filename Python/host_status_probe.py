# 主机状态辅助探针：提供状态 API 与上报命令；psutil 缺失时使用平台能力回退。
# 环境变量输出需经过敏感名称过滤，设备缓存由 DEVICE_LOCK 保护。
from __future__ import annotations

import argparse
import ctypes
import ipaddress
import json
import os
import platform
import shutil
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from datetime import datetime, timezone
from threading import Lock
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse

try:
    import psutil  # type: ignore
except ImportError:  # pragma: no cover - intentionally optional
    psutil = None


DEFAULT_DISK_PATH = "C:\\" if os.name == "nt" else "/"
DEFAULT_ENV_KEYS = [
    "COMPUTERNAME",
    "OS",
    "PROCESSOR_ARCHITECTURE",
    "NUMBER_OF_PROCESSORS",
    "JAVA_HOME",
    "PYTHONHOME",
    "PYTHONPATH",
    "Path",
]
SENSITIVE_ENV_WORDS = ("KEY", "SECRET", "TOKEN", "PASSWORD", "PASS", "PWD", "CREDENTIAL")
OFFLINE_AFTER_SECONDS = 30

DEVICE_LOCK = Lock()
DEVICE_CACHE: Dict[str, Dict[str, Any]] = {}
SERVER_LOGS: List[str] = []


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def local_now_text() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def now_millis() -> int:
    return int(time.time() * 1000)


def bytes_to_gib(value: float) -> float:
    return round(value / (1024**3), 2)


# 无有效总量时返回 None，区别于“有效采样且占用率为零”。
def percent(used: float, total: float) -> Optional[float]:
    if total <= 0:
        return None
    return round((used / total) * 100, 2)


def get_local_ips() -> List[str]:
    ips: List[str] = []
    try:
        host = socket.gethostname()
        for item in socket.getaddrinfo(host, None):
            ip = item[4][0]
            if ip not in ips and not ip.startswith("127.") and ip != "::1":
                ips.append(ip)
    except OSError:
        pass
    return sorted(ips, key=ip_sort_key)


def ip_sort_key(value: str) -> Tuple[int, str]:
    try:
        ip = ipaddress.ip_address(value)
    except ValueError:
        return (9, value)

    if ip.version == 4 and ip.is_private:
        return (0, value)
    if ip.version == 4:
        return (1, value)
    if ip.version == 6 and not ip.is_link_local:
        return (2, value)
    return (3, value)


def get_primary_ip() -> str:
    ips = get_local_ips()
    if ips:
        return ips[0]

    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.connect(("8.8.8.8", 80))
            return sock.getsockname()[0]
    except OSError:
        return "127.0.0.1"


def get_cpu_status(sample_seconds: float) -> Dict[str, Any]:
    logical_count = os.cpu_count()
    status: Dict[str, Any] = {
        "logical_count": logical_count,
        "usage_percent": None,
        "load_average": None,
    }

    if psutil:
        status["physical_count"] = psutil.cpu_count(logical=False)
        status["usage_percent"] = psutil.cpu_percent(interval=sample_seconds)
        status["per_cpu_percent"] = psutil.cpu_percent(interval=None, percpu=True)
        try:
            freq = psutil.cpu_freq()
            if freq:
                status["frequency_mhz"] = {
                    "current": round(freq.current, 2),
                    "min": round(freq.min, 2),
                    "max": round(freq.max, 2),
                }
        except (OSError, RuntimeError):
            pass
    else:
        if os.name == "nt":
            try:
                output = subprocess.check_output(
                    [
                        "powershell",
                        "-NoProfile",
                        "-Command",
                        r"(Get-Counter '\Processor(_Total)\% Processor Time' -SampleInterval 1 -MaxSamples 1).CounterSamples.CookedValue",
                    ],
                    text=True,
                    stderr=subprocess.DEVNULL,
                    encoding="utf-8",
                    errors="replace",
                    timeout=max(sample_seconds + 3, 4),
                ).strip()
                status["usage_percent"] = round(float(output), 2)
            except (OSError, ValueError, subprocess.SubprocessError):
                status["usage_percent"] = None
        elif hasattr(os, "getloadavg"):
            status["load_average"] = [round(v, 2) for v in os.getloadavg()]

    return status


def get_windows_memory() -> Optional[Dict[str, Any]]:
    class MEMORYSTATUSEX(ctypes.Structure):
        _fields_ = [
            ("dwLength", ctypes.c_ulong),
            ("dwMemoryLoad", ctypes.c_ulong),
            ("ullTotalPhys", ctypes.c_ulonglong),
            ("ullAvailPhys", ctypes.c_ulonglong),
            ("ullTotalPageFile", ctypes.c_ulonglong),
            ("ullAvailPageFile", ctypes.c_ulonglong),
            ("ullTotalVirtual", ctypes.c_ulonglong),
            ("ullAvailVirtual", ctypes.c_ulonglong),
            ("ullAvailExtendedVirtual", ctypes.c_ulonglong),
        ]

    memory = MEMORYSTATUSEX()
    memory.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
    if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)):  # type: ignore[attr-defined]
        return None

    total = float(memory.ullTotalPhys)
    available = float(memory.ullAvailPhys)
    used = total - available
    return {
        "total_gib": bytes_to_gib(total),
        "available_gib": bytes_to_gib(available),
        "used_gib": bytes_to_gib(used),
        "used_percent": round(float(memory.dwMemoryLoad), 2),
    }


def get_memory_status() -> Dict[str, Any]:
    if psutil:
        vm = psutil.virtual_memory()
        swap = psutil.swap_memory()
        return {
            "total_gib": bytes_to_gib(vm.total),
            "available_gib": bytes_to_gib(vm.available),
            "used_gib": bytes_to_gib(vm.used),
            "used_percent": round(vm.percent, 2),
            "swap_total_gib": bytes_to_gib(swap.total),
            "swap_used_gib": bytes_to_gib(swap.used),
            "swap_used_percent": round(swap.percent, 2),
        }

    if os.name == "nt":
        windows_memory = get_windows_memory()
        if windows_memory:
            return windows_memory

    return {
        "total_gib": None,
        "available_gib": None,
        "used_gib": None,
        "used_percent": None,
        "note": "Install psutil for detailed memory metrics on this platform.",
    }


def get_disk_status(paths: List[str]) -> List[Dict[str, Any]]:
    disks: List[Dict[str, Any]] = []
    seen = set()

    for path in paths:
        normalized = os.path.abspath(path)
        if normalized in seen:
            continue
        seen.add(normalized)

        try:
            usage = shutil.disk_usage(normalized)
        except OSError as exc:
            disks.append({"path": path, "error": str(exc)})
            continue

        disks.append(
            {
                "path": normalized,
                "total_gib": bytes_to_gib(usage.total),
                "used_gib": bytes_to_gib(usage.used),
                "free_gib": bytes_to_gib(usage.free),
                "used_percent": percent(usage.used, usage.total),
            }
        )

    return disks


def get_network_status() -> Dict[str, Any]:
    status: Dict[str, Any] = {
        "hostname": socket.gethostname(),
        "ips": get_local_ips(),
    }

    if psutil:
        counters = psutil.net_io_counters()
        status["io"] = {
            "bytes_sent_gib": bytes_to_gib(counters.bytes_sent),
            "bytes_recv_gib": bytes_to_gib(counters.bytes_recv),
            "packets_sent": counters.packets_sent,
            "packets_recv": counters.packets_recv,
            "errors_in": counters.errin,
            "errors_out": counters.errout,
            "drops_in": counters.dropin,
            "drops_out": counters.dropout,
        }

    return status


def get_process_count() -> Optional[int]:
    if psutil:
        return len(psutil.pids())

    command = ["tasklist"] if os.name == "nt" else ["ps", "-e"]
    try:
        output = subprocess.check_output(command, text=True, stderr=subprocess.DEVNULL)
    except (OSError, subprocess.SubprocessError):
        return None
    return max(len(output.splitlines()) - 1, 0)


def is_sensitive_env_key(key: str) -> bool:
    upper_key = key.upper()
    return any(word in upper_key for word in SENSITIVE_ENV_WORDS)


def get_environment_snapshot(keys: List[str]) -> Dict[str, Optional[str]]:
    env = dict(os.environ)
    snapshot: Dict[str, Optional[str]] = {}

    for key in keys:
        if is_sensitive_env_key(key):
            continue
        if key in env:
            snapshot[key] = env.get(key)

    return snapshot


def get_running_apps(limit: int, monitor_names: List[str]) -> List[Dict[str, Any]]:
    monitor_set = {name.lower() for name in monitor_names}
    apps: List[Dict[str, Any]] = []

    if psutil:
        for proc in psutil.process_iter(["pid", "name", "username", "cpu_percent", "memory_info", "exe", "status"]):
            try:
                info = proc.info
                name = info.get("name") or ""
                username = info.get("username") or ""
                if not name:
                    continue
                if monitor_set and name.lower() not in monitor_set:
                    continue
                if not monitor_set and username and "SYSTEM" in username.upper():
                    continue
                memory_info = info.get("memory_info")
                apps.append(
                    {
                        "pid": info.get("pid"),
                        "name": name,
                        "username": username,
                        "status": info.get("status"),
                        "cpu_percent": info.get("cpu_percent"),
                        "memory_mb": round(memory_info.rss / (1024 * 1024), 2) if memory_info else None,
                        "exe": info.get("exe"),
                    }
                )
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                continue
    elif os.name == "nt":
        try:
            output = subprocess.check_output(
                ["tasklist", "/fo", "csv", "/nh"],
                text=True,
                stderr=subprocess.DEVNULL,
                encoding="utf-8",
                errors="replace",
            )
            for line in output.splitlines():
                parts = [part.strip().strip('"') for part in line.split('","')]
                if len(parts) < 2:
                    continue
                name = parts[0]
                if monitor_set and name.lower() not in monitor_set:
                    continue
                apps.append({"name": name, "pid": parts[1]})
        except (OSError, subprocess.SubprocessError):
            pass

    apps.sort(key=lambda item: str(item.get("name", "")).lower())
    return apps[:limit]


def get_monitored_app_status(running_apps: List[Dict[str, Any]], monitor_names: List[str]) -> List[Dict[str, Any]]:
    if not monitor_names:
        return []

    running_names = {str(app.get("name", "")).lower() for app in running_apps}
    return [
        {
            "name": name,
            "running": name.lower() in running_names,
        }
        for name in monitor_names
    ]


def get_installed_software(limit: int, timeout: float) -> List[Dict[str, Any]]:
    if os.name != "nt":
        return []

    ps_command = r"""
    $paths = @(
      'HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*',
      'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
      'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
    )
    Get-ItemProperty $paths -ErrorAction SilentlyContinue |
      Where-Object { $_.DisplayName } |
      Select-Object DisplayName, DisplayVersion, Publisher, InstallDate |
      Sort-Object DisplayName -Unique |
      ConvertTo-Json -Compress
    """

    try:
        output = subprocess.check_output(
            ["powershell", "-NoProfile", "-Command", ps_command],
            text=True,
            stderr=subprocess.DEVNULL,
            encoding="utf-8",
            errors="replace",
            timeout=timeout,
        ).strip()
    except (OSError, subprocess.SubprocessError):
        return []

    if not output:
        return []

    try:
        parsed = json.loads(output)
    except json.JSONDecodeError:
        return []

    if isinstance(parsed, dict):
        software = [parsed]
    elif isinstance(parsed, list):
        software = parsed
    else:
        software = []

    normalized = [
        {
            "name": item.get("DisplayName"),
            "version": item.get("DisplayVersion"),
            "publisher": item.get("Publisher"),
            "install_date": item.get("InstallDate"),
        }
        for item in software
        if isinstance(item, dict) and item.get("DisplayName")
    ]
    return normalized[:limit]


def get_uptime_seconds() -> Optional[int]:
    if psutil:
        return int(time.time() - psutil.boot_time())

    if os.name != "nt":
        try:
            with open("/proc/uptime", "r", encoding="utf-8") as handle:
                return int(float(handle.read().split()[0]))
        except (OSError, ValueError, IndexError):
            return None

    return None


def evaluate_health(
    cpu_used: Optional[float],
    memory_used: Optional[float],
    disks: List[Dict[str, Any]],
    warn: float,
    critical: float,
) -> Dict[str, Any]:
    checks: List[Dict[str, Any]] = []

    def add_check(name: str, value: Optional[float]) -> None:
        if value is None:
            checks.append({"name": name, "status": "unknown", "value": None})
        elif value >= critical:
            checks.append({"name": name, "status": "critical", "value": value})
        elif value >= warn:
            checks.append({"name": name, "status": "warn", "value": value})
        else:
            checks.append({"name": name, "status": "ok", "value": value})

    add_check("cpu_usage_percent", cpu_used)
    add_check("memory_used_percent", memory_used)
    for disk in disks:
        add_check(f"disk_used_percent:{disk.get('path')}", disk.get("used_percent"))

    if any(check["status"] == "critical" for check in checks):
        overall = "critical"
    elif any(check["status"] == "warn" for check in checks):
        overall = "warn"
    elif all(check["status"] == "unknown" for check in checks):
        overall = "unknown"
    else:
        overall = "ok"

    return {"status": overall, "checks": checks}


def collect_status(args: argparse.Namespace) -> Dict[str, Any]:
    cpu = get_cpu_status(args.sample_seconds)
    memory = get_memory_status()
    disks = get_disk_status(args.disk_path)
    uptime_seconds = get_uptime_seconds()

    return {
        "probe": "host_status",
        "timestamp": utc_now_iso(),
        "system": {
            "hostname": socket.gethostname(),
            "platform": platform.platform(),
            "system": platform.system(),
            "release": platform.release(),
            "architecture": platform.machine(),
            "python": platform.python_version(),
            "uptime_seconds": uptime_seconds,
        },
        "cpu": cpu,
        "memory": memory,
        "disks": disks,
        "network": get_network_status(),
        "process_count": get_process_count(),
        "health": evaluate_health(
            cpu.get("usage_percent"),
            memory.get("used_percent"),
            disks,
            args.warn_percent,
            args.critical_percent,
        ),
        "receivedAt": utc_now_iso(),
    }


def build_local_host_payload(args: argparse.Namespace) -> Dict[str, Any]:
    data = collect_status(args)
    data["summary"] = {
        "hostName": data["system"]["hostname"],
        "ipAddress": (data["network"].get("ips") or [get_primary_ip()])[0],
        "status": "warning" if data["health"]["status"] in {"warn", "critical"} else "online",
        "lastSeen": local_now_text(),
        "metrics": {
            "cpuUsage": float(data["cpu"].get("usage_percent") or 0),
            "memoryUsage": float(data["memory"].get("used_percent") or 0),
            "diskUsage": float((data["disks"][0] or {}).get("used_percent") or 0) if data["disks"] else 0,
        },
    }
    return data


def push_server_log(message: str) -> None:
    SERVER_LOGS.append(f"{local_now_text()} {message}")
    if len(SERVER_LOGS) > 200:
        del SERVER_LOGS[:-200]


def normalize_collect_payload(payload: Dict[str, Any], ip_address: str) -> Tuple[Optional[str], Optional[Dict[str, Any]], Optional[str]]:
    device_id = str(payload.get("deviceId") or payload.get("device_id") or "").strip()
    if not device_id:
        return None, None, "deviceId required"

    hostname = str(payload.get("hostname") or payload.get("hostName") or payload.get("host_name") or "UNKNOWN").strip() or "UNKNOWN"
    metrics = payload.get("metrics") if isinstance(payload.get("metrics"), dict) else {}
    timestamp = payload.get("timestamp") or now_millis()

    entry = {
        "deviceId": device_id,
        "hostname": hostname,
        "timestamp": timestamp,
        "lastSeenMs": now_millis(),
        "ipAddress": ip_address,
        "metrics": {
            "cpuUsage": float(metrics.get("cpuUsage") or metrics.get("cpu_usage") or 0),
            "memoryUsage": float(metrics.get("memoryUsage") or metrics.get("memory_usage") or 0),
            "diskUsage": float(metrics.get("diskUsage") or metrics.get("disk_usage") or 0),
        },
    }
    return device_id, entry, None


def collect_device(payload: Dict[str, Any], ip_address: str) -> Tuple[bool, str]:
    device_id, entry, error = normalize_collect_payload(payload, ip_address)
    if error or not device_id or not entry:
        return False, error or "invalid payload"

    with DEVICE_LOCK:
        is_new = device_id not in DEVICE_CACHE
        DEVICE_CACHE[device_id] = entry
        if is_new:
            push_server_log(f"学生机 {entry['hostname']} 已接入，地址 {ip_address}")
    return True, "ok"


def build_gateway_snapshot(args: argparse.Namespace) -> Dict[str, Any]:
    current_ms = now_millis()
    with DEVICE_LOCK:
        devices = []
        for item in DEVICE_CACHE.values():
            metrics = item.get("metrics") or {}
            online = current_ms - int(item.get("lastSeenMs") or 0) <= OFFLINE_AFTER_SECONDS * 1000
            warning = (
                float(metrics.get("cpuUsage") or 0) >= args.warn_percent
                or float(metrics.get("memoryUsage") or 0) >= args.warn_percent
                or float(metrics.get("diskUsage") or 0) >= args.critical_percent
            )
            devices.append(
                {
                    "deviceId": item.get("deviceId"),
                    "hostname": item.get("hostname"),
                    "timestamp": item.get("timestamp"),
                    "lastSeen": item.get("lastSeenMs"),
                    "ipAddress": item.get("ipAddress"),
                    "metrics": metrics,
                    "online": online,
                    "warning": warning,
                }
            )

        devices.sort(key=lambda item: str(item.get("hostname") or ""))
        logs = list(SERVER_LOGS[-60:])

    return {
        "running": True,
        "lastError": "",
        "gatewayIp": get_primary_ip(),
        "connectedCount": len([item for item in devices if item["online"]]),
        "totalCount": len(devices),
        "cloud": {
            "ok": False,
            "message": "Python 设备状态服务运行中",
            "lastPushAt": None,
            "lastBatchSize": 0,
        },
        "devices": devices,
        "settings": {
            "cloudEndpoint": "",
            "businessSystemUrl": "",
        },
        "logs": logs,
    }


def collect_static_assets(args: argparse.Namespace) -> Dict[str, Any]:
    env_keys = list(dict.fromkeys(DEFAULT_ENV_KEYS + (args.env_key or [])))
    running_apps = get_running_apps(args.running_app_limit, args.monitor_app or [])
    installed_software = (
        get_installed_software(args.installed_software_limit, args.installed_software_timeout)
        if args.include_installed_software
        else []
    )

    return {
        "probe": "host_static_assets",
        "timestamp": utc_now_iso(),
        "system": {
            "hostname": socket.gethostname(),
            "platform": platform.platform(),
            "system": platform.system(),
            "release": platform.release(),
            "architecture": platform.machine(),
            "python": platform.python_version(),
        },
        "environment": get_environment_snapshot(env_keys),
        "running_apps": running_apps,
        "monitored_apps": get_monitored_app_status(running_apps, args.monitor_app or []),
        "installed_software": installed_software,
        "summary": {
            "environment_count": len(env_keys),
            "running_app_count": len(running_apps),
            "installed_software_count": len(installed_software),
        },
    }


def post_status(url: str, data: Dict[str, Any], timeout: float, token: Optional[str]) -> Dict[str, Any]:
    body = json.dumps(data, ensure_ascii=False).encode("utf-8")
    headers = {
        "Content-Type": "application/json; charset=utf-8",
        "User-Agent": "host-status-probe/1.0",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_body = response.read().decode("utf-8", errors="replace")
            return {
                "ok": 200 <= response.status < 300,
                "status_code": response.status,
                "response": response_body[:500],
            }
    except urllib.error.HTTPError as exc:
        response_body = exc.read().decode("utf-8", errors="replace")
        return {
            "ok": False,
            "status_code": exc.code,
            "error": response_body[:500],
        }
    except urllib.error.URLError as exc:
        return {
            "ok": False,
            "status_code": None,
            "error": str(exc.reason),
        }


def make_api_response(data: Any, message: str = "ok", code: int = 200) -> Dict[str, Any]:
    return {
        "code": code,
        "message": message,
        "data": data,
    }


class HostStatusRequestHandler(BaseHTTPRequestHandler):
    server_version = "HostStatusProbe/1.0"

    @property
    def probe_args(self) -> argparse.Namespace:
        return self.server.probe_args  # type: ignore[attr-defined]

    def log_message(self, format: str, *args: Any) -> None:
        if not getattr(self.server, "quiet", False):  # type: ignore[attr-defined]
            super().log_message(format, *args)

    def send_cors_headers(self) -> None:
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type,Authorization")
        self.send_header("Access-Control-Max-Age", "86400")

    def send_json(self, payload: Any, status: int = HTTPStatus.OK) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_cors_headers()
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self) -> None:
        self.send_response(HTTPStatus.NO_CONTENT)
        self.send_cors_headers()
        self.end_headers()

    def do_GET(self) -> None:
        path = urlparse(self.path).path.rstrip("/") or "/"

        if path in {"/health", "/api/health"}:
            self.send_json(make_api_response({"running": True, "time": local_now_text()}))
            return

        if path in {"/host-status/latest", "/api/host-status/latest"}:
            try:
                data = build_local_host_payload(self.probe_args)
                self.send_json(make_api_response(data))
            except Exception as exc:  # pragma: no cover - defensive HTTP boundary
                self.send_json(make_api_response(None, str(exc), 500), HTTPStatus.INTERNAL_SERVER_ERROR)
            return

        if path in {"/api/status", "/devices/status"}:
            self.send_json(build_gateway_snapshot(self.probe_args))
            return

        self.send_json(make_api_response(None, "not found", 404), HTTPStatus.NOT_FOUND)

    def do_POST(self) -> None:
        path = urlparse(self.path).path.rstrip("/") or "/"
        length = int(self.headers.get("Content-Length", "0") or 0)
        raw_body = self.rfile.read(length).decode("utf-8", errors="replace") if length > 0 else "{}"

        try:
            payload = json.loads(raw_body) if raw_body else {}
        except json.JSONDecodeError:
            self.send_json(make_api_response(None, "invalid json", 400), HTTPStatus.BAD_REQUEST)
            return

        if path in {"/api/collect", "/devices/collect"}:
            ok, message = collect_device(payload, self.client_address[0])
            status = HTTPStatus.OK if ok else HTTPStatus.BAD_REQUEST
            self.send_json(make_api_response({"accepted": ok}, message, int(status)), status)
            return

        self.send_json(make_api_response(None, "not found", 404), HTTPStatus.NOT_FOUND)


def serve_status_api(args: argparse.Namespace) -> int:
    server = ThreadingHTTPServer((args.host, args.port), HostStatusRequestHandler)
    server.probe_args = args  # type: ignore[attr-defined]
    server.quiet = args.quiet  # type: ignore[attr-defined]
    push_server_log(f"Python 设备状态服务已启动，监听 {args.host}:{args.port}")

    if not args.quiet:
        print(f"Host status API listening on http://{args.host}:{args.port}")
        print("GET  /host-status/latest")
        print("GET  /api/status")
        print("POST /api/collect")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        if not args.quiet:
            print("\nHost status API stopped.")
    finally:
        server.server_close()
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Collect host status as JSON.")
    parser.add_argument(
        "--disk-path",
        action="append",
        default=[DEFAULT_DISK_PATH],
        help="Disk path to probe. Can be used multiple times. Default: system root.",
    )
    parser.add_argument(
        "--sample-seconds",
        type=float,
        default=1.0,
        help="CPU sampling window in seconds when psutil is available. Default: 1.0.",
    )
    parser.add_argument(
        "--warn-percent",
        type=float,
        default=80.0,
        help="Warn threshold for CPU/memory/disk usage percent. Default: 80.",
    )
    parser.add_argument(
        "--critical-percent",
        type=float,
        default=90.0,
        help="Critical threshold for CPU/memory/disk usage percent. Default: 90.",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="Pretty-print JSON output.",
    )
    parser.add_argument(
        "--fail-on-unhealthy",
        action="store_true",
        help="Exit with non-zero status when health is warn or critical.",
    )
    parser.add_argument(
        "--post-url",
        help="HTTP endpoint to receive the collected JSON by POST.",
    )
    parser.add_argument(
        "--post-timeout",
        type=float,
        default=5.0,
        help="HTTP POST timeout in seconds. Default: 5.",
    )
    parser.add_argument(
        "--token",
        help="Bearer token used in the Authorization header when posting.",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Do not print the probe JSON. Useful for scheduled upload jobs.",
    )
    parser.add_argument(
        "--static-post-url",
        help="HTTP endpoint to receive environment/app/software static assets by POST.",
    )
    parser.add_argument(
        "--env-key",
        action="append",
        help="Additional environment variable key to include. Can be used multiple times.",
    )
    parser.add_argument(
        "--monitor-app",
        action="append",
        help="Application process name to monitor, such as idea64.exe. Can be used multiple times.",
    )
    parser.add_argument(
        "--running-app-limit",
        type=int,
        default=200,
        help="Maximum running app records in static asset payload. Default: 200.",
    )
    parser.add_argument(
        "--include-installed-software",
        action="store_true",
        help="Include installed software inventory in static asset payload.",
    )
    parser.add_argument(
        "--installed-software-limit",
        type=int,
        default=500,
        help="Maximum installed software records in static asset payload. Default: 500.",
    )
    parser.add_argument(
        "--installed-software-timeout",
        type=float,
        default=20.0,
        help="PowerShell installed software query timeout in seconds. Default: 20.",
    )
    parser.add_argument(
        "--interval",
        type=float,
        default=0,
        help="Repeat status reporting every N seconds. 0 means run once. Default: 0.",
    )
    parser.add_argument(
        "--static-interval-seconds",
        type=float,
        default=86400,
        help="Repeat static asset reporting every N seconds when --interval is used. Default: 86400.",
    )
    parser.add_argument(
        "--serve",
        action="store_true",
        help="Run a local HTTP API for frontend polling and device collection.",
    )
    parser.add_argument(
        "--host",
        default="127.0.0.1",
        help="HTTP API host when --serve is used. Default: 127.0.0.1.",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=8765,
        help="HTTP API port when --serve is used. Default: 8765.",
    )
    return parser.parse_args()


def report_status_once(args: argparse.Namespace) -> int:
    data = collect_status(args)

    post_result = None
    if args.post_url:
        post_result = post_status(args.post_url, data, args.post_timeout, args.token)

    if not args.quiet:
        print(json.dumps(data, ensure_ascii=False, indent=2 if args.pretty else None))
        if post_result is not None:
            print(json.dumps({"post": post_result}, ensure_ascii=False, indent=2 if args.pretty else None))

    if post_result is not None and not post_result["ok"]:
        return 3

    if args.fail_on_unhealthy and data["health"]["status"] in {"warn", "critical"}:
        return 2
    return 0


def report_static_assets_once(args: argparse.Namespace) -> int:
    if not args.static_post_url:
        return 0

    data = collect_static_assets(args)
    post_result = post_status(args.static_post_url, data, args.post_timeout, args.token)

    if not args.quiet:
        print(json.dumps({"static_assets": data, "post": post_result}, ensure_ascii=False, indent=2 if args.pretty else None))

    return 0 if post_result["ok"] else 4


def main() -> int:
    args = parse_args()

    if args.serve:
        return serve_status_api(args)

    static_exit_code = report_static_assets_once(args)
    if args.interval <= 0:
        status_exit_code = report_status_once(args)
        return static_exit_code or status_exit_code

    next_static_report_at = time.time() + max(args.static_interval_seconds, args.interval)
    while True:
        status_exit_code = report_status_once(args)
        if status_exit_code not in (0, 2):
            return status_exit_code

        if args.static_post_url and time.time() >= next_static_report_at:
            static_exit_code = report_static_assets_once(args)
            if static_exit_code != 0:
                return static_exit_code
            next_static_report_at = time.time() + args.static_interval_seconds

        time.sleep(args.interval)


if __name__ == "__main__":
    sys.exit(main())
