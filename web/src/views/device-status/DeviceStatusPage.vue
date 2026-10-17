<!--
  Web 管理平台的设备运行状态视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  Cpu,
  Monitor,
  Refresh,
  Search,
  WarningFilled,
} from '@element-plus/icons-vue'
import { labResourceService } from '../../api/services/basicResources'
import { listDeviceStatusEvents, listLabRuntimeStatus } from '../../api/services/deviceStatus'
import { bindActivationCodeToLab, listLabActivationBindings, unbindActivationCodeFromLab } from '../../api/services/deviceActivation'

defineOptions({
  name: 'DeviceStatusPage',
})

const FALLBACK_LABS = [
  {
    id: 1,
    code: 'A101',
    name: 'A101 云计算实验室',
    building: '信息楼 A 区',
    floor: '1F',
    room: 'A101',
    status: '开放',
    capacity: 72,
  },
  {
    id: 2,
    code: 'B203',
    name: 'B203 网络安全实验室',
    building: '实训楼 B 区',
    floor: '2F',
    room: 'B203',
    status: '开放',
    capacity: 56,
  },
  {
    id: 3,
    code: 'C305',
    name: 'C305 人工智能实验室',
    building: '创新楼 C 区',
    floor: '3F',
    room: 'C305',
    status: '维护中',
    capacity: 64,
  },
]

const loading = ref(false)
const bindingActivation = ref(false)
const unbindingActivation = ref(false)
const labs = ref([])
const devices = ref([])
const activationCodes = ref([])
const statusEvents = ref([])
const selectedLabId = ref('')
const labKeyword = ref('')
const deviceKeyword = ref('')
const labActivationCode = ref('')

/**
 * 获取实验室编码。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getLabCode = (lab = {}) => lab.code || lab.labCode || lab.room || lab.roomNo || ''
/**
 * 获取实验室名称。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getLabName = (lab = {}) => lab.name || lab.labName || getLabCode(lab)
/**
 * 获取设备编码。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getDeviceCode = (device = {}) => device.code || device.deviceCode || device.assetNo || ''
/**
 * 获取设备名称。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getDeviceName = (device = {}) => device.name || device.deviceName || getDeviceCode(device)
/**
 * 获取Bound设备键。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getBoundDeviceKey = (device = {}) => normalizeText(device.terminalId || device.macAddress || device.deviceCode || device.code || device.hostName)
/**
 * maskActivation编码的处理逻辑。
 * 入参：code（编码）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const maskActivationCode = (code = '') => {
  const value = String(code || '').trim()
  if (!value) return '-'
  if (value.length <= 10) return `${value.slice(0, 2)}****${value.slice(-2)}`
  return `${value.slice(0, 4)}****${value.slice(-4)}`
}

/**
 * 归一化实验室。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeLab = (lab = {}) => ({
  ...lab,
  id: lab.id,
  code: getLabCode(lab),
  name: getLabName(lab),
  building: lab.building || '',
  floor: lab.floor || '',
  room: lab.room || lab.roomNo || getLabCode(lab),
  status: lab.status || lab.openStatus || '开放',
  capacity: Number(lab.capacity) || 0,
})

const runtimeStatusMap = {
  normal: '正常',
  online: '正常',
  warning: '异常',
  abnormal: '异常',
  offline: '离线',
}

const healthStatusMap = {
  good: '良好',
  normal: '良好',
  warning: '关注',
  abnormal: '风险',
  unknown: '未知',
}

const onlineStatusMap = {
  online: true,
  offline: false,
  abnormal: true,
}

/**
 * 格式化Percent。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatPercent = (value) => {
  if (value === undefined || value === null || value === '') return ''
  const number = Number(value)
  return Number.isFinite(number) ? `${number}%` : String(value)
}

/**
 * 归一化设备。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeDevice = (device = {}) => ({
  ...device,
  id: device.id || device.terminalId || device.deviceId,
  code: getDeviceCode(device) || device.terminalId || device.macAddress || '-',
  name: device.hostName || getDeviceName(device) || device.terminalId || '未命名主机',
  category: '主机终端',
  labId: device.labId ?? '',
  labName: device.lab || device.labName || '',
  location: device.ipAddress || device.location || '',
  status: runtimeStatusMap[device.runtimeStatus] || device.runtimeStatus || (device.onlineStatus === 'offline' ? '离线' : '正常'),
  health: healthStatusMap[device.health] || device.health || '未知',
  online: onlineStatusMap[device.onlineStatus] ?? Boolean(device.online ?? device.onlineStatus !== 'offline'),
  updatedAt: device.lastReportTime || device.updatedAt || '',
  terminalId: device.terminalId || '',
  macAddress: device.macAddress || '',
  hostName: device.hostName || '',
  ipAddress: device.ipAddress || '',
  cpuUsage: formatPercent(device.cpuUsage),
  memoryUsage: formatPercent(device.memoryUsage),
  diskUsage: formatPercent(device.diskUsage),
  loginUser: device.loginUser || '',
  source: device.sourceType || 'terminal',
  sourceType: device.sourceType || 'terminal',
  activatedAt: device.activatedAt || device.createdAt || '',
})

/**
 * 归一化文本。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeText = (value) => String(value || '').trim().toLowerCase()

/**
 * 获取状态类型。
 * 入参：status（状态）。
 * @returns 文本结果。
 */
