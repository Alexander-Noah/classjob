<script setup>
import { computed, onMounted, ref } from 'vue'
import { User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import campusBuildingImage from '../../../assets/digital-twin/campus-building.png'
import {
  labResourceService,
  repairResourceService,
  reservationResourceService,
} from '../../../api/services/basicResources'
import { listClassTimetables } from '../../../api/services/classTimetable'
import { deviceAssetService } from '../../../api/services/deviceAssets'

defineOptions({
  name: 'RolePortalPage',
})

const props = defineProps({
  config: {
    type: Object,
    required: true,
  },
})

const twinLoading = ref(false)
const liveLabs = ref([])
const liveDevices = ref([])
const liveReservations = ref([])
const liveRepairs = ref([])
const liveTimetables = ref([])
const activeModule = ref('系统总览')
const selectedLabCode = ref('')
const selectedHotspot = ref('2F 实训区')
const operationLog = ref([])

const getTodayText = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const normalizeText = (value) =>
  String(value || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')

const hasLabToken = (text, code) => {
  const normalizedCode = String(code || '').trim()
  if (!normalizedCode) return false
  return new RegExp(`(^|[^0-9A-Za-z])${normalizedCode.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}([^0-9A-Za-z]|$)`).test(
    String(text || ''),
  )
}

const getLabCode = (lab = {}) => lab.code || lab.labCode || lab.room || lab.roomNo || ''
const getLabName = (lab = {}) => lab.name || lab.labName || lab.roomName || getLabCode(lab)
const getLabStatus = (lab = {}) => lab.status || lab.openStatus || '开放'
const getLabFloor = (lab = {}) => lab.floor || ''
const getLabCapacity = (lab = {}) => Number(lab.capacity) || 0
const getDeviceLabText = (device = {}) => device.labName || device.lab || device.roomName || device.location || ''
const getReservationLabText = (reservation = {}) =>
  reservation.lab || reservation.labName || reservation.labDisplayName || reservation.roomName || reservation.classroom || ''
const getReservationDate = (reservation = {}) =>
  reservation.date || reservation.reservationDate || reservation.useDate || reservation.startDate || ''
const getRepairStatus = (repair = {}) => repair.status || repair.repairStatus || ''
const getLabId = (lab = {}) => lab.id || lab.labId || getLabCode(lab)

const isLabMatched = (text, lab) => {
  const normalizedSource = normalizeText(text)
  if (!normalizedSource) return false
  const name = normalizeText(getLabName(lab))
  if (name && (normalizedSource.includes(name) || name.includes(normalizedSource))) return true
  return hasLabToken(text, getLabCode(lab))
}

const isDeviceOnline = (device = {}) => {
  const status = String(device.status || device.onlineStatus || device.assetStatus || '').toLowerCase()
  if (status.includes('离线') || status.includes('停用') || status.includes('报废') || status.includes('offline')) return false
  return true
}

const isReservationApproved = (reservation = {}) =>
  ['已通过', 'approved', 'APPROVED', '通过'].includes(reservation.status || reservation.reviewStatus)

const getStatusColor = (status) => {
  if (status === '开放') return '#22c55e'
  if (status === '使用中' || status === '预约中') return '#f59e0b'
  if (status === '维护中' || status === '停用') return '#ef4444'
  return '#3b82f6'
}

const getUsageByStatus = (status, capacity, deviceCount) => {
  if (status === '使用中') return 88
  if (status === '预约中') return 64
  if (status === '维护中' || status === '停用') return 12
  if (capacity > 0 && deviceCount > 0) return Math.min(58, Math.max(18, Math.round((deviceCount / capacity) * 45)))
  return 30
}

const buildLabNodes = () => {
  const sourceLabs = liveLabs.value.length ? liveLabs.value : props.config.digitalTwin?.labs || []
  return sourceLabs.slice(0, 6).map((lab) => {
    const code = getLabCode(lab)
    const status = getLabStatus(lab)
    const matchedDevices = liveDevices.value.filter((device) => isLabMatched(getDeviceLabText(device), lab))
    const deviceCount = matchedDevices.length || Number(lab.devices ?? lab.deviceCount) || 0
    const capacity = getLabCapacity(lab)

    return {
      id: getLabId(lab),
      raw: lab,
      code,
      name: getLabName(lab),
      floor: getLabFloor(lab),
      status,
      usage: getUsageByStatus(status, capacity, deviceCount),
      color: getStatusColor(status),
    }
  })
}

const liveLabNodes = computed(buildLabNodes)

const selectedLab = computed(() => {
  if (!liveLabNodes.value.length) return null
  return liveLabNodes.value.find((lab) => lab.code === selectedLabCode.value) || liveLabNodes.value[0]
})

const selectedLabDevices = computed(() =>
  selectedLab.value ? liveDevices.value.filter((device) => isLabMatched(getDeviceLabText(device), selectedLab.value)) : [],
)

const selectedLabReservations = computed(() =>
  selectedLab.value
    ? liveReservations.value.filter((reservation) => isLabMatched(getReservationLabText(reservation), selectedLab.value))
    : [],
)

const selectedLabRepairs = computed(() =>
  selectedLab.value
    ? liveRepairs.value.filter((repair) =>
        isLabMatched(repair.labName || repair.lab || repair.roomName || repair.location, selectedLab.value),
      )
    : [],
)

const selectedLabPanel = computed(() => {
  const lab = selectedLab.value
  if (!lab) {
    return {
      title: selectedHotspot.value,
      meta: activeModule.value,
      rows: [],
    }
  }

  return {
    title: `${lab.code} ${lab.name}`,
    meta: `${lab.floor || '楼层未录入'} · ${lab.status} · ${activeModule.value}`,
    rows: [
      { label: '设备数量', value: `${selectedLabDevices.value.length || 0} 台` },
      { label: '在线设备', value: `${selectedLabDevices.value.filter(isDeviceOnline).length || 0} 台` },
      { label: '关联预约', value: `${selectedLabReservations.value.length || 0} 条` },
      { label: '报修工单', value: `${selectedLabRepairs.value.length || 0} 条` },
    ],
  }
})

const selectLab = (lab) => {
  selectedLabCode.value = lab.code
  selectedHotspot.value = `${lab.code} 空间`
  operationLog.value.unshift(`${lab.code} 空间已聚焦`)
  operationLog.value = operationLog.value.slice(0, 4)
}

const selectModule = (moduleName) => {
  activeModule.value = moduleName
  operationLog.value.unshift(`切换至 ${moduleName}`)
  operationLog.value = operationLog.value.slice(0, 4)
}

const selectHotspot = (hotspot) => {
  selectedHotspot.value = hotspot
  operationLog.value.unshift(`定位到 ${hotspot}`)
  operationLog.value = operationLog.value.slice(0, 4)
}

const updateSelectedLabStatus = async (status) => {
  const lab = selectedLab.value
  if (!lab?.id) return

  const previousLabs = [...liveLabs.value]
  const nextRawLab = {
    ...lab.raw,
    id: lab.id,
    status,
    openStatus: status,
  }

  liveLabs.value = liveLabs.value.map((item) => (String(getLabId(item)) === String(lab.id) ? nextRawLab : item))

  try {
    await labResourceService.update(lab.id, nextRawLab)
    operationLog.value.unshift(`${lab.code} 已切换为${status}`)
    operationLog.value = operationLog.value.slice(0, 4)
    ElMessage.success(`${lab.code} 已切换为${status}`)
  } catch (error) {
    liveLabs.value = previousLabs
    ElMessage.error(error?.message || '状态更新失败')
  }
}

const liveSummary = computed(() => {
  if (!liveLabs.value.length && !liveDevices.value.length) return props.config.digitalTwin.summary

  const today = getTodayText()
  const labCount = liveLabs.value.length
  const totalCapacity = liveLabs.value.reduce((sum, lab) => sum + getLabCapacity(lab), 0)
  const onlineCount = liveDevices.value.filter(isDeviceOnline).length
  const deviceCount = liveDevices.value.length
  const todayReservations = liveReservations.value.filter(
    (reservation) => isReservationApproved(reservation) && String(getReservationDate(reservation)) === today,
  )
  const todayCourses = liveTimetables.value.length

  return [
    { label: '纳管空间', value: String(labCount || 0), unit: '间', trend: `总容量 ${totalCapacity || 0} 人` },
    { label: '资产设备', value: String(deviceCount || 0), unit: '台', trend: `可用资产 ${onlineCount || 0} 台` },
    { label: '今日课表', value: String(todayCourses || 0), unit: '节', trend: '课表实时同步' },
    { label: '今日预约', value: String(todayReservations.length || 0), unit: '条', trend: '审批通过预约' },
  ]
})

const liveEvents = computed(() => {
  const events = []
  const abnormalRepairs = liveRepairs.value
    .filter((repair) => !['已完成', '已关闭', '完成'].includes(getRepairStatus(repair)))
    .slice(0, 2)

  abnormalRepairs.forEach((repair) => {
    events.push({
      level: '关注',
      title: repair.title || repair.deviceName || repair.device || '待处理报修',
      desc: `${repair.labName || repair.lab || repair.roomName || '实验室'} · ${getRepairStatus(repair) || '处理中'}`,
    })
  })

  const usingLab = liveLabNodes.value.find((lab) => lab.status === '使用中')
  if (usingLab) {
    events.push({
      level: '关注',
      title: `${usingLab.code} 正在使用`,
      desc: `${usingLab.name} 当前占用率 ${usingLab.usage}%，请关注设备与环境状态。`,
    })
  }

  if (liveDevices.value.length) {
    const onlineCount = liveDevices.value.filter(isDeviceOnline).length
    events.push({
      level: '正常',
      title: '设备在线数据已同步',
      desc: `${onlineCount} 台设备在线，来自设备资产台账。`,
    })
  }

  return events.length ? events.slice(0, 4) : props.config.digitalTwin.events
})

const loadTwinData = async () => {
  if (!props.config.digitalTwinOnly) return

  twinLoading.value = true
  try {
    const [labs, devices, reservations, repairs, timetables] = await Promise.all([
      labResourceService.list({ fallbackRecords: [] }),
      deviceAssetService.list({ fallbackRecords: [] }),
      reservationResourceService.list({ fallbackRecords: [] }),
      repairResourceService.list({ fallbackRecords: [] }),
      listClassTimetables({ limit: 5000 }).catch(() => []),
    ])

    liveLabs.value = Array.isArray(labs) ? labs : []
    liveDevices.value = Array.isArray(devices) ? devices : []
    liveReservations.value = Array.isArray(reservations) ? reservations : []
    liveRepairs.value = Array.isArray(repairs) ? repairs : []
    liveTimetables.value = Array.isArray(timetables) ? timetables : []
    selectedLabCode.value = selectedLabCode.value || buildLabNodes()[0]?.code || ''
  } finally {
    twinLoading.value = false
  }
}

onMounted(loadTwinData)
</script>

<template>
  <section v-if="config.digitalTwinOnly && config.digitalTwin" class="twin-page twin-command">
      <header class="command-header">
        <span class="header-wing"></span>
        <div>
          <small>{{ config.digitalTwin.kicker }}</small>
          <h1>{{ config.digitalTwin.title }}</h1>
        </div>
        <span class="header-wing"></span>
        <button class="live-pill" type="button" :disabled="twinLoading" @click="loadTwinData">
          <i></i>{{ twinLoading ? '同步中' : '实时联动' }}
        </button>
      </header>

      <div class="command-body">
        <aside class="sensor-column">
          <article v-for="item in liveSummary" :key="item.label" class="sensor-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
            <em>{{ item.trend }}</em>
          </article>
        </aside>

        <main class="scene-panel">
          <div class="scene-toolbar">
            <button
              v-for="tag in config.digitalTwin.sceneTags"
              :key="tag"
              :class="{ active: activeModule === tag }"
              type="button"
              @click="selectModule(tag)"
            >
              {{ tag }}
            </button>
          </div>
          <div class="campus-scene">
            <img class="campus-building-image" :src="campusBuildingImage" alt="实验楼数字孪生场景" />
            <div class="scene-skyline"></div>
            <div class="hud-line line-a"></div>
            <div class="hud-line line-b"></div>
            <div class="hud-line line-c"></div>
            <div class="building-model photo-hotspots">
              <button
                class="pin pin-a"
                :class="{ active: selectedHotspot === '2F 实训区' }"
                type="button"
                @click="selectHotspot('2F 实训区')"
              >
                2F 实训区
              </button>
              <button
                class="pin pin-b"
                :class="{ active: selectedHotspot === '设备机房' }"
                type="button"
                @click="selectHotspot('设备机房')"
              >
                设备机房
              </button>
              <button
                class="pin pin-c"
                :class="{ active: selectedHotspot === '预约占用' }"
                type="button"
                @click="selectHotspot('预约占用')"
              >
                预约占用
              </button>
            </div>
            <div class="energy-ring ring-a"></div>
            <div class="energy-ring ring-b"></div>
          </div>
          <div class="lab-grid">
            <article
              v-for="lab in liveLabNodes"
              :key="lab.code"
              class="lab-node"
              :class="{ active: selectedLab?.code === lab.code }"
              :style="{ '--lab-color': lab.color, '--lab-usage': `${lab.usage}%` }"
              role="button"
              tabindex="0"
              @click="selectLab(lab)"
              @keyup.enter="selectLab(lab)"
            >
              <span class="node-dot"></span>
              <strong>{{ lab.code }}</strong>
              <small>{{ lab.floor }} · {{ lab.name }}</small>
              <div>
                <em>{{ lab.status }}</em>
                <b>{{ lab.usage }}%</b>
              </div>
            </article>
          </div>
        </main>

        <aside class="event-column">
          <article class="control-card">
            <h2>空间控制</h2>
            <strong>{{ selectedLabPanel.title }}</strong>
            <p>{{ selectedLabPanel.meta }}</p>
            <div class="control-metrics">
              <span v-for="row in selectedLabPanel.rows" :key="row.label">
                <em>{{ row.label }}</em>
                <b>{{ row.value }}</b>
              </span>
            </div>
            <div class="control-actions">
              <button type="button" @click="updateSelectedLabStatus('开放')">开放</button>
              <button type="button" @click="updateSelectedLabStatus('使用中')">占用</button>
              <button type="button" @click="updateSelectedLabStatus('维护中')">维护</button>
            </div>
          </article>
          <article class="event-card">
            <h2>运行事件</h2>
            <div v-for="event in liveEvents" :key="event.title" class="event-item">
              <el-tag :type="event.level === '正常' ? 'success' : 'warning'" effect="dark" size="small">
                {{ event.level }}
              </el-tag>
              <div>
                <strong>{{ event.title }}</strong>
                <p>{{ event.desc }}</p>
              </div>
            </div>
          </article>
          <article class="system-card">
            <h2>应用场景</h2>
            <button
              v-for="scenario in config.digitalTwin.scenarios"
              :key="scenario"
              :class="{ active: activeModule === scenario }"
              type="button"
              @click="selectModule(scenario)"
            >
              {{ scenario }}
            </button>
          </article>
        </aside>
      </div>

      <nav class="twin-dock" aria-label="数字孪生平台模块">
        <button
          v-for="action in config.actions"
          :key="action.title"
          :class="{ active: activeModule === action.title }"
          type="button"
          @click="selectModule(action.title)"
        >
          <strong>{{ action.tag }}</strong>
          <span>{{ action.title }}</span>
        </button>
      </nav>
    </section>

  <section v-else class="role-page">
    <el-card class="role-hero" shadow="never">
      <div class="hero-copy">
        <span class="hero-icon">
          <el-icon><User /></el-icon>
        </span>
        <div>
          <el-tag effect="light">{{ config.badge }}</el-tag>
          <h1>{{ config.title }}</h1>
        </div>
      </div>
    </el-card>

    <div class="stat-grid">
      <article v-for="stat in config.stats" :key="stat.label" class="stat-card">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <small>{{ stat.trend }}</small>
      </article>
    </div>

    <div class="action-grid">
      <article v-for="action in config.actions" :key="action.title" class="action-card">
        <div>
          <strong>{{ action.title }}</strong>
          <el-tag size="small" effect="light">{{ action.tag }}</el-tag>
        </div>
      </article>
    </div>

    <div class="workbench-grid">
      <el-card class="record-card" shadow="never">
        <template #header>
          <div class="section-title">
            <h2>{{ config.recordsTitle }}</h2>
          </div>
        </template>
        <el-table :data="config.records" class="role-table" empty-text="暂无记录">
          <el-table-column
            v-for="(column, index) in config.columns"
            :key="column"
            :label="column"
            :min-width="index === 0 ? 150 : 120"
          >
            <template #default="{ row }">
              <el-tag v-if="index === config.columns.length - 1" effect="light">{{ row[index] }}</el-tag>
              <span v-else>{{ row[index] }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.role-page {
  display: grid;
  gap: 18px;
}

.role-hero,
.stat-card,
.action-card,
.record-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.role-hero :deep(.el-card__body) {
  padding: 22px;
}

.hero-copy {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 30px;
}

.hero-copy h1 {
  margin: 10px 0 0;
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.3;
}

.hero-copy p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.stat-grid,
.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 240px), 1fr));
  gap: 14px;
}

