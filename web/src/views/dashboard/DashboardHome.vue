<script setup>
import { computed, markRaw, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Calendar,
  CircleCheck,
  Cpu,
  DataAnalysis,
  DocumentChecked,
  Monitor,
  OfficeBuilding,
  Tickets,
  TrendCharts,
  WarningFilled,
} from '@element-plus/icons-vue'
import { fetchDashboardOverview } from '../../api/dashboard'
import http from '../../api/http'
import { API_ENDPOINTS } from '../../api/endpoints'
import { buildDashboardScheduleView, buildDoorReminderList } from './dashboardSchedule'

defineOptions({
  name: 'DashboardHome',
})

const dashboardData = ref({
  stats: [],
  labUsage: [],
  deviceStatus: [],
  notices: [],
  doorAlerts: [],
  todaySchedules: [],
  recentActivities: [],
})
const isLoading = ref(false)
const errorMessage = ref('')
const currentTime = ref(new Date())
const consumableRecords = ref([])
let clockTimer = null
const router = useRouter()
const SCHEDULE_DETAIL_PATH = '/lab-admin/class-timetable-data'
const DEVICE_STATUS_PATH = '/lab-admin/device-status'

const DASHBOARD_ICON_MAP = {
  lab: markRaw(OfficeBuilding),
  device: markRaw(Cpu),
  normal: markRaw(CircleCheck),
  fault: markRaw(WarningFilled),
  calendar: markRaw(Calendar),
  reservation: markRaw(Calendar),
  repair: markRaw(Tickets),
  record: markRaw(DocumentChecked),
  statistics: markRaw(TrendCharts),
}

const sourceDoorAlerts = computed(() => dashboardData.value.doorAlerts)
const schedules = computed(() => dashboardData.value.todaySchedules)
const scheduleView = computed(() => buildDashboardScheduleView(schedules.value, currentTime.value, { limit: 8 }))
const scheduleRows = computed(() => scheduleView.value.visibleItems)
const doorReminderView = computed(() =>
  buildDoorReminderList(schedules.value, sourceDoorAlerts.value, currentTime.value, { limit: 5 }),
)
const normalizePath = (path) => {
  if (!path) return ''
  if (path.startsWith('/lab-admin')) return path
  const redirectMap = {
    '/reservations': '/lab-admin/reservations',
    '/repairs': '/lab-admin/repairs',
    '/records': '/lab-admin/records',
    '/environments': '/lab-admin/environments',
    '/device-status': DEVICE_STATUS_PATH,
    '/devices': '/lab-admin/devices',
    '/room-control': '/lab-admin/room-control',
  }
  return redirectMap[path] || path
}

const getResponseData = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.records)) return payload.data.records
  if (Array.isArray(payload?.records)) return payload.records
  return []
}

const toStockNumber = (value, fallback = 0) => {
  const number = Number(String(value ?? '').replace(/[^\d.-]/g, ''))
  return Number.isFinite(number) ? number : fallback
}

const getConsumableState = (stock, warnThreshold) => {
  if (stock <= 0) return { status: '待补货', tagType: 'danger' }
  if (stock <= warnThreshold) return { status: '低库存', tagType: 'warning' }
  return { status: '库存充足', tagType: 'success' }
}

const normalizeConsumable = (item = {}) => {
  const stock = Math.max(toStockNumber(item.stock), 0)
  const warnThreshold = Math.max(toStockNumber(item.warnThreshold, 5), 0)
  const state = getConsumableState(stock, warnThreshold)

  return {
    id: item.id,
    name: item.name || '未命名耗材',
    category: item.category || '未分类',
    stock,
    unit: item.unit || '件',
    warnThreshold,
    status: item.status || state.status,
    tagType: item.tagType || state.tagType,
  }
}

const consumableCategoryRows = computed(() => {
  const groups = consumableRecords.value.reduce((result, item) => {
    const current = result[item.category] || { category: item.category, count: 0, stock: 0 }
    current.count += 1
    current.stock += item.stock
    result[item.category] = current
    return result
  }, {})
  return Object.values(groups).sort((first, second) => second.stock - first.stock)
})