const getStatusType = (status) => {
  if (['正常', '开放', '在线', '良好'].includes(status)) return 'success'
  if (['关注', '维护中', '维修中'].includes(status)) return 'warning'
  if (['故障', '离线'].includes(status)) return 'danger'
  if (['停用'].includes(status)) return 'info'
  return 'primary'
}

const selectedLab = computed(/** 计算派生状态。 */ () => labs.value.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => String(lab.id) === String(selectedLabId.value)) || labs.value[0] || null)

const visibleLabs = computed(/** 计算派生状态。 */ () => {
  const keyword = normalizeText(labKeyword.value)
  if (!keyword) return labs.value
  return labs.value.filter(/** 判断当前条目是否符合筛选条件。 入参：lab。 */ (lab) =>
    [lab.name, lab.code, lab.building, lab.room, lab.status].some(/** 判断当前条目是否满足任一匹配条件。 入参：value。 */ (value) => normalizeText(value).includes(keyword)),
  )
})

const currentLabDevices = computed(/** 计算派生状态。 */ () => {
  if (!selectedLab.value) return []
  return devices.value.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ (device) => String(device.labId || '') === String(selectedLab.value.id))
})

/**
 * 筛选运行时行集合ByKeyword。
 * 入参：rows（行集合）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const filterRuntimeRowsByKeyword = (rows) => {
  const keyword = normalizeText(deviceKeyword.value)
  if (!keyword) return rows
  return rows.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ (device) =>
    [
      device.code,
      device.name,
      device.hostName,
      device.ipAddress,
      device.macAddress,
      device.terminalId,
      device.status,
      device.health,
      device.source,
    ].some(/** 判断当前条目是否满足任一匹配条件。 入参：value。 */ (value) => normalizeText(value).includes(keyword)),
  )
}

const selectedLabDevices = computed(/** 计算派生状态。 */ () => {
  return filterRuntimeRowsByKeyword(currentLabDevices.value)
})

const selectedLabActivation = computed(/** 计算派生状态。 */ () =>
  activationCodes.value.find(/** 判断当前条目是否为查找目标。 入参：record。 */ (record) => String(record.labId || '') === String(selectedLab.value?.id || '') && record.status === 'active'),
)

const selectedLabActivationQuota = computed(/** 计算派生状态。 */ () => ({
  bound: Number(selectedLabActivation.value?.boundCount || 0),
  limit: Number(selectedLabActivation.value?.deviceLimit || 0),
  remaining: Math.max(Number(selectedLabActivation.value?.deviceLimit || 0) - Number(selectedLabActivation.value?.boundCount || 0), 0),
}))

