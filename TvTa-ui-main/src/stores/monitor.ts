/*
 * monitor的 Pinia 状态模块，集中维护桌面页面共用状态。
 * 主要导出：DeviceMetrics、DeviceAction、DeviceRecord、GatewayStatus、LocalHostStatus 等。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { invoke } from '@tauri-apps/api/core'
import { useActivationStore } from '@/stores/activation'
import { getApiBaseUrl } from '@/utils/request'

/** Rust 网关快照到 UI 的适配模型；状态采集和设备指令通过 Tauri invoke 执行。 */
export interface DeviceMetrics {
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
}

export type DeviceAction = 'view' | 'refresh' | 'wake' | 'reboot' | 'shutdown' | 'lock' | 'message' | 'remove'

export interface DeviceRecord {
  id: string
  hostName: string
  location: string
  status: 'online' | 'offline' | 'warning'
  lastSeen: string
  metrics: DeviceMetrics
  ipAddress: string
  firstSeen?: string
  savedAt?: string
  actionState?: string
}

export interface GatewayStatus {
  running: boolean
  gatewayIp: string
  cloudForwardingEnabled: boolean
  cloudEndpoint: string
  lastSyncText: string
}

export interface LocalHostStatus {
  hostName: string
  ipAddress: string
  status: 'online' | 'warning' | 'offline'
  lastSeen: string
  metrics: DeviceMetrics
}

interface GatewaySettings {
  cloudEndpoint: string
  businessSystemUrl: string
}

// 快照字段允许缺失，兼容网关初始化与较早版本；展示层负责补默认值。
interface GatewaySnapshot {
  running?: boolean
  lastError?: string
  gatewayIp?: string
  connectedCount?: number
  totalCount?: number
  cloud?: {
    ok?: boolean
    message?: string
    lastPushAt?: number | string | null
  }
  devices?: Array<{
    deviceId?: string
    hostname?: string
    online?: boolean
    warning?: boolean
    lastSeen?: number
    timestamp?: number
    ipAddress?: string
    metrics?: {
      cpuUsage?: number
      memoryUsage?: number
      diskUsage?: number
    }
  }>
  settings?: GatewaySettings
  logs?: string[]
}

interface LocalHostStatusResult {
  hostName?: string
  ipAddress?: string
  status?: 'online' | 'warning' | 'offline'
  lastSeen?: string
  metrics?: {
    cpuUsage?: number
    memoryUsage?: number
    diskUsage?: number
  }
}

interface LocalHostProbeResult {
  code?: number
  message?: string
  data?: Record<string, any>
}

interface RefreshOptions {
  silent?: boolean
}

interface LocalMonitorCache {
  gateway?: GatewayStatus
  devices?: DeviceRecord[]
  localHost?: LocalHostStatus
  logs?: string[]
  statusMessage?: string
  updatedAt?: string
}

const GATEWAY_BASE_URL = import.meta.env.VITE_GATEWAY_BASE_URL || 'http://127.0.0.1:5000'
const LOCAL_PROBE_BASE_URL = import.meta.env.VITE_LOCAL_PROBE_BASE_URL || 'http://127.0.0.1:8765'
const DEFAULT_BUSINESS_SYSTEM_URL = import.meta.env.VITE_BUSINESS_SYSTEM_URL || 'http://127.0.0.1:5173'
const MONITOR_CACHE_KEY = 'tvta-ui-monitor-local-cache'