const maxConsumableStock = computed(() => Math.max(...consumableCategoryRows.value.map((item) => item.stock), 1))
const visibleConsumableCategoryRows = computed(() => consumableCategoryRows.value.slice(0, 4))
const hiddenConsumableCategoryCount = computed(() => Math.max(consumableCategoryRows.value.length - visibleConsumableCategoryRows.value.length, 0))

const consumableSummary = computed(() => {
  const warningCount = consumableRecords.value.filter((item) => item.tagType === 'warning' || item.tagType === 'danger').length
  const totalStock = consumableRecords.value.reduce((sum, item) => sum + item.stock, 0)

  return {
    totalKinds: consumableRecords.value.length,
    totalStock,
    warningCount,
    categoryCount: new Set(consumableRecords.value.map((item) => item.category).filter(Boolean)).size,
  }
})

const ACTIVITY_TAG_TYPES = new Set(['primary', 'success', 'warning', 'danger', 'info'])

const normalizeActivityType = (value) => {
  const type = String(value || '').trim()
  if (ACTIVITY_TAG_TYPES.has(type)) return type
  if (/预警|告警|维修|故障|待处理/.test(type)) return 'warning'
  if (/完成|正常|通过|同步/.test(type)) return 'success'
  if (/课程|课表|预约|门禁/.test(type)) return 'primary'
  return 'info'
}

const normalizeActivity = (item = {}, index = 0) => {
  const typeLabel = item.category || item.typeLabel || item.status || item.type || '动态'

  return {
    id: item.id || `${item.time || 'activity'}-${index}`,
    time: item.time || '刚刚',
    title: item.title || item.name || '实验室动态',
    desc: item.desc || item.description || item.content || '暂无详情',
    type: normalizeActivityType(item.type || item.statusType || typeLabel),
    typeLabel,
  }
}

const generatedActivities = computed(() => {
  const items = []

  doorReminderView.value.visibleItems.slice(0, 2).forEach((item, index) => {
    items.push({
      id: `door-${item.id || index}`,
      time: item.time,
      title: `${item.lab} ${item.status}`,
      desc: `${item.title}，${item.audience} / ${item.person}`,
      type: item.statusType,
      typeLabel: '门禁',
    })
  })

  scheduleRows.value.slice(0, 3).forEach((item, index) => {
    items.push({
      id: `schedule-${item.id || index}`,
      time: item.time,
      title: `${item.lab} ${item.course}`,
      desc: `${item.className} · ${item.teacher}`,
      type: item.statusType,
      typeLabel: item.status || '课程',
    })
  })

  const warningItems = consumableRecords.value.filter((item) => item.tagType === 'warning' || item.tagType === 'danger')
  if (warningItems.length) {
    items.push({
      id: 'consumable-warning',
      time: '库存',
      title: `耗材库存预警 ${warningItems.length} 项`,
      desc: warningItems.slice(0, 3).map((item) => item.name).join('、'),
      type: 'warning',
      typeLabel: '耗材',
    })
  }

  return items.map(normalizeActivity).slice(0, 6)
})

const recentActivities = computed(() => {
  const source = Array.isArray(dashboardData.value.recentActivities) ? dashboardData.value.recentActivities : []
  const normalizedSource = source.map(normalizeActivity)
  return (normalizedSource.length ? normalizedSource : generatedActivities.value).slice(0, 6)
})

const stats = computed(() =>
  dashboardData.value.stats.map((stat) => ({
    ...stat,
    title: stat.title || stat.label || '实验室信息',
    icon: DASHBOARD_ICON_MAP[stat.iconKey] || markRaw(DataAnalysis),
  })),
)

const loadDashboardOverview = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const [overview, consumables] = await Promise.all([fetchDashboardOverview(), fetchConsumableRecords()])
    dashboardData.value = overview
    consumableRecords.value = consumables
  } catch (error) {
    errorMessage.value = '工作台数据加载失败，请稍后重试'
    if (!error?.__handled) {
      ElMessage.error(errorMessage.value)
    }
  } finally {
    isLoading.value = false
  }
}