const activationBoundDevices = computed(/** 计算派生状态。 */ () => {
  const rows = selectedLabActivation.value?.boundDevices
  return Array.isArray(rows) ? rows : []
})

const activationDeviceStatusRows = computed(/** 计算派生状态。 */ () => {
  if (!selectedLabActivation.value) return []
  if (!activationBoundDevices.value.length) return filterRuntimeRowsByKeyword(currentLabDevices.value)

  const labDeviceByKey = new Map()
  currentLabDevices.value.forEach(/** 处理遍历到的当前条目。 入参：device。 */ (device) => {
    ;[device.code, device.deviceCode, device.macAddress, device.hostName, device.terminalId]
      .map(normalizeText)
      .filter(Boolean)
      .forEach(/** 处理遍历到的当前条目。 入参：key。 */ (key) => labDeviceByKey.set(key, device))
  })

  const keyword = normalizeText(deviceKeyword.value)
  return activationBoundDevices.value
    .map(/** 将当前条目转换为目标结果。 入参：boundDevice、index。 */ (boundDevice, index) => {
      const matchedDevice = labDeviceByKey.get(getBoundDeviceKey(boundDevice))
      return {
        id: boundDevice.macAddress || boundDevice.deviceCode || boundDevice.hostName || `activation-device-${index}`,
        name: matchedDevice?.name || boundDevice.hostName || boundDevice.deviceName || '已接入主机',
        code: matchedDevice?.code || boundDevice.deviceCode || boundDevice.macAddress || '-',
        macAddress: boundDevice.macAddress || '-',
        activatedAt: boundDevice.activatedAt || '-',
        terminalId: matchedDevice?.terminalId || boundDevice.terminalId || '',
        hostName: matchedDevice?.hostName || boundDevice.hostName || '',
        ipAddress: matchedDevice?.ipAddress || '',
        status: matchedDevice?.status || '待上报',
        health: matchedDevice?.health || '未知',
        online: matchedDevice?.online ?? false,
        location: matchedDevice?.location || matchedDevice?.ipAddress || '当前实验室已接入主机',
        source: matchedDevice ? matchedDevice.source : '接入授权记录',
        updatedAt: matchedDevice?.updatedAt || '',
        cpuUsage: matchedDevice?.cpuUsage || '',
        memoryUsage: matchedDevice?.memoryUsage || '',
        diskUsage: matchedDevice?.diskUsage || '',
        loginUser: matchedDevice?.loginUser || '',
      }
    })
    .filter(
      /**
       * 判断当前条目是否符合筛选条件。
       * 入参：row。
       */
      (row) =>
        !keyword ||
        [row.name, row.code, row.macAddress, row.location, row.status, row.health, row.source].some(/** 判断当前条目是否满足任一匹配条件。 入参：value。 */ (value) =>
          normalizeText(value).includes(keyword),
        ),
    )
})

const displayedDeviceStatusRows = computed(/** 计算派生状态。 */ () =>
  selectedLabActivation.value ? activationDeviceStatusRows.value : selectedLabDevices.value,
)

const deviceStats = computed(/** 计算派生状态。 */ () => {
  const rows = displayedDeviceStatusRows.value
  const online = rows.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ (device) => device.online).length
  const offline = rows.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ (device) => !device.online).length
  const fault = rows.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ (device) => !['离线'].includes(device.status) && (['故障', '异常'].includes(device.status) || device.health === '风险')).length

  return [
    { label: '设备总数', value: rows.length, trend: selectedLab.value?.name || '未选择实验室', icon: Monitor, type: 'primary' },
    { label: '在线设备', value: online, trend: `${rows.length ? Math.round((online / rows.length) * 100) : 0}% 在线`, icon: CircleCheck, type: 'success' },
    { label: '离线设备', value: offline, trend: offline ? '等待终端心跳' : '全部在线', icon: Cpu, type: offline ? 'danger' : 'success' },
    { label: '异常设备', value: fault, trend: fault ? '需要处理' : '状态正常', icon: WarningFilled, type: fault ? 'danger' : 'success' },
  ]
})