.stat-card,
.action-card {
  min-height: 112px;
  padding: 18px;
}

.stat-card span,
.stat-card small,
.action-card p,
.section-title span {
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  color: var(--text-primary);
  font-size: 28px;
  line-height: 1;
}

.stat-card small {
  display: block;
  margin-top: 10px;
}

.action-card div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.action-card strong,
.section-title h2 {
  color: var(--text-primary);
  font-size: 15px;
}

.action-card p {
  margin: 12px 0 0;
  line-height: 1.7;
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.section-title h2 {
  margin: 0;
  font-size: 16px;
}

.section-title span {
  display: block;
  margin-top: 5px;
}

.role-table {
  width: 100%;
}

.twin-page {
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  gap: 14px;
  min-height: calc(100vh - 112px);
  margin: -20px -24px -28px;
  padding: 18px;
  overflow: hidden;
  border: 1px solid rgba(96, 165, 250, 0.42);
  border-radius: 0;
  background:
    radial-gradient(circle at 50% 34%, rgba(37, 99, 235, 0.28), transparent 42%),
    linear-gradient(180deg, #071226 0%, #050b1c 100%);
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.28);
  color: #dbeafe;
}

.twin-page::before {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(96, 165, 250, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(96, 165, 250, 0.08) 1px, transparent 1px);
  background-size: 34px 34px;
  content: '';
  pointer-events: none;
}

.command-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  min-height: 74px;
  text-align: center;
}