const fetchConsumableRecords = async () => {
  try {
    const response = await http.get(API_ENDPOINTS.consumables.list)
    return getResponseData(response.data).map(normalizeConsumable)
  } catch {
    return []
  }
}

const openPath = (path) => {
  const nextPath = normalizePath(path)
  if (!nextPath) return
  router.push(nextPath)
}

onMounted(() => {
  loadDashboardOverview()
  clockTimer = window.setInterval(() => {
    currentTime.value = new Date()
  }, 60000)
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
  }
})
</script>

<template>
  <section class="dashboard-page" v-loading="isLoading">
    <el-card class="page-heading" shadow="never">
      <div class="page-heading-content">
        <div>
          <h1>实验室管理员工作台</h1>
        </div>
        <el-button type="primary" plain @click="loadDashboardOverview">刷新工作台</el-button>
      </div>
    </el-card>

    <el-alert
      v-if="errorMessage"
      class="dashboard-alert"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button size="small" type="primary" @click="loadDashboardOverview">重新加载</el-button>
      </template>
    </el-alert>

    <section class="section-block">
      <div class="section-heading">
        <div>
          <h2>实验室概览</h2>
          <span>按当前实验室数据汇总运行信息</span>
        </div>
      </div>
      <div class="stats-grid">
        <article v-for="stat in stats" :key="stat.title" class="stat-card">
          <div class="stat-top">
            <span class="stat-icon" :style="{ color: stat.color, backgroundColor: `${stat.color}14` }">
              <component :is="stat.icon" />
            </span>
            <span class="stat-trend">{{ stat.trend }}</span>
          </div>
          <div class="stat-value">
            <strong>{{ Number(stat.value).toLocaleString() }}</strong>
            <span>{{ stat.unit }}</span>
          </div>
          <p>{{ stat.title }}</p>
        </article>
      </div>
    </section>

    <div class="workbench-grid primary-grid">
      <el-card class="module-card alert-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2>实验室开门提醒</h2>
              <span>15 分钟内待开门与当前使用中的实验室</span>
            </div>
          </div>
        </template>
        <div class="alert-list">
          <el-empty
            v-if="!doorReminderView.visibleItems.length"
            class="alert-empty"
            description="暂无需要开门的实验室"
            :image-size="88"
          />
          <template v-else>
            <article v-for="item in doorReminderView.visibleItems" :key="item.id" class="alert-item">
              <div class="alert-time">
                <strong>{{ item.lab }}</strong>
                <time>{{ item.time }}</time>
              </div>
              <div class="alert-main">
                <strong :title="item.title">{{ item.title }}</strong>
                <p>
                  <span>{{ item.audience }}</span>
                  <span>{{ item.person }}</span>
                </p>
              </div>
              <el-tag :type="item.statusType" effect="light" size="small">{{ item.status }}</el-tag>
            </article>
          </template>
        </div>
        <div v-if="doorReminderView.hasMore" class="card-actions is-compact">
          <el-button link type="primary" @click="openPath(SCHEDULE_DETAIL_PATH)">
            查看更多
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>

      <el-card class="module-card schedule-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2>实验室今日安排</h2>
              <span>当前进行、即将开始与最近后续课程</span>
            </div>
          </div>
        </template>
        <el-table :data="scheduleRows" class="schedule-table" empty-text="今日暂无实验室安排" height="306">
          <el-table-column label="时间" min-width="116" prop="time" />
          <el-table-column label="实验室" min-width="86" prop="lab" />
          <el-table-column label="课程" min-width="180" prop="course" show-overflow-tooltip />
          <el-table-column label="班级" min-width="150" prop="className" show-overflow-tooltip />
          <el-table-column label="教师" min-width="92" prop="teacher" />
          <el-table-column label="状态" min-width="112">
            <template #default="{ row }">
              <el-tag :type="row.statusType" effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="scheduleView.hasMore" class="card-footer">
          <span>已显示 {{ scheduleRows.length }} 条重点安排，共 {{ scheduleView.totalToday }} 条今日课程</span>
          <el-button link type="primary" @click="openPath(SCHEDULE_DETAIL_PATH)">
            查看完整课表
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>

    <div class="workbench-grid secondary-grid">
      <el-card class="module-card inventory-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2>库存统计数据</h2>
              <span>按耗材分类汇总当前库存与预警状态</span>
            </div>
            <el-tag :type="consumableSummary.warningCount ? 'warning' : 'success'" effect="light">
              预警 {{ consumableSummary.warningCount }} 项
            </el-tag>
          </div>
        </template>
        <div class="inventory-body">
          <div class="inventory-summary-grid">
            <div>
              <span>耗材种类</span>
              <strong>{{ consumableSummary.totalKinds.toLocaleString() }}</strong>
            </div>
            <div>
              <span>库存总量</span>
              <strong>{{ consumableSummary.totalStock.toLocaleString() }}</strong>
            </div>
            <div>
              <span>分类数量</span>
              <strong>{{ consumableSummary.categoryCount.toLocaleString() }}</strong>
            </div>
          </div>

          <div class="inventory-category-panel">
            <el-empty
              v-if="!visibleConsumableCategoryRows.length"
              class="inline-empty"
              description="暂无库存统计数据"
              :image-size="82"
            />
            <template v-else>
              <div class="inventory-category-grid">
                <article v-for="item in visibleConsumableCategoryRows" :key="item.category" class="inventory-category-item">
                  <div class="inventory-category-head">
                    <span>{{ item.category }}</span>
                    <strong>{{ item.stock.toLocaleString() }}</strong>
                  </div>
                  <div class="status-bar">
                    <i :style="{ width: `${Math.max((item.stock / maxConsumableStock) * 100, 8)}%`, backgroundColor: '#2563eb' }"></i>
                  </div>
                  <small>{{ item.count }} 种耗材</small>
                </article>
              </div>
              <p v-if="hiddenConsumableCategoryCount" class="inventory-more">
                另有 {{ hiddenConsumableCategoryCount }} 类库存已收起，可进入耗材库存查看完整明细。
              </p>
            </template>
          </div>
        </div>
        <div class="card-actions">
          <el-button plain type="primary" @click="openPath('/lab-admin/consumables')">
            查看耗材库存
          </el-button>
        </div>
      </el-card>

      <el-card class="module-card activity-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2>实验室最近动态</h2>
              <span>记录实验室今日关键业务变化</span>
            </div>
          </div>
        </template>
        <div class="activity-list">
          <el-empty
            v-if="!recentActivities.length"
            class="activity-empty"
            description="暂无最新动态"
            :image-size="82"
          />
          <template v-else>
            <article v-for="item in recentActivities" :key="item.id" class="activity-item">
              <div class="activity-meta">
                <time>{{ item.time }}</time>
                <el-tag :type="item.type" effect="light" size="small">{{ item.typeLabel }}</el-tag>
              </div>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </article>
          </template>
        </div>
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  --dashboard-gap: 16px;
  --panel-padding: 18px;
  display: grid;
  gap: var(--dashboard-gap);
}