/**
 * 归一化运行时列表。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeRuntimeList = (payload = {}) => {
  if (Array.isArray(payload?.devices)) return payload.devices
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.data?.devices)) return payload.data.devices
  if (Array.isArray(payload?.data?.records)) return payload.data.records
  if (Array.isArray(payload)) return payload
  return []
}

/**
 * 归一化事件列表。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeEventList = (payload = {}) => {
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.data?.records)) return payload.data.records
  if (Array.isArray(payload)) return payload
  return []
}

/**
 * 加载运行时状态。
 * 入参：labId（实验室标识）。
 */
const loadRuntimeStatus = async (labId = selectedLabId.value) => {
  if (!labId) {
    devices.value = []
    statusEvents.value = []
    return
  }

  const [runtimePayload, eventPayload] = await Promise.all([
    listLabRuntimeStatus(labId),
    listDeviceStatusEvents({ labId, limit: 50 }).catch(/** 处理异步失败结果。 */ () => ({ records: [] })),
  ])

  devices.value = normalizeRuntimeList(runtimePayload).map(normalizeDevice)
  statusEvents.value = normalizeEventList(eventPayload)
}

/**
 * 加载数据。
 */
const loadData = async () => {
  loading.value = true
  try {
    const [loadedLabs, loadedActivationCodes] = await Promise.all([
      labResourceService.list({ fallbackRecords: FALLBACK_LABS }),
      listLabActivationBindings().catch(/** 处理异步失败结果。 */ () => []),
    ])

    labs.value = loadedLabs.map(normalizeLab)
    activationCodes.value = Array.isArray(loadedActivationCodes) ? loadedActivationCodes : []
    selectedLabId.value = selectedLabId.value || labs.value[0]?.id || ''
    await loadRuntimeStatus(selectedLabId.value)
  } catch (error) {
    ElMessage.warning(error?.message || '设备运行状态数据读取失败')
    labs.value = FALLBACK_LABS.map(normalizeLab)
    devices.value = []
    statusEvents.value = []
    selectedLabId.value = labs.value[0]?.id || ''
  } finally {
    loading.value = false
  }
}

/**
 * 选择实验室。
 * 入参：lab（实验室）。
 */
const selectLab = (lab) => {
  selectedLabId.value = lab.id
  deviceKeyword.value = ''
  loadRuntimeStatus(lab.id).catch(/** 处理异步失败结果。 入参：error。 */ (error) => {
    devices.value = []
    statusEvents.value = []
    ElMessage.error(error?.message || '当前实验室设备运行状态读取失败')
  })
}

/**
 * bindActivation编码的处理逻辑。
 */
const bindActivationCode = async () => {
  const lab = selectedLab.value
  const code = labActivationCode.value.trim()

  if (!lab) {
    ElMessage.warning('请先选择实验室')
    return
  }
  if (!code) {
    ElMessage.warning('请输入实验室接入码')
    return
  }
  if (selectedLabActivation.value) {
    ElMessage.warning('当前实验室接入授权已启用，请先解除后再重新启用')
    return
  }

  bindingActivation.value = true
  try {
    const record = await bindActivationCodeToLab({
      code,
      labId: lab.id,
      labCode: lab.code,
      labName: lab.name,
    })
    labActivationCode.value = ''
    activationCodes.value = await listLabActivationBindings().catch(/** 处理异步失败结果。 */ () => [record])
    await loadRuntimeStatus(lab.id)
    ElMessage.success('实验室接入授权已启用')
  } catch (error) {
    ElMessage.error(error?.message || '实验室接入授权启用失败')
  } finally {
    bindingActivation.value = false
  }
}

/**
 * unbindActivation编码的处理逻辑。
 */
