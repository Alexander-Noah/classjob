import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { invoke } from '@tauri-apps/api/core'
import { useActivationStore } from '@/stores/activation'
import { getApiBaseUrl } from '@/utils/request'

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

function formatDateTime(value?: number | string | null) {
  if (!value) return '--'
  const date = typeof value === 'number' ? new Date(value) : new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function buildLocation(index: number) {
  return `PC-${String(index + 1).padStart(2, '0')}`
}

function isTauriRuntime() {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
}

async function tryInvoke<T>(command: string, payload?: Record<string, unknown>) {
  if (!isTauriRuntime()) {
    throw new Error('not running in Tauri desktop runtime')
  }
  return invoke<T>(command, payload)
}

function normalizeGatewayDevices(items: GatewaySnapshot['devices'] = []): DeviceRecord[] {
  return items.map((item, index) => ({
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

function getDeviceSortKey(device: DeviceRecord) {
  const locationNumber = device.location.match(/\d+/)?.[0]
  if (locationNumber) return Number(locationNumber)
  const hostNumber = device.hostName.match(/\d+/)?.[0]
  if (hostNumber) return Number(hostNumber)
  return Number.MAX_SAFE_INTEGER
}

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

function isLocalProbeDevice(device: DeviceRecord) {
  const text = `${device.id} ${device.hostName} ${device.location}`.toLowerCase()
  return text.includes('local') || text.includes('probe') || text.includes('unknown')
}

function buildSettings(endpoint: string): GatewaySettings {
  return {
    cloudEndpoint: endpoint,
    businessSystemUrl: DEFAULT_BUSINESS_SYSTEM_URL
  }
}

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

export const useMonitorStore = defineStore('monitor', () => {
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

  const onlineCount = computed(() => devices.value.filter(item => item.status === 'online').length)
  const warningCount = computed(() => devices.value.filter(item => item.status === 'warning').length)
  const offlineCount = computed(() => devices.value.filter(item => item.status === 'offline').length)
  const totalCount = computed(() => devices.value.length)
  const onlineRate = computed(() => {
    if (!devices.value.length) return '0%'
    return `${Math.round((onlineCount.value / devices.value.length) * 100)}%`
  })

  function persistLocalState() {
    writeLocalMonitorCache({
      gateway: gateway.value,
      devices: devices.value,
      localHost: localHost.value,
      logs: logs.value,
      statusMessage: statusMessage.value
    })
  }

  function mergeKnownDevices(nextDevices: DeviceRecord[]) {
    const knownMap = new Map(devices.value.map(device => [device.id, device]))
    const incomingIds = new Set(nextDevices.map(device => device.id))
    const merged: DeviceRecord[] = nextDevices.map((device, index) => {
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
      .filter(device => !incomingIds.has(device.id))
      .forEach(device => {
        merged.push(toOfflineDevice(device))
      })

    devices.value = merged.sort((left, right) => {
      const locationCompare = getDeviceSortKey(left) - getDeviceSortKey(right)
      if (locationCompare !== 0) return locationCompare
      return left.hostName.localeCompare(right.hostName, 'zh-CN')
    })
  }

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
      devices.value = cache.devices.filter(device => !isLocalProbeDevice(device))
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

  async function fetchGatewayStatus() {
    const response = await fetch(`${GATEWAY_BASE_URL}/api/status`)
    if (!response.ok) {
      throw new Error(`闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偞鐗犻、鏇㈡晜閽樺缃曟繝鐢靛Т閿曘倝鎮ч崱娆戠焼闁割偆鍠撶粻楣冩煕閳╁喚娈滈柣娑欑矒閺屻倝寮堕幐搴′淮闂佽鍠楅〃濠偽涢崘銊㈡婵炲棙鍔曢弸鎴犵磽娓氣偓缁犳牜绮婚弽顐や笉闁哄稁鍘奸拑鐔兼煟閺冨洢鈧偓闁稿鎸搁埥澶娾枎濡厧濮虹紓鍌欒兌婵敻骞愭ィ鍐ㄧ疅闁归棿绀佺粻銉︺亜閺傝法姣為柡瀣嚇濮婃椽宕崟顓犱紘闂佸摜濮甸悧鐘绘偘椤旂晫鐟归柍褜鍓熼悰顕€骞掑Δ鈧粻锝嗙節闂堟稒锛嶆繛鍏肩洴濮婃椽鎳￠妶鍛€剧紓渚囧枛鐎涒晠寮茬捄浣曟棃宕橀鍡欏幆闂備礁婀遍崕銈咁潖婵犳碍鍎楁繛鍡楁禋閻斿棝鎮规潪鎷岊劅闁绘搩鍓熼弻娑橆潩椤掑鍓堕梺鍝勬湰缁嬫垿鍩ユ径鎰闁绘劘灏欓崫妤佷繆閻愵亜鈧牠寮婚妸鈹у洭妫冨☉杈ㄧ稁?(${response.status})`)
    }
    return response.json() as Promise<GatewaySnapshot>
  }

  async function fetchGatewaySnapshotByInvoke() {
    return tryInvoke<GatewaySnapshot>('get_gateway_snapshot')
  }

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

  async function refreshLocalHostStatus(silent = true) {
    void silent
    const status = await fetchLocalHostStatus()
    applyLocalHostStatus(status)
  }

  function refreshLocalHostStatusInBackground() {
    void refreshLocalHostStatus(true).catch(() => {
      // Keep the device panel refresh stable even if the local probe is temporarily unavailable.
    })
  }

  async function refresh(options: RefreshOptions = {}) {
    if (refreshInFlight.value) {
      return
    }
    refreshInFlight.value = true
    if (!options.silent) {
      loading.value = true
    }
    try {
      await syncLanTeacherConfig().catch(() => {
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

  function startPolling(intervalMs = 5000) {
    hydrate()
    if (pollTimer.value) {
      window.clearInterval(pollTimer.value)
    }
    void refresh({ silent: true })
    pollTimer.value = window.setInterval(() => {
      void refresh({ silent: true })
    }, intervalMs)
  }

  function stopPolling() {
    if (pollTimer.value) {
      window.clearInterval(pollTimer.value)
      pollTimer.value = null
    }
  }

  async function startGateway() {
    loading.value = true
    try {
      const snapshot = await tryInvoke<GatewaySnapshot>('start_gateway')
      applyGatewaySnapshot(snapshot)
    } finally {
      loading.value = false
    }
  }

  async function stopGateway() {
    loading.value = true
    try {
      const snapshot = await tryInvoke<GatewaySnapshot>('stop_gateway')
      applyGatewaySnapshot(snapshot)
    } finally {
      loading.value = false
    }
  }

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

  function updateDeviceActionState(deviceId: string, actionState: string) {
    devices.value = devices.value.map(device =>
      device.id === deviceId
        ? {
            ...device,
            actionState
          }
        : device
    )
    persistLocalState()
  }

  function removeDevice(deviceId: string) {
    const target = devices.value.find(device => device.id === deviceId)
    devices.value = devices.value.filter(device => device.id !== deviceId)
    if (target) {
      logs.value = [`${formatDateTime(Date.now())} removed ${target.location} (${target.hostName})`, ...logs.value].slice(0, 60)
    }
    persistLocalState()
  }

  async function requestDeviceAction(deviceId: string, action: Exclude<DeviceAction, 'view' | 'remove'>) {
    const target = devices.value.find(device => device.id === deviceId)
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