.page-heading,
.module-card,
.stat-card,
.section-block {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  box-shadow: none;
}

.page-heading :deep(.el-card__body) {
  padding: 20px 22px;
}

.page-heading-content,
.card-header,
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
}

.page-heading-content > div,
.card-header > div,
.section-heading > div {
  min-width: 0;
}

.page-heading h1,
.section-heading h2,
.card-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-weight: 700;
  letter-spacing: 0;
}

.page-heading h1 {
  font-size: 22px;
  line-height: 1.3;
}

.section-heading span,
.card-header span {
  color: var(--text-secondary);
  font-size: 13px;
}

.dashboard-page :deep(.el-button) {
  border-radius: 8px;
  font-weight: 600;
}

.dashboard-page :deep(.el-tag) {
  border-radius: 6px;
  font-weight: 600;
}

.dashboard-alert {
  margin: 0;
}

.section-block {
  padding: var(--panel-padding);
}

.section-heading {
  margin-bottom: 14px;
}

.section-heading h2,
.card-header h2 {
  font-size: 16px;
  line-height: 1.35;
}

.section-heading span,
.card-header span {
  display: block;
  margin-top: 5px;
  line-height: 1.45;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
  min-height: 126px;
  padding: 15px;
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.stat-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  font-size: 21px;
}