/**
 * 格式化日期时间。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function formatDateTime(value?: number | string | null) {
  if (!value) return '--'
  const date = typeof value === 'number' ? new Date(value) : new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', { hour12: false })
}

/**
 * 构建Location。
 * 入参：index（索引）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function buildLocation(index: number) {
  return `PC-${String(index + 1).padStart(2, '0')}`
}

/**
 * 判断是否为Tauri运行时。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function isTauriRuntime() {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
}

/**
 * tryInvoke的处理逻辑。
 * 入参：command（命令）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
async function tryInvoke<T>(command: string, payload?: Record<string, unknown>) {
  if (!isTauriRuntime()) {
    throw new Error('not running in Tauri desktop runtime')
  }
  return invoke<T>(command, payload)
}

/**
 * 归一化网关设备。
 * 入参：items（条目集合）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function normalizeGatewayDevices(items: GatewaySnapshot['devices'] = []): DeviceRecord[] {
  return items.map(/** 将当前条目转换为目标结果。 入参：item、index。 */ (item, index) => ({
    id: item.deviceId || `unknown-${index + 1}`,
    hostName: item.hostname || 'UNKNOWN',
    location: buildLocation(index),
    status: item.online ? (item.warning ? 'warning' : 'online') : 'offline',
    lastSeen: formatDateTime(item.lastSeen || item.timestamp),
    ipAddress: item.ipAddress || '--',
    metrics: {
      cpuUsage: Number(item.metrics?.cpuUsage ?? 0),
      memoryUsage: Number(item.metrics?.memoryUsage ?? 0),
      diskUsage: Number(item.metrics?.diskUsage ?? 0)
    },
    firstSeen: formatDateTime(item.timestamp),
    savedAt: formatDateTime(Date.now())
  }))
}

/**
 * 获取设备Sort键。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function getDeviceSortKey(device: DeviceRecord) {
  const locationNumber = device.location.match(/\d+/)?.[0]
  if (locationNumber) return Number(locationNumber)
  const hostNumber = device.hostName.match(/\d+/)?.[0]
  if (hostNumber) return Number(hostNumber)
  return Number.MAX_SAFE_INTEGER
}

/**
 * 转换为Offline设备。
 * 入参：device（设备）。
 * @returns 组装的键值对象。
 */
function toOfflineDevice(device: DeviceRecord): DeviceRecord {
  return {
    ...device,
    status: 'offline',
    metrics: {
      cpuUsage: 0,
      memoryUsage: 0,
      diskUsage: 0
    },
    actionState: device.actionState || 'waiting for student device online'
  }
}

/**
 * 判断是否为本地Probe设备。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function isLocalProbeDevice(device: DeviceRecord) {
  const text = `${device.id} ${device.hostName} ${device.location}`.toLowerCase()
  return text.includes('local') || text.includes('probe') || text.includes('unknown')
}

/**
 * 构建设置。
 * 入参：endpoint（端点）。
 * @returns 组装的键值对象。
 */
function buildSettings(endpoint: string): GatewaySettings {
  return {
    cloudEndpoint: endpoint,
    businessSystemUrl: DEFAULT_BUSINESS_SYSTEM_URL
  }
}

/**
 * 读取本地监控缓存。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function readLocalMonitorCache(): LocalMonitorCache | null {
  if (typeof window === 'undefined') return null

  try {
    const raw = window.localStorage.getItem(MONITOR_CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as LocalMonitorCache
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

/**
 * 写入本地监控缓存。
 * 入参：payload（载荷）。
 */
function writeLocalMonitorCache(payload: LocalMonitorCache) {
  if (typeof window === 'undefined') return

  window.localStorage.setItem(
    MONITOR_CACHE_KEY,
    JSON.stringify({
      ...payload,
      updatedAt: new Date().toISOString()
    })
  )
}