.command-header small {
  display: block;
  color: #67e8f9;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.command-header h1 {
  margin: 4px 0 0;
  color: #e0f2fe;
  font-size: 30px;
  font-weight: 900;
  line-height: 1.1;
  text-shadow: 0 0 22px rgba(96, 165, 250, 0.82);
}

.command-header p {
  margin: 8px 0 0;
  color: #93c5fd;
  font-size: 13px;
}

.header-wing {
  width: min(22vw, 260px);
  height: 22px;
  border-top: 1px solid rgba(96, 165, 250, 0.75);
  border-bottom: 1px solid rgba(96, 165, 250, 0.26);
  transform: skewX(-28deg);
}

.header-wing:first-child {
  transform: skewX(28deg);
}

.live-pill {
  position: absolute;
  right: 0;
  top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border: 1px solid rgba(96, 165, 250, 0.52);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.7);
  color: #bfdbfe;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
}

.live-pill:disabled {
  cursor: wait;
  opacity: 0.72;
}

.live-pill i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 5px rgba(34, 197, 94, 0.14);
}

.command-body {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(210px, 0.42fr) minmax(620px, 1fr) minmax(260px, 0.5fr);
  gap: 14px;
  min-height: 0;
}

.sensor-column,
.event-column {
  display: grid;
  gap: 10px;
  align-content: start;
}