.stat-trend {
  overflow: hidden;
  min-width: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
}

.stat-value strong {
  color: var(--text-primary);
  font-size: 29px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.stat-value span,
.stat-card p {
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-card p {
  margin: 0;
  line-height: 1.45;
}

.workbench-grid {
  display: grid;
  gap: var(--dashboard-gap);
  align-items: stretch;
  min-width: 0;
}

.primary-grid {
  grid-template-columns: minmax(320px, 0.72fr) minmax(0, 1.28fr);
}

.secondary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.module-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.module-card :deep(.el-card__header) {
  flex: 0 0 auto;
  min-height: 64px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-light);
  background: #ffffff;
}

.module-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  padding: 16px 18px 18px;
}

.alert-card,
.schedule-card {
  min-height: 418px;
}

.alert-list {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 0;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.alert-empty {
  flex: 1 1 auto;
  height: 100%;
}

.alert-item,
.inventory-category-item,
.activity-item {
  min-width: 0;
}

.alert-item {
  display: grid;
  grid-template-columns: minmax(72px, 86px) minmax(0, 1fr) auto;
  align-items: center;
  flex: 0 0 auto;
  gap: 12px;
  min-height: 58px;
  padding: 11px 0;
  border-bottom: 1px solid var(--border-light);
}

.alert-item:last-child {
  border-bottom: 0;
}

.alert-time {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.alert-time strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-time time,
.activity-item time {
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 700;
}

.alert-main {
  min-width: 0;
}

.alert-main strong {
  display: block;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-item p,
.activity-item p {
  margin: 6px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.55;
}

.alert-main p {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-bottom: 0;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--border-light);
}

.card-actions.is-compact {
  margin-top: 10px;
  padding-top: 10px;
}

.schedule-table {
  flex: 1;
  width: 100%;
  min-height: 0;
}

.schedule-table :deep(.el-table__header-wrapper th) {
  background: #f8fafc;
  color: var(--text-secondary);
  font-weight: 700;
}

.schedule-table :deep(.el-table__cell) {
  padding: 8px 0;
}

.schedule-table :deep(.cell) {
  color: #334155;
  font-size: 13px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
  color: var(--text-secondary);
  font-size: 12px;
}

.inventory-card :deep(.el-card__body) {
  padding-top: 18px;
}

.inventory-body {
  display: grid;
  gap: 14px;
}

.inventory-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.inventory-summary-grid div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  min-height: 72px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #f8fbff;
}

.inventory-summary-grid span,
.inventory-category-item span,
.inventory-category-item small {
  color: var(--text-secondary);
  font-size: 12px;
}

.inventory-summary-grid strong {
  color: var(--text-primary);
  font-size: 26px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.inventory-category-panel {
  min-width: 0;
}

.inventory-category-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.secondary-grid .inventory-category-grid,
.secondary-grid .activity-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.inventory-category-item {
  display: grid;
  gap: 10px;
  align-content: start;
  min-height: 96px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #ffffff;
}

.inventory-category-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.inventory-category-item strong {
  color: var(--text-primary);
  font-size: 24px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.inventory-category-item small {
  line-height: 1.35;
}

.inventory-more {
  margin: 12px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.inline-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 140px;
}

.status-bar {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.status-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width 0.2s ease;
}

.activity-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.activity-item {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 116px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #ffffff;
}

.activity-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.activity-item strong {
  display: block;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-empty {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
}

@media (max-width: 1280px) {
  .primary-grid,
  .secondary-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .inventory-category-grid,
  .activity-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .page-heading-content,
  .card-header,
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .stats-grid,
  .inventory-summary-grid,
  .inventory-category-grid,
  .activity-list {
    grid-template-columns: 1fr;
  }

  .alert-item {
    grid-template-columns: 1fr;
  }

  .alert-item .el-tag {
    justify-self: flex-start;
  }

}
</style>