export const useMonitorStore = defineStore('monitor', /** 处理 defineStore 调用中的回调逻辑。 */ () => {
  const gateway = ref<GatewayStatus>({
    running: false,
    gatewayIp: '--',
    cloudForwardingEnabled: false,
    cloudEndpoint: '',
    lastSyncText: 'not synced yet'
  })
  const localHost = ref<LocalHostStatus>({
    hostName: 'local host',
    ipAddress: '--',
    status: 'offline',
    lastSeen: 'not collected yet',
    metrics: {
      cpuUsage: 0,
      memoryUsage: 0,
      diskUsage: 0
    }
  })
  const devices = ref<DeviceRecord[]>([])
  const logs = ref<string[]>([])
  const loading = ref(false)
  const usingDemoData = ref(true)
  const statusMessage = ref('showing demo data')
  const pollTimer = ref<number | null>(null)
  const refreshInFlight = ref(false)
  const hydrated = ref(false)
  const lastLanTeacherConfigKey = ref('')

  const onlineCount = computed(/** 计算派生状态。 */ () => devices.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item => item.status === 'online').length)
  const warningCount = computed(/** 计算派生状态。 */ () => devices.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item => item.status === 'warning').length)
  const offlineCount = computed(/** 计算派生状态。 */ () => devices.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item => item.status === 'offline').length)
  const totalCount = computed(/** 计算派生状态。 */ () => devices.value.length)
  const onlineRate = computed(/** 计算派生状态。 */ () => {
    if (!devices.value.length) return '0%'
    return `${Math.round((onlineCount.value / devices.value.length) * 100)}%`
  })

  /**
   * 持久化本地状态。
   */
  function persistLocalState() {
    writeLocalMonitorCache({
      gateway: gateway.value,
      devices: devices.value,
      localHost: localHost.value,
      logs: logs.value,
      statusMessage: statusMessage.value
    })
  }

  /**
   * 合并Known设备。
   * 入参：nextDevices（next设备）。
   */
  function mergeKnownDevices(nextDevices: DeviceRecord[]) {
    const knownMap = new Map(devices.value.map(/** 将当前条目转换为目标结果。 入参：device。 */ device => [device.id, device]))
    const incomingIds = new Set(nextDevices.map(/** 将当前条目转换为目标结果。 入参：device。 */ device => device.id))
    const merged: DeviceRecord[] = nextDevices.map(/** 将当前条目转换为目标结果。 入参：device、index。 */ (device, index) => {
      const known = knownMap.get(device.id)
      return {
        ...known,
        ...device,
        location: known?.location || device.location || buildLocation(index),
        firstSeen: known?.firstSeen || device.firstSeen || device.lastSeen,
        savedAt: known?.savedAt || device.savedAt || formatDateTime(Date.now()),
        actionState: known?.actionState
      }
    })

    devices.value
      .filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ device => !incomingIds.has(device.id))
      .forEach(/** 处理遍历到的当前条目。 入参：device。 */ device => {
        merged.push(toOfflineDevice(device))
      })

    devices.value = merged.sort(/** 比较当前两项的排序先后。 入参：left、right。 */ (left, right) => {
      const locationCompare = getDeviceSortKey(left) - getDeviceSortKey(right)
      if (locationCompare !== 0) return locationCompare
      return left.hostName.localeCompare(right.hostName, 'zh-CN')
    })
  }

  /**
   * hydrate的处理逻辑。
   */
  function hydrate() {
    if (hydrated.value) return
    hydrated.value = true

    const cache = readLocalMonitorCache()
    if (!cache) return

    if (cache.gateway) {
      gateway.value = {
        running: Boolean(cache.gateway.running),
        gatewayIp: cache.gateway.gatewayIp || '--',
        cloudForwardingEnabled: Boolean(cache.gateway.cloudForwardingEnabled),
        cloudEndpoint: cache.gateway.cloudEndpoint || '',
        lastSyncText: cache.gateway.lastSyncText || 'read from local cache'
      }
    }
    if (Array.isArray(cache.devices) && cache.devices.length) {
      devices.value = cache.devices.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ device => !isLocalProbeDevice(device))
    }
    if (cache.localHost) {
      localHost.value = cache.localHost
    }
    if (Array.isArray(cache.logs)) {
      logs.value = cache.logs
    }
    usingDemoData.value = false
    statusMessage.value = cache.statusMessage || 'loaded local client cache'
  }

  /**
   * 应用网关快照。
   * 入参：snapshot（快照）。
   */
  function applyGatewaySnapshot(snapshot: GatewaySnapshot) {
    const nextDevices = normalizeGatewayDevices(snapshot.devices || [])
    const lastSyncText = formatDateTime(snapshot.cloud?.lastPushAt)

    gateway.value = {
      running: Boolean(snapshot.running),
      gatewayIp: snapshot.gatewayIp || '--',
      cloudForwardingEnabled: Boolean(snapshot.settings?.cloudEndpoint),
      cloudEndpoint: snapshot.settings?.cloudEndpoint || '',
      lastSyncText: lastSyncText === '--' ? 'cloud sync not started' : lastSyncText
    }

    mergeKnownDevices(nextDevices)

    if (Array.isArray(snapshot.logs) && snapshot.logs.length) {
      logs.value = snapshot.logs.slice(-60).reverse()
    }

    usingDemoData.value = false
    statusMessage.value = snapshot.lastError || snapshot.cloud?.message || 'connected to local teacher gateway'
    persistLocalState()
  }

  /**
   * 获取网关状态。
   * 涉及：发起 HTTP 请求。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async function fetchGatewayStatus() {
    const response = await fetch(`${GATEWAY_BASE_URL}/api/status`)
    if (!response.ok) {
      throw new Error(`闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偞鐗犻、鏇㈡晜閽樺缃曟繝鐢靛Т閿曘倝鎮ч崱娆戠焼闁割偆鍠撶粻楣冩煕閳╁喚娈滈柣娑欑矒閺屻倝寮堕幐搴′淮闂佽鍠楅〃濠偽涢崘銊㈡婵炲棙鍔曢弸鎴犵磽娓氣偓缁犳牜绮婚弽顐や笉闁哄稁鍘奸拑鐔兼煟閺冨洢鈧偓闁稿鎸搁埥澶娾枎濡厧濮虹紓鍌欒兌婵敻骞愭ィ鍐ㄧ疅闁归棿绀佺粻銉︺亜閺傝法姣為柡瀣嚇濮婃椽宕崟顓犱紘闂佸摜濮甸悧鐘绘偘椤旂晫鐟归柍褜鍓熼悰顕€骞掑Δ鈧粻锝嗙節闂堟稒锛嶆繛鍏肩洴濮婃椽鎳￠妶鍛€剧紓渚囧枛鐎涒晠寮茬捄浣曟棃宕橀鍡欏幆闂備礁婀遍崕銈咁潖婵犳碍鍎楁繛鍡楁禋閻斿棝鎮规潪鎷岊劅闁绘搩鍓熼弻娑橆潩椤掑鍓堕梺鍝勬湰缁嬫垿鍩ユ径鎰闁绘劘灏欓崫妤佷繆閻愵亜鈧牠寮婚妸鈹у洭妫冨☉杈ㄧ稁?(${response.status})`)
    }
    return response.json() as Promise<GatewaySnapshot>
  }

  /**
   * 获取网关快照ByInvoke。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async function fetchGatewaySnapshotByInvoke() {
    return tryInvoke<GatewaySnapshot>('get_gateway_snapshot')
  }

  /**
   * 同步Lan教师配置。
   */
  async function syncLanTeacherConfig() {
    if (!isTauriRuntime()) return
    const activationStore = useActivationStore()
    activationStore.hydrate()
    if (!activationStore.isLicensed || !activationStore.labId || !activationStore.token) return

    const apiBaseUrl = getApiBaseUrl()
    const configKey = `${activationStore.labId}|${activationStore.token}|${apiBaseUrl}`
    if (configKey === lastLanTeacherConfigKey.value) return

    await tryInvoke('configure_lan_teacher', {
      labId: activationStore.labId,
      token: activationStore.token,
      apiBaseUrl
    })
    lastLanTeacherConfigKey.value = configKey
  }

  /**
   * 获取本地主机状态。
   * 涉及：发起 HTTP 请求。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async function fetchLocalHostStatus() {
    try {
      return await tryInvoke<LocalHostStatusResult>('get_local_host_status')
    } catch (invokeError) {
      const response = await fetch(`${LOCAL_PROBE_BASE_URL}/host-status/latest`)
      if (!response.ok) {
        throw invokeError
      }
      const result = await response.json() as LocalHostProbeResult
      const data = result?.data || {}
      const summary = data.summary || {}
      return {
        hostName: summary.hostName,
        ipAddress: summary.ipAddress,
        status: summary.status,
        lastSeen: summary.lastSeen,
        metrics: summary.metrics
      } satisfies LocalHostStatusResult
    }
  }

  /**
   * 应用本地主机状态。
   * 入参：status（状态）。
   */
  function applyLocalHostStatus(status: LocalHostStatusResult) {
    const hostname = status.hostName || 'UNKNOWN'
    const cpuUsage = Number(status.metrics?.cpuUsage ?? 0)
    const memoryUsage = Number(status.metrics?.memoryUsage ?? 0)
    const diskUsage = Number(status.metrics?.diskUsage ?? 0)
    const ipAddress = status.ipAddress || '--'

    localHost.value = {
      hostName: hostname,
      ipAddress,
      status: status.status || (cpuUsage >= 85 || memoryUsage >= 85 || diskUsage >= 90 ? 'warning' : 'online'),
      lastSeen: status.lastSeen || 'just now',
      metrics: {
        cpuUsage,
        memoryUsage,
        diskUsage
      }
    }

    gateway.value = {
      running: true,
      gatewayIp: ipAddress,
      cloudForwardingEnabled: false,
      cloudEndpoint: '',
      lastSyncText: status.lastSeen || 'just now'
    }

    logs.value = [
      'using local client probe data',
      `Host: ${hostname}`,
      `CPU ${cpuUsage.toFixed(1)}% / Memory ${memoryUsage.toFixed(1)}% / Disk ${diskUsage.toFixed(1)}%`
    ]

    usingDemoData.value = false
    statusMessage.value = devices.value.length
      ? 'connected to local client probe; waiting for student devices'
      : 'connected to local client probe; waiting for student devices to come online'
    persistLocalState()
  }

  /**
   * 刷新本地主机状态。
   * 入参：silent（silent）。
   */
  async function refreshLocalHostStatus(silent = true) {
    void silent
    const status = await fetchLocalHostStatus()
    applyLocalHostStatus(status)
  }

  /**
   * 刷新本地主机状态进入Background。
   */
  function refreshLocalHostStatusInBackground() {
    void refreshLocalHostStatus(true).catch(/** 处理异步失败结果。 */ () => {
      // Keep the device panel refresh stable even if the local probe is temporarily unavailable.
    })
  }

  /**
   * 刷新当前操作。
   * 入参：options（选项）。
   */
  async function refresh(options: RefreshOptions = {}) {
    if (refreshInFlight.value) {
      return
    }
    refreshInFlight.value = true
    if (!options.silent) {
      loading.value = true
    }
    try {
      await syncLanTeacherConfig().catch(/** 处理异步失败结果。 */ () => {
        // LAN teacher registration is best-effort; the legacy local gateway still runs.
      })
      const snapshot = await fetchGatewayStatus()
      applyGatewaySnapshot(snapshot)
      refreshLocalHostStatusInBackground()
      return
    } catch (_gatewayHttpError) {
      try {
        const snapshot = await fetchGatewaySnapshotByInvoke()
        if (snapshot.running || (snapshot.devices && snapshot.devices.length) || snapshot.logs?.length) {
          applyGatewaySnapshot(snapshot)
          refreshLocalHostStatusInBackground()
          return
        }
      } catch (_invokeError) {
        // Ignore and continue to local host probe fallback.
      }

      try {
        await refreshLocalHostStatus(Boolean(options.silent))
      } catch (localProbeError: any) {
        const hasLocalData = devices.value.length > 0 || logs.value.length > 0
        usingDemoData.value = !hasLocalData
        statusMessage.value = hasLocalData
          ? 'local gateway unavailable; showing last local data'
          : localProbeError?.message || 'local gateway unavailable; showing demo data'
      }
    } finally {
      if (!options.silent) {
        loading.value = false
      }
      refreshInFlight.value = false
    }
  }

  /**
   * 启动Polling。
   * 入参：intervalMs（intervalMs）。
   */
  function startPolling(intervalMs = 5000) {
    hydrate()
    if (pollTimer.value) {
      window.clearInterval(pollTimer.value)
    }
    void refresh({ silent: true })
    pollTimer.value = window.setInterval(/** 执行周期安排的任务。 */ () => {
      void refresh({ silent: true })
    }, intervalMs)
  }

  /**
   * 停止Polling。
   */
  function stopPolling() {
    if (pollTimer.value) {
      window.clearInterval(pollTimer.value)
      pollTimer.value = null
    }
  }

  /**
   * 启动网关。
   */
  async function startGateway() {
    loading.value = true
    try {
      const snapshot = await tryInvoke<GatewaySnapshot>('start_gateway')
      applyGatewaySnapshot(snapshot)
    } finally {
      loading.value = false
    }
  }

  /**
   * 停止网关。
   */
  async function stopGateway() {
    loading.value = true
    try {
      const snapshot = await tryInvoke<GatewaySnapshot>('stop_gateway')
      applyGatewaySnapshot(snapshot)
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新Cloud端点。
   * 入参：endpoint（端点）。
   */
  async function updateCloudEndpoint(endpoint: string) {
    loading.value = true
    try {
      const snapshot = await tryInvoke<GatewaySnapshot>('update_gateway_settings', {
        settings: buildSettings(endpoint)
      })
      applyGatewaySnapshot(snapshot)
    } catch (error) {
      gateway.value = {
        ...gateway.value,
        cloudEndpoint: endpoint,
        cloudForwardingEnabled: Boolean(endpoint)
      }
      statusMessage.value = 'gateway unavailable; cloud endpoint saved locally'
      persistLocalState()
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新设备Action状态。
   * 入参：deviceId（设备标识）；actionState（action状态）。
   */
  function updateDeviceActionState(deviceId: string, actionState: string) {
    devices.value = devices.value.map(/** 将当前条目转换为目标结果。 入参：device。 */ device =>
      device.id === deviceId
        ? {
            ...device,
            actionState
          }
        : device
    )
    persistLocalState()
  }

  /**
   * 移除设备。
   * 入参：deviceId（设备标识）。
   */
  function removeDevice(deviceId: string) {
    const target = devices.value.find(/** 判断当前条目是否为查找目标。 入参：device。 */ device => device.id === deviceId)
    devices.value = devices.value.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ device => device.id !== deviceId)
    if (target) {
      logs.value = [`${formatDateTime(Date.now())} removed ${target.location} (${target.hostName})`, ...logs.value].slice(0, 60)
    }
    persistLocalState()
  }

  /**
   * 请求设备Action的处理逻辑。
   * 涉及：发起 HTTP 请求。
   * 入参：deviceId（设备标识）；action（action）。
   */
  async function requestDeviceAction(deviceId: string, action: Exclude<DeviceAction, 'view' | 'remove'>) {
    const target = devices.value.find(/** 判断当前条目是否为查找目标。 入参：device。 */ device => device.id === deviceId)
    if (!target) {
      throw new Error('device not found')
    }

    const actionLabelMap: Record<Exclude<DeviceAction, 'view' | 'remove'>, string> = {
      refresh: 'refresh',
      wake: 'wake',
      reboot: 'reboot',
      shutdown: 'shutdown',
      lock: 'lock',
      message: 'message'
    }
    const actionLabel = actionLabelMap[action]

    try {
      const response = await fetch(`${GATEWAY_BASE_URL}/api/devices/${encodeURIComponent(deviceId)}/action`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ action })
      })
      if (!response.ok) {
        throw new Error(`gateway rejected action (${response.status})`)
      }
      updateDeviceActionState(deviceId, `${actionLabel} sent`)
      logs.value = [`${formatDateTime(Date.now())} ${target.location} ${actionLabel} sent to student device`, ...logs.value].slice(0, 60)
      persistLocalState()
      return
    } catch (_httpError) {
      try {
        await tryInvoke('send_device_action', { deviceId, action })
        updateDeviceActionState(deviceId, `${actionLabel} sent`)
        logs.value = [`${formatDateTime(Date.now())} ${target.location} ${actionLabel} sent through local channel`, ...logs.value].slice(0, 60)
        persistLocalState()
        return
      } catch (_invokeError) {
        updateDeviceActionState(deviceId, `${actionLabel} recorded; waiting for student channel`)
        logs.value = [`${formatDateTime(Date.now())} ${target.location} ${actionLabel} recorded; local student channel unavailable`, ...logs.value].slice(0, 60)
      }
    }
  }

  return {
    gateway,
    localHost,
    devices,
    logs,
    loading,
    usingDemoData,
    statusMessage,
    onlineCount,
    warningCount,
    offlineCount,
    totalCount,
    onlineRate,
    hydrate,
    refresh,
    startPolling,
    stopPolling,
    startGateway,
    stopGateway,
    updateCloudEndpoint,
    requestDeviceAction,
    removeDevice
  }
})