.sensor-card,
.system-card,
.event-card,
.scene-panel {
  border: 1px solid rgba(96, 165, 250, 0.3);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.62);
  box-shadow: inset 0 0 24px rgba(96, 165, 250, 0.07);
}

.sensor-card {
  padding: 14px;
}

.sensor-card span,
.sensor-card em {
  display: block;
  color: #93c5fd;
  font-size: 12px;
  font-style: normal;
}

.sensor-card strong {
  display: block;
  margin: 8px 0;
  color: #e0f2fe;
  font-size: 26px;
  line-height: 1;
}

.sensor-card small {
  margin-left: 4px;
  color: #93c5fd;
  font-size: 12px;
}

.system-card,
.event-card,
.control-card {
  padding: 14px;
}

.system-card h2,
.event-card h2,
.control-card h2 {
  margin: 0 0 12px;
  color: #e0f2fe;
  font-size: 15px;
}

.system-card span,
.system-card button {
  display: inline-flex;
  margin: 0 6px 8px 0;
  padding: 7px 10px;
  border: 1px solid rgba(96, 165, 250, 0.25);
  border-radius: 6px;
  background: rgba(37, 99, 235, 0.24);
  color: #dbeafe;
  font-size: 12px;
}

.system-card button {
  cursor: pointer;
}