const unbindActivationCode = async () => {
  const activation = selectedLabActivation.value
  if (!activation?.code) return

  try {
    await ElMessageBox.confirm('解除后该实验室可重新启用新的接入码，已接入终端记录仍会保留。', '解除实验室接入授权', {
      confirmButtonText: '解除授权',
      cancelButtonText: '返回',
      type: 'warning',
    })
    unbindingActivation.value = true
    await unbindActivationCodeFromLab(activation.code)
    activationCodes.value = await listLabActivationBindings()
    labActivationCode.value = ''
    await loadRuntimeStatus(selectedLabId.value)
    ElMessage.success('已解除当前实验室接入授权')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.message || '实验室接入授权解除失败')
  } finally {
    unbindingActivation.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="device-status-page">
    <el-card class="status-header" shadow="never">
      <div class="header-main">
        <span class="header-icon">
          <el-icon>
            <Monitor />
          </el-icon>
        </span>
        <div>
          <h1>设备状态</h1>
        </div>
      </div>
      <el-button :icon="Refresh" :loading="loading" type="primary" plain @click="loadData">刷新</el-button>
    </el-card>

    <div class="status-workbench" v-loading="loading">
      <el-card class="lab-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <div>
              <h2>实验室</h2>
              <span>{{ visibleLabs.length }} / {{ labs.length }}</span>
            </div>
          </div>
        </template>

        <el-input v-model="labKeyword" clearable placeholder="搜索实验室名称、编号、楼层">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>

        <el-scrollbar class="lab-list-scroll">
          <div class="lab-list">
            <button
              v-for="lab in visibleLabs"
              :key="lab.id"
              class="lab-item"
              :class="{ active: String(lab.id) === String(selectedLabId) }"
              type="button"
              @click="selectLab(lab)"
            >
              <span class="lab-title">
                <strong>{{ lab.name }}</strong>
                <el-tag :type="getStatusType(lab.status)" effect="light">{{ lab.status }}</el-tag>
              </span>
              <span>{{ lab.building }} {{ lab.floor }} {{ lab.room }}</span>
              <small>
                {{ devices.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ (device) => String(device.labId) === String(lab.id)).length }} 台设备
                <template v-if="activationCodes.find(/** 判断当前条目是否为查找目标。 入参：record。 */ (record) => String(record.labId || '') === String(lab.id) && record.status === 'active')">
                  · 接入授权已启用
                </template>
              </small>
            </button>
            <el-empty v-if="!visibleLabs.length" description="暂无匹配实验室" />
          </div>
        </el-scrollbar>
      </el-card>

      <main class="status-main">
        <el-card class="activation-card" shadow="never">
          <div class="activation-content">
            <div>
              <h2>{{ selectedLab?.name || '请选择实验室' }}</h2>
              <span v-if="selectedLab">{{ selectedLab.code }} · {{ selectedLab.building }} {{ selectedLab.room }}</span>
            </div>
            <div class="activation-state" :class="{ active: selectedLabActivation }">
              <strong>{{ selectedLabActivation ? '接入授权已启用' : '未启用实验室接入授权' }}</strong>
              <span v-if="selectedLabActivation">
                {{ maskActivationCode(selectedLabActivation.code) }} · 终端接入额度 {{ selectedLabActivationQuota.bound }}/{{ selectedLabActivationQuota.limit }}
              </span>
              <span v-else>启用后教师端/实验室客户端可首次接入当前实验室</span>
            </div>
          </div>

          <div v-if="!selectedLabActivation" class="activation-form">
            <el-input v-model="labActivationCode" clearable placeholder="输入实验室接入码，例如 4150652b-00247744-00024173" />
            <el-button :loading="bindingActivation" type="primary" @click="bindActivationCode">启用当前实验室接入授权</el-button>
          </div>
          <div v-else class="activation-actions">
            <span>设备接入授权用于教师端/实验室客户端首次接入，后续状态上报使用终端凭证。</span>
            <el-button :loading="unbindingActivation" type="danger" plain @click="unbindActivationCode">解除实验室接入授权</el-button>
          </div>
        </el-card>

        <div class="metric-grid">
          <div v-for="item in deviceStats" :key="item.label" class="metric-item" :class="item.type">
            <span class="metric-icon">
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
            </span>
            <div>
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
              <small>{{ item.trend }}</small>
            </div>
          </div>
        </div>

        <el-card class="device-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <div>
                <h2>设备状态明细</h2>
                <span>
                  {{ selectedLabActivation ? '当前实验室已接入主机' : '当前实验室主机' }} {{ displayedDeviceStatusRows.length }} 台
                </span>
              </div>
              <el-input v-model="deviceKeyword" class="device-search" clearable placeholder="搜索主机名、IP、MAC、终端">
                <template #prefix>
                  <el-icon>
                    <Search />
                  </el-icon>
                </template>
              </el-input>
            </div>
          </template>

          <el-table
            :data="displayedDeviceStatusRows"
            border
            stripe
            :empty-text="selectedLabActivation ? '暂无主机上报状态' : '暂无教师端上报数据，请先完成设备接入授权并启动教师端客户端。'"
          >
            <el-table-column label="主机名" min-width="180">
              <template #default="{ row }">
                <div class="device-cell">
                  <span class="device-type other">
                    <el-icon>
                      <Monitor />
                    </el-icon>
                  </span>
                  <div>
                    <strong>{{ row.hostName || row.name || '未命名主机' }}</strong>
                    <span>{{ row.code || row.terminalId || '-' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="IP 地址" min-width="140" prop="ipAddress" show-overflow-tooltip />
            <el-table-column label="MAC / 终端" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">{{ row.macAddress || row.terminalId || '-' }}</template>
            </el-table-column>
            <el-table-column label="在线状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.online ? 'success' : 'danger'" effect="light">
                  {{ row.online ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="运行状态" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" effect="plain">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="健康度" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.health)" effect="plain">{{ row.health }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="CPU 使用率" width="110" prop="cpuUsage" />
            <el-table-column label="内存使用率" width="110" prop="memoryUsage" />
            <el-table-column label="磁盘使用率" width="110" prop="diskUsage" />
            <el-table-column label="登录用户" min-width="120" prop="loginUser" show-overflow-tooltip />
            <el-table-column label="来源" width="120">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.source }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="激活时间" min-width="150" prop="activatedAt" show-overflow-tooltip />
            <el-table-column label="最近上报时间" min-width="150" prop="updatedAt" show-overflow-tooltip />
          </el-table>
        </el-card>
      </main>
    </div>
  </section>
</template>

<style scoped>
.device-status-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.status-header,
.lab-panel,
.activation-card,
.device-card {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.status-header :deep(.el-card__body),
.header-main,
.panel-header,
.activation-content,
.activation-form,
.lab-title,
.device-cell {
  display: flex;
  align-items: center;
}

.status-header :deep(.el-card__body) {
  justify-content: space-between;
  gap: 20px;
  min-height: 104px;
  padding: 22px 24px;
  background:
    linear-gradient(135deg, #ffffff 0%, #f8fbff 56%, #eef6ff 100%);
}

.header-main {
  min-width: 0;
  gap: 14px;
}

.header-icon,
.metric-icon,
.device-type {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.header-icon {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eaf2ff;
  font-size: 26px;
}

.header-main h1,
.panel-header h2,
.activation-content h2 {
  margin: 0;
  color: var(--text-primary);
  font-weight: 700;
  letter-spacing: 0;
}

.header-main h1 {
  font-size: 25px;
  line-height: 1.28;
}

.header-main p,
.panel-header span,
.activation-content span,
.activation-state span,
.lab-item span,
.lab-item small,
.device-cell span,
.metric-item small {
  color: #64748b;
  font-size: 13px;
}

.header-main p {
  max-width: 78ch;
  margin: 8px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.65;
}

.status-workbench {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.lab-panel {
  position: sticky;
  top: 14px;
}

.lab-panel :deep(.el-card__header),
.device-card :deep(.el-card__header) {
  min-height: 62px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.lab-panel :deep(.el-card__body) {
  padding: 14px;
}

.lab-panel :deep(.el-input__wrapper),
.device-card :deep(.el-input__wrapper),
.activation-card :deep(.el-input__wrapper) {
  min-height: 38px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

.lab-panel :deep(.el-input__wrapper:hover),
.device-card :deep(.el-input__wrapper:hover),
.activation-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.panel-header {
  justify-content: space-between;
  gap: 12px;
}

.panel-header h2,
.activation-content h2 {
  font-size: 16px;
  line-height: 1.35;
}

.lab-list-scroll {
  height: calc(100vh - 284px);
  min-height: 420px;
  margin-top: 14px;
}

.lab-list {
  display: grid;
  gap: 8px;
}

.lab-item {
  width: 100%;
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition:
    background 0.18s ease,
    border-color 0.18s ease;
}

.lab-item:hover {
  border-color: #93c5fd;
  background: #f8fbff;
}

.lab-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.lab-title {
  justify-content: space-between;
  gap: 10px;
}

.lab-title strong,
.device-cell strong {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.35;
}

.status-main {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.activation-card :deep(.el-card__body) {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.activation-content {
  justify-content: space-between;
  gap: 16px;
}

.activation-state {
  display: grid;
  gap: 4px;
  min-width: 220px;
  padding: 11px 12px;
  border-radius: 8px;
  border: 1px solid #fed7aa;
  background: #fff7ed;
}

.activation-state.active {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.activation-state strong {
  color: var(--text-primary);
  font-size: 14px;
}

.activation-form {
  gap: 10px;
  align-items: stretch;
}

.activation-form .el-input {
  max-width: 520px;
}

.activation-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fafc;
}

.activation-actions span {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-item {
  display: flex;
  gap: 12px;
  min-width: 0;
  min-height: 108px;
  padding: 14px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.metric-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  color: #2f7cf6;
  background: #eef4ff;
  font-size: 20px;
  line-height: 1;
}

.metric-icon :deep(.el-icon) {
  font-size: 20px;
}

.metric-item.success .metric-icon {
  color: #16a34a;
  background: #dcfce7;
}

.metric-item.danger .metric-icon {
  color: #dc2626;
  background: #fee2e2;
}

.metric-item strong {
  display: block;
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.1;
}

.metric-item > div > span {
  display: block;
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.metric-item small {
  display: block;
  margin-top: 4px;
}

.device-search {
  width: 300px;
}

.device-card :deep(.el-card__body) {
  padding: 0;
}

.device-card :deep(.el-table) {
  --el-table-border-color: #e5edf6;
  --el-table-header-bg-color: #f8fbff;
  --el-table-row-hover-bg-color: #eff6ff;
}

.device-card :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.device-card :deep(.el-table th.el-table__cell) {
  height: 46px;
  background: #f8fbff !important;
  color: #475569;
  font-weight: 700;
}

.device-card :deep(.el-table__body td) {
  height: 58px;
}

.device-cell {
  gap: 10px;
}

.device-cell > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.device-type {
  width: 34px;
  height: 34px;
  border-radius: 8px;
}

.device-type.access {
  color: #0f766e;
  background: #ccfbf1;
}

.device-type.camera {
  color: #2563eb;
  background: #dbeafe;
}

.device-type.light {
  color: #ca8a04;
  background: #fef3c7;
}

.device-type.other {
  color: #64748b;
  background: #f1f5f9;
}

@media (max-width: 1180px) {
  .status-workbench {
    grid-template-columns: 1fr;
  }

  .lab-panel {
    position: static;
  }

  .lab-list-scroll {
    height: auto;
    max-height: 360px;
    min-height: 0;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .status-header :deep(.el-card__body),
  .activation-content,
  .activation-form,
  .activation-actions,
  .panel-header {
    align-items: stretch;
    flex-direction: column;
  }

  .activation-form .el-input,
  .device-search {
    width: 100%;
    max-width: none;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .status-header :deep(.el-card__body) {
    padding: 18px;
  }

  .header-main {
    align-items: flex-start;
  }

  .header-main h1 {
    font-size: 21px;
  }

  .status-header :deep(.el-button) {
    width: 100%;
  }
}
</style>