.system-card button.active,
.scene-toolbar button.active,
.twin-dock button.active {
  border-color: rgba(103, 232, 249, 0.82);
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.4), rgba(30, 64, 175, 0.52));
  box-shadow: 0 0 18px rgba(14, 165, 233, 0.24);
}

.scene-panel {
  min-height: 0;
  padding: 14px;
}

.scene-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.scene-toolbar button {
  padding: 6px 12px;
  border: 1px solid rgba(96, 165, 250, 0.38);
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.26);
  color: #bfdbfe;
  font-size: 12px;
  cursor: pointer;
}

.campus-scene {
  position: relative;
  height: clamp(360px, 44vh, 520px);
  overflow: hidden;
  border: 1px solid rgba(59, 130, 246, 0.24);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(30, 58, 138, 0.22), rgba(15, 23, 42, 0.2)),
    radial-gradient(circle at 50% 66%, rgba(34, 197, 94, 0.12), transparent 36%);
}

.campus-building-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.95) contrast(1.05);
}

.campus-scene::before {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(90deg, rgba(2, 6, 23, 0.52), rgba(15, 23, 42, 0.08) 36%, rgba(2, 6, 23, 0.5)),
    linear-gradient(180deg, rgba(15, 23, 42, 0.22), rgba(15, 23, 42, 0.48));
  content: '';
  pointer-events: none;
}

.campus-scene::after {
  position: absolute;
  inset: 0;
  z-index: 2;
  background:
    linear-gradient(rgba(96, 165, 250, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(96, 165, 250, 0.08) 1px, transparent 1px);
  background-size: 42px 42px;
  content: '';
  mix-blend-mode: screen;
  pointer-events: none;
}

.scene-skyline {
  position: absolute;
  left: 24px;
  top: 22px;
  z-index: 3;
  width: 190px;
  height: 54px;
  border-left: 3px solid #38bdf8;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.72), rgba(37, 99, 235, 0.16));
}

.hud-line {
  position: absolute;
  z-index: 4;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(103, 232, 249, 0.8), transparent);
  transform-origin: left center;
  pointer-events: none;
}

.hud-line::after {
  position: absolute;
  right: -5px;
  top: -4px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #67e8f9;
  box-shadow: 0 0 14px rgba(103, 232, 249, 0.75);
  content: '';
}

.line-a {
  left: 36%;
  top: 34%;
  width: 18%;
  transform: rotate(-8deg);
}

.line-b {
  left: 52%;
  top: 46%;
  width: 16%;
  transform: rotate(16deg);
}

.line-c {
  left: 31%;
  top: 56%;
  width: 14%;
  transform: rotate(26deg);
}

.photo-hotspots {
  position: absolute;
  inset: 0;
  z-index: 5;
}

.pin {
  position: absolute;
  z-index: 4;
  padding: 4px 8px;
  border: 1px solid rgba(96, 165, 250, 0.72);
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.82);
  color: #bfdbfe;
  font-size: 12px;
  font-style: normal;
  box-shadow: 0 0 18px rgba(59, 130, 246, 0.5);
  cursor: pointer;
}

.pin.active {
  border-color: rgba(250, 204, 21, 0.82);
  color: #fef3c7;
  box-shadow: 0 0 22px rgba(250, 204, 21, 0.34);
}

.pin::after {
  position: absolute;
  left: 50%;
  top: 100%;
  width: 1px;
  height: 48px;
  background: #60a5fa;
  content: '';
}

.pin-a {
  left: 42%;
  top: 30%;
}

.pin-b {
  left: 55%;
  top: 38%;
}

.pin-c {
  left: 64%;
  top: 27%;
}

.energy-ring {
  position: absolute;
  z-index: 4;
  border-radius: 50%;
}

.energy-ring {
  border: 1px solid rgba(56, 189, 248, 0.34);
  box-shadow: 0 0 22px rgba(56, 189, 248, 0.18);
}

.ring-a {
  left: 21%;
  top: 158px;
  width: 96px;
  height: 34px;
  transform: rotate(-18deg);
}

.ring-b {
  right: 17%;
  top: 166px;
  width: 116px;
  height: 38px;
  transform: rotate(15deg);
}

.lab-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.lab-node {
  position: relative;
  display: grid;
  min-height: 112px;
  padding: 13px;
  overflow: hidden;
  border: 1px solid rgba(96, 165, 250, 0.28);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.66);
  box-shadow: inset 0 0 20px rgba(96, 165, 250, 0.08);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.lab-node::before {
  position: absolute;
  left: 0;
  bottom: 0;
  width: var(--lab-usage);
  height: 5px;
  background: var(--lab-color);
  content: '';
}

.lab-node:hover,
.lab-node.active {
  border-color: rgba(103, 232, 249, 0.72);
  box-shadow:
    inset 0 0 22px rgba(96, 165, 250, 0.13),
    0 0 20px rgba(14, 165, 233, 0.16);
  transform: translateY(-1px);
}

.lab-node.active::after {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(103, 232, 249, 0.48);
  border-radius: 8px;
  content: '';
  pointer-events: none;
}

.node-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--lab-color);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.lab-node strong {
  margin-top: 8px;
  color: #e0f2fe;
  font-size: 18px;
}

.lab-node small {
  margin-top: 4px;
  overflow: hidden;
  color: #93c5fd;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lab-node div {
  align-self: end;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}

.lab-node em {
  color: var(--lab-color);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.lab-node b {
  color: var(--lab-color);
  font-size: 20px;
}

.event-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid rgba(96, 165, 250, 0.16);
}

.event-item:first-of-type {
  border-top: 0;
}

.event-item strong {
  color: #e0f2fe;
  font-size: 13px;
}

.event-item p {
  margin: 4px 0 0;
  color: #93c5fd;
  font-size: 12px;
  line-height: 1.5;
}

.control-card,
.control-card {
  border: 1px solid rgba(96, 165, 250, 0.3);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.62);
  box-shadow: inset 0 0 24px rgba(96, 165, 250, 0.07);
}

.control-card > strong {
  display: block;
  color: #e0f2fe;
  font-size: 14px;
}

.control-card > p {
  margin: 6px 0 12px;
  color: #93c5fd;
  font-size: 12px;
}

.control-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.control-metrics span {
  padding: 9px;
  border: 1px solid rgba(96, 165, 250, 0.18);
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.42);
}

.control-metrics em,
.control-metrics b {
  display: block;
}

.control-metrics em {
  color: #93c5fd;
  font-size: 12px;
  font-style: normal;
}

.control-metrics b {
  margin-top: 4px;
  color: #e0f2fe;
  font-size: 15px;
}

.control-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.control-actions button {
  min-height: 34px;
  border: 1px solid rgba(96, 165, 250, 0.38);
  border-radius: 6px;
  background: rgba(37, 99, 235, 0.28);
  color: #dbeafe;
  cursor: pointer;
  font-weight: 700;
}

.control-actions button:hover {
  border-color: rgba(103, 232, 249, 0.82);
  background: rgba(14, 165, 233, 0.34);
}

.twin-dock {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-top: 0;
}

.twin-dock button {
  min-height: 60px;
  border: 1px solid rgba(96, 165, 250, 0.38);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.34), rgba(15, 23, 42, 0.78));
  color: #dbeafe;
  cursor: pointer;
}

.twin-dock strong,
.twin-dock span {
  display: block;
}

.twin-dock strong {
  color: #67e8f9;
  font-size: 12px;
}

.twin-dock span {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 700;
}

.architecture-stack {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 8px;
  margin-top: 0;
}

.architecture-stack article {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
}

.architecture-stack article > strong {
  display: grid;
  place-items: center;
  min-height: 44px;
  border: 1px solid rgba(96, 165, 250, 0.4);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.78);
  color: #bfdbfe;
  font-size: 13px;
}

.architecture-stack article > div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 44px;
  padding: 8px;
  border: 1px solid rgba(96, 165, 250, 0.25);
  border-radius: 8px;
  background: rgba(30, 64, 175, 0.24);
}

.architecture-stack span {
  min-width: 106px;
  padding: 7px 10px;
  border-radius: 6px;
  background: rgba(96, 165, 250, 0.24);
  color: #dbeafe;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 1180px) {
  .command-body {
    grid-template-columns: 1fr;
  }

  .sensor-column {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .system-card {
    grid-column: 1 / -1;
  }

  .twin-dock {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-copy,
  .command-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-wing,
  .live-pill {
    display: none;
  }

  .command-header {
    text-align: left;
  }

  .command-header h1 {
    font-size: 24px;
  }

  .sensor-column,
  .lab-grid,
  .twin-dock,
  .architecture-stack article {
    grid-template-columns: 1fr;
  }

  .scene-panel {
    min-height: auto;
  }

  .campus-scene {
    height: 300px;
  }

  .building-model {
    width: 230px;
  }
}

@media (max-width: 900px) {
  .twin-page {
    min-height: calc(100vh - 92px);
    margin: -14px;
  }
}
</style>
