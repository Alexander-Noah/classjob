<!--
  Web 管理平台的全校课表采集视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { credentialPresentation, crawlerTaskStatusLabel } from '../../modules/timetable/crawlerPresentation'
import {
  clearClassTimetableCrawlerTasks,
  deleteClassTimetableCrawlerCredential,
  getClassTimetableCrawlerCredential,
  getClassTimetableCrawlerConfig,
  getClassTimetableSummary,
  listClassTimetableCrawlerTasks,
  saveClassTimetableCrawlerConfig,
  saveClassTimetableCrawlerCredential,
  triggerClassTimetableCrawler,
} from '../../api/services/classTimetable'

defineOptions({
  name: 'LabAdminClassTimetable',
})

const ACCOUNT_REMARK_STORAGE_KEY = 'lab-admin-class-timetable:account-remark'

const intervalOptions = [2, 4, 6, 8, 12]
const weekdayOptions = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' },
]

const configForm = reactive({
  scheduleEnabled: true,
  frequency: 'interval',
  intervalHours: 6,
  dailyTime: '02:00',
  weeklyDay: 'MON',
  weeklyTime: '02:00',
  cron: '0 0 */6 * * *',
  semester: '',
  semesterStartDate: '',
  nextRunAt: '',
})

const accountFilter = reactive({
  keyword: '',
  status: '',
})

const accountPagination = reactive({
  page: 1,
  pageSize: 10,
})

const taskPagination = reactive({
  page: 1,
  pageSize: 6,
})

const accountDialogVisible = ref(false)
const taskDetailVisible = ref(false)
const credential = ref({ configured: false, status: 'loading' })
const credentialState = computed(/** 计算派生状态。 */ () => credentialPresentation(credential.value))
const credentialForm = reactive({
  username: '',
  password: '',
  remark: '',
})

const summary = ref({})
const taskRecords = ref([])
const activeTask = ref(null)
const isCrawling = ref(false)
const isSavingConfig = ref(false)
const isSavingCredential = ref(false)

const accountRows = computed(/** 计算派生状态。 */ () => {
  if (!credential.value?.configured) return []
  const keyword = accountFilter.keyword.trim()
  const row = {
    id: 1,
    username: credential.value.usernameMasked || '已配置账号',
    remark: credentialForm.remark || '课表采集账号',
    sort: 0,
    status: credential.value.usable === false ? '待更新' : '启用',
    latestSuccessAt: summary.value.latestSyncAt || credential.value.updatedAt || '',
    latestError: credential.value.message || taskRecords.value.find(/** 判断当前条目是否为查找目标。 入参：task。 */ (task) => task.status !== 'success')?.error || '',
  }
  const matchedKeyword = !keyword || row.username.includes(keyword) || row.remark.includes(keyword)
  const matchedStatus = !accountFilter.status || row.status === accountFilter.status
  return matchedKeyword && matchedStatus ? [row] : []
})

const pagedAccountRows = computed(/** 计算派生状态。 */ () => {
  const start = (accountPagination.page - 1) * accountPagination.pageSize
  return accountRows.value.slice(start, start + accountPagination.pageSize)
})

const pagedTaskRecords = computed(/** 计算派生状态。 */ () => {
  const start = (taskPagination.page - 1) * taskPagination.pageSize
  return taskRecords.value.slice(start, start + taskPagination.pageSize)
})

const latestTask = computed(/** 计算派生状态。 */ () => taskRecords.value[0] || null)

const scheduleSummary = computed(/** 计算派生状态。 */ () => {
  if (!configForm.scheduleEnabled) return '已关闭自动采集'
  if (configForm.frequency === 'daily') return `每天 ${configForm.dailyTime} 自动采集`
  if (configForm.frequency === 'weekly') {
    const weekday = weekdayOptions.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.value === configForm.weeklyDay)?.label || '周一'
    return `每${weekday} ${configForm.weeklyTime} 自动采集`
  }
  return `每 ${configForm.intervalHours} 小时自动采集`
})

const nextRunText = computed(/** 计算派生状态。 */ () => {
  if (!configForm.scheduleEnabled) return '自动采集已关闭'
  if (!credentialState.value.canCollect) return '账号可用后恢复自动采集'
  return configForm.nextRunAt || '保存配置后生成'
})

/**
 * 加载本地状态。
 */
const loadLocalState = () => {
  credentialForm.remark = window.localStorage?.getItem(ACCOUNT_REMARK_STORAGE_KEY) || '课表采集账号'
}

/**
 * 解析时间。
 * 入参：value（值）；fallback（fallback）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseTime = (value, fallback = '02:00') => {
  const matched = String(value || '').match(/^(\d{1,2}):(\d{2})$/)
  if (!matched) return fallback
  const hour = Math.min(Math.max(Number(matched[1]), 0), 23)
  const minute = Math.min(Math.max(Number(matched[2]), 0), 59)
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

/**
 * 解析Cron目标Form。
 * 入参：cron（cron）。
 */
const parseCronToForm = (cron) => {
  const parts = String(cron || '').trim().split(/\s+/)
  if (parts.length !== 6) {
    configForm.frequency = 'interval'
    configForm.intervalHours = 6
    return
  }

  const [, minute, hour, dayOfMonth, month, dayOfWeek] = parts
  const intervalMatched = hour.match(/^\*\/(\d+)$/)
  if (minute === '0' && intervalMatched && dayOfMonth === '*' && month === '*' && dayOfWeek === '*') {
    const interval = Number(intervalMatched[1])
    configForm.frequency = 'interval'
    configForm.intervalHours = intervalOptions.includes(interval) ? interval : 6
    return
  }

  if (dayOfMonth === '*' && month === '*' && dayOfWeek === '*') {
    configForm.frequency = 'daily'
    configForm.dailyTime = parseTime(`${hour}:${minute}`)
    return
  }

  if (dayOfMonth === '*' && month === '*' && weekdayOptions.some(/** 判断当前条目是否满足任一匹配条件。 入参：item。 */ (item) => item.value === dayOfWeek)) {
    configForm.frequency = 'weekly'
    configForm.weeklyDay = dayOfWeek
    configForm.weeklyTime = parseTime(`${hour}:${minute}`)
    return
  }

  configForm.frequency = 'interval'
  configForm.intervalHours = 6
}

/**
 * 构建Cron来源配置。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const buildCronFromConfig = () => {
  if (configForm.frequency === 'daily') {
    const [hour, minute] = parseTime(configForm.dailyTime).split(':')
    return `0 ${Number(minute)} ${Number(hour)} * * *`
  }
  if (configForm.frequency === 'weekly') {
    const [hour, minute] = parseTime(configForm.weeklyTime).split(':')
    return `0 ${Number(minute)} ${Number(hour)} * * ${configForm.weeklyDay}`
  }
  return `0 0 */${configForm.intervalHours} * * *`
}

/**
 * 应用Crawler配置。
 * 入参：config（配置）。
 */
const applyCrawlerConfig = (config = {}) => {
  configForm.scheduleEnabled =
    config.scheduleEnabled === undefined || config.scheduleEnabled === null
      ? configForm.scheduleEnabled
      : config.scheduleEnabled === true || config.scheduleEnabled === 1 || config.scheduleEnabled === '1'
  configForm.cron = config.cron || configForm.cron
  configForm.semester = config.semester || configForm.semester
  configForm.semesterStartDate = config.semesterStartDate || configForm.semesterStartDate
  configForm.nextRunAt = config.nextRunAt || ''
  parseCronToForm(configForm.cron)
}

/**
 * 获取Crawler配置。
 */
const fetchCrawlerConfig = async () => {
  applyCrawlerConfig((await getClassTimetableCrawlerConfig()) || {})
}

/**
 * 获取Task记录集合。
 */
const fetchTaskRecords = async () => {
  taskRecords.value = (await listClassTimetableCrawlerTasks({ limit: 100 })) || []
}

/**
 * 获取凭据。
 */
const fetchCredential = async () => {
  try {
    credential.value = await getClassTimetableCrawlerCredential()
  } catch {
    credential.value = { ...credential.value, usable: false, status: 'load_failed' }
  }
}

/**
 * 获取摘要。
 */
const fetchSummary = async () => {
  summary.value = (await getClassTimetableSummary()) || {}
}

/**
 * 刷新All。
 */
const refreshAll = async () => {
  await Promise.all([fetchCredential(), fetchSummary(), fetchCrawlerConfig(), fetchTaskRecords()])
}

/**
 * 保存配置。
 */
const saveConfig = async () => {
  if (!configForm.semester) {
    ElMessage.warning('请选择课表学期')
    return
  }
  if (!configForm.semesterStartDate) {
    ElMessage.warning('请选择学期起始日期')
    return
  }

  isSavingConfig.value = true
  try {
    const config = await saveClassTimetableCrawlerConfig({
      scheduleEnabled: configForm.scheduleEnabled,
      cron: buildCronFromConfig(),
      semester: configForm.semester,
      semesterStartDate: configForm.semesterStartDate,
    })
    applyCrawlerConfig(config)
    ElMessage.success('课表采集配置已保存')
  } catch (error) {
    ElMessage.error(error?.message || '课表采集配置保存失败')
  } finally {
    isSavingConfig.value = false
  }
}

/**
 * 打开AccountDialog。
 */
const openAccountDialog = () => {
  credentialForm.username = ''
  credentialForm.password = ''
  credentialForm.remark = credentialForm.remark || '课表采集账号'
  accountDialogVisible.value = true
}

/**
 * 保存凭据。
 */
const saveCredential = async () => {
  if (!credentialForm.username.trim() || !credentialForm.password.trim()) {
    ElMessage.warning('请填写教务账号和密码')
    return
  }

  isSavingCredential.value = true
  try {
    credential.value = await saveClassTimetableCrawlerCredential({
      username: credentialForm.username,
      password: credentialForm.password,
    })
    window.localStorage?.setItem(ACCOUNT_REMARK_STORAGE_KEY, credentialForm.remark || '课表采集账号')
    credentialForm.password = ''
    accountDialogVisible.value = false
    ElMessage.success('教务账号已加密保存')
  } catch (error) {
    ElMessage.error(error?.message || '教务账号保存失败')
  } finally {
    isSavingCredential.value = false
  }
}

/**
 * 删除凭据。
 */
const deleteCredential = async () => {
  try {
    await ElMessageBox.confirm('删除后后台采集服务将无法登录教务系统，确认删除这个账号吗？', '删除教务账号', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteClassTimetableCrawlerCredential()
    credential.value = { configured: false }
    credentialForm.username = ''
    credentialForm.password = ''
    ElMessage.success('已删除教务账号')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.message || '删除教务账号失败')
  }
}

/**
 * 重置Account筛选条件。
 */
const resetAccountFilter = () => {
  accountFilter.keyword = ''
  accountFilter.status = ''
  accountPagination.page = 1
}

/**
 * 查询Account行集合。
 */
const queryAccountRows = () => {
  accountPagination.page = 1
}

/**
 * 处理Crawler。
 */
const handleCrawler = async () => {
  if (!credentialState.value.canCollect) {
    ElMessage.warning(credentialState.value.description)
    return
  }

  isCrawling.value = true
  try {
    const result = await triggerClassTimetableCrawler()
    if (result?.status === 'success') {
      ElMessage.success(result.message || '课表采集完成')
    } else {
      ElMessage.warning(result?.message || '采集任务已结束，请查看任务记录')
    }
    await refreshAll()
  } catch (error) {
    ElMessage.error(
      error?.code === 'ECONNABORTED'
        ? '前端等待超时，采集任务可能仍在后台运行，请稍后刷新'
        : error?.message || '课表采集启动失败',
    )
    await fetchTaskRecords()
  } finally {
    isCrawling.value = false
  }
}

/**
 * 打开Task详情。
 * 入参：task（task）。
 */
const openTaskDetail = (task) => {
  activeTask.value = task
  taskDetailVisible.value = true
}

/**
 * 清理Task记录集合。
 */
const clearTaskRecords = async () => {
  if (!taskRecords.value.length) return
  try {
    await ElMessageBox.confirm('确认清空采集任务记录吗？', '清空任务记录', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await clearClassTimetableCrawlerTasks()
    taskRecords.value = []
    ElMessage.success('采集任务记录已清空')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '清空任务记录失败')
    }
  }
}

/**
 * 获取Task状态Tag类型。
 * 入参：status（状态）。
 * @returns 文本结果。
 */
const getTaskStatusTagType = (status) => {
  if (status === 'success') return 'success'
  if (status === 'running') return 'primary'
  if (status === 'skipped') return 'warning'
  return 'danger'
}

/**
 * 格式化Trigger类型。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatTriggerType = (value) => {
  if (value === 'scheduled') return '定时'
  if (value === 'manual') return '手动'
  return value || '-'
}

onMounted(/** 在组件挂载后初始化相关状态。 */ async () => {
  loadLocalState()
  await refreshAll()
})
</script>

<template>
  <section class="class-timetable-page">
    <header class="page-head">
      <div>
        <h1>教学课表数据采集</h1>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button :disabled="!credentialState.canCollect" :loading="isCrawling" type="primary" @click="handleCrawler">
          立即采集
        </el-button>
      </div>
    </header>

    <section class="status-grid">
      <article class="status-card">
        <span>采集状态</span>
        <strong>{{ credentialState.title }}</strong>
        <small>{{ credentialState.description }}</small>
      </article>
      <article class="status-card">
        <span>当前计划</span>
        <strong>{{ scheduleSummary }}</strong>
        <small>下次采集：{{ nextRunText }}</small>
      </article>
      <article class="status-card">
        <span>最近任务</span>
        <strong>{{ crawlerTaskStatusLabel(latestTask?.status) }}</strong>
        <small>{{ latestTask?.finishedAt || latestTask?.startedAt || '完成采集后生成任务记录' }}</small>
      </article>
    </section>

    <el-card class="panel-card config-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <strong>采集配置</strong>
          </div>
          <el-button :loading="isSavingConfig" type="primary" @click="saveConfig">保存配置</el-button>
        </div>
      </template>

      <div class="config-layout">
        <aside class="config-note">
          <span class="note-label">采集策略</span>
          <h2>{{ scheduleSummary }}</h2>
        </aside>

        <el-form class="config-form" label-position="top">
          <el-form-item label="定时采集">
            <el-switch v-model="configForm.scheduleEnabled" active-text="开启" inactive-text="关闭" />
          </el-form-item>

          <el-form-item label="采集频率">
            <el-radio-group v-model="configForm.frequency" :disabled="!configForm.scheduleEnabled">
              <el-radio-button value="interval">间隔采集</el-radio-button>
              <el-radio-button value="daily">每天采集</el-radio-button>
              <el-radio-button value="weekly">每周采集</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="configForm.frequency === 'interval'" label="时间间隔">
            <el-select v-model="configForm.intervalHours" :disabled="!configForm.scheduleEnabled">
              <el-option
                v-for="hours in intervalOptions"
                :key="hours"
                :label="`每 ${hours} 小时`"
                :value="hours"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="configForm.frequency === 'daily'" label="每天时间">
            <el-time-picker
              v-model="configForm.dailyTime"
              :disabled="!configForm.scheduleEnabled"
              format="HH:mm"
              value-format="HH:mm"
            />
          </el-form-item>

          <template v-if="configForm.frequency === 'weekly'">
            <el-form-item label="星期">
              <el-select v-model="configForm.weeklyDay" :disabled="!configForm.scheduleEnabled">
                <el-option
                  v-for="weekday in weekdayOptions"
                  :key="weekday.value"
                  :label="weekday.label"
                  :value="weekday.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="当天时间">
              <el-time-picker
                v-model="configForm.weeklyTime"
                :disabled="!configForm.scheduleEnabled"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
          </template>

          <el-form-item label="下次采集">
            <el-input :model-value="nextRunText" disabled />
          </el-form-item>

          <el-form-item label="学期起始日期">
            <el-date-picker
              v-model="configForm.semesterStartDate"
              placeholder="选择日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-form-item label="课表学期">
            <el-select
              v-model="configForm.semester"
              allow-create
              filterable
              default-first-option
              placeholder="选择或输入学期，如 2026-2027-1"
            >
              <el-option
                v-for="semester in summary.semesters || []"
                :key="semester"
                :label="semester"
                :value="semester"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <strong>教务账号管理</strong>
          </div>
          <el-button :icon="Plus" type="primary" @click="openAccountDialog">新增账号</el-button>
        </div>
      </template>

      <div class="account-filter">
        <label class="filter-item">
          <span>关键字</span>
          <el-input v-model="accountFilter.keyword" clearable placeholder="按账号或备注搜索" />
        </label>
        <label class="filter-item">
          <span>状态</span>
          <el-select v-model="accountFilter.status" clearable placeholder="全部">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </label>
        <div class="filter-buttons">
          <el-button :icon="Search" type="primary" @click="queryAccountRows">查询</el-button>
          <el-button @click="resetAccountFilter">重置</el-button>
        </div>
      </div>

      <el-alert
        v-if="!credentialState.canCollect && credential.status !== 'loading'"
        class="table-alert"
        show-icon
        :title="credentialState.title"
        type="warning"
        :closable="false"
      >
        <span>{{ credentialState.description }}</span>
        <el-button v-if="credential.status === 'load_failed'" link type="primary" @click="fetchCredential">重新加载</el-button>
        <el-button v-else link type="primary" @click="openAccountDialog">{{ credential.configured ? '重新配置账号' : '新增账号' }}</el-button>
      </el-alert>

      <el-table :data="pagedAccountRows" border class="management-table">
        <el-table-column label="ID" prop="id" width="90" />
        <el-table-column label="教务账号" min-width="180" prop="username" />
        <el-table-column label="备注" min-width="180" prop="remark" />
        <el-table-column label="排序" prop="sort" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="credentialState.canCollect ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近成功时间" min-width="180" prop="latestSuccessAt" />
        <el-table-column label="最近失败信息" min-width="220" prop="latestError" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default>
            <el-button link type="primary" @click="openAccountDialog">编辑</el-button>
            <el-button link type="danger" @click="deleteCredential">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="accountPagination.page"
          v-model:page-size="accountPagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="accountRows.length"
          background
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <strong>采集任务记录</strong>
          </div>
          <el-button :disabled="!taskRecords.length" :icon="Delete" @click="clearTaskRecords">清空记录</el-button>
        </div>
      </template>

      <el-table :data="pagedTaskRecords" border class="management-table">
        <el-table-column label="任务ID" prop="id" width="120" />
        <el-table-column label="触发方式" width="120">
          <template #default="{ row }">
            {{ formatTriggerType(row.triggerType) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusTagType(row.status)">{{ crawlerTaskStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="采集范围" prop="scope" width="120" />
        <el-table-column label="来源标识" min-width="140" prop="source" />
        <el-table-column label="成功账号" min-width="150" prop="successAccount" />
        <el-table-column label="尝试次数" prop="attempts" width="110" />
        <el-table-column label="更新条数" prop="updatedCount" width="110" />
        <el-table-column label="开始时间" min-width="180" prop="startedAt" />
        <el-table-column label="结束时间" min-width="180" prop="finishedAt" />
        <el-table-column label="错误信息" min-width="220" prop="error" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button :icon="View" link type="primary" @click="openTaskDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="taskPagination.page"
          v-model:page-size="taskPagination.pageSize"
          :page-sizes="[6, 10, 20]"
          :total="taskRecords.length"
          background
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>

    <el-dialog v-model="accountDialogVisible" align-center title="教务账号" width="min(520px, calc(100vw - 32px))">
      <el-form label-position="top">
        <el-alert
          v-if="credential.configured"
          class="dialog-alert"
          :closable="false"
          show-icon
          title="编辑账号时需要重新输入完整账号和密码，原账号不会明文回显。"
          type="info"
        />
        <el-form-item label="教务账号" required>
          <el-input
            v-model="credentialForm.username"
            clearable
            :placeholder="credential.configured ? '请输入新账号；原账号只显示脱敏值' : '请输入教务系统账号'"
          />
        </el-form-item>
        <el-form-item label="教务密码" required>
          <el-input
            v-model="credentialForm.password"
            placeholder="保存时会加密写入数据库，不会明文存储"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="credentialForm.remark" clearable placeholder="例如：主采集账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button :loading="isSavingCredential" type="primary" @click="saveCredential">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="taskDetailVisible" align-center title="采集任务详情" width="min(760px, calc(100vw - 32px))">
      <template v-if="activeTask">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ activeTask.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ crawlerTaskStatusLabel(activeTask.status) }}</el-descriptions-item>
          <el-descriptions-item label="触发方式">{{ formatTriggerType(activeTask.triggerType) }}</el-descriptions-item>
          <el-descriptions-item label="成功账号">{{ activeTask.successAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新条数">{{ activeTask.updatedCount }}</el-descriptions-item>
          <el-descriptions-item label="尝试次数">{{ activeTask.attempts }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ activeTask.startedAt }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ activeTask.finishedAt }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="2">{{ activeTask.error || '-' }}</el-descriptions-item>
        </el-descriptions>
        <pre class="task-output">{{ activeTask.output || '暂无采集输出' }}</pre>
      </template>
      <template #footer>
        <el-button type="primary" @click="taskDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.class-timetable-page {
  display: grid;
  gap: 18px;
  min-height: 100%;
  padding: 2px 0 20px;
}

.page-head,
.card-header,
.page-actions,
.filter-buttons {
  display: flex;
  align-items: center;
}

.page-head {
  position: relative;
  overflow: hidden;
  justify-content: space-between;
  gap: 24px;
  min-height: 140px;
  border: 1px solid rgba(59, 130, 246, 0.16);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(20, 184, 166, 0.08) 48%, rgba(255, 255, 255, 0.96)),
    var(--surface-color);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
  padding: 24px 28px;
}

.page-head::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: "";
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.08) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(90deg, #000 0%, rgba(0, 0, 0, 0.18) 68%, transparent 100%);
}

.page-head > * {
  position: relative;
  z-index: 1;
}

.page-head h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
}

.page-actions,
.filter-buttons {
  gap: 10px;
}

.page-actions {
  flex-shrink: 0;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: -46px;
  padding: 0 18px;
  position: relative;
  z-index: 2;
}

.status-card {
  position: relative;
  min-height: 112px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
  padding: 18px 18px 16px;
}

.status-card::after {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  content: "";
  background: radial-gradient(circle, rgba(59, 130, 246, 0.16), rgba(20, 184, 166, 0.06) 64%, transparent 65%);
}

.status-card span,
.note-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.status-card strong {
  display: block;
  margin-top: 10px;
  color: var(--text-primary);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.25;
}

.status-card small {
  display: block;
  margin-top: 10px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.panel-card {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  background: var(--surface-color);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.panel-card :deep(.el-card__header) {
  border-bottom-color: rgba(148, 163, 184, 0.2);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  padding: 18px 22px;
}

.panel-card :deep(.el-card__body) {
  padding: 22px;
}

.card-header {
  justify-content: space-between;
  gap: 16px;
}

.card-header strong {
  color: var(--text-primary);
  font-size: 17px;
}

.config-layout {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.config-note {
  min-height: 100%;
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 8px;
  background: linear-gradient(145deg, #eff6ff, #f8fafc 60%, #ffffff);
  padding: 20px;
}

.config-note h2 {
  margin: 12px 0 0;
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.25;
}

.config-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(190px, 1fr));
  gap: 16px 18px;
  align-items: end;
}

.config-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.config-form :deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-weight: 650;
}

.config-form :deep(.el-select),
.config-form :deep(.el-date-editor.el-input),
.config-form :deep(.el-input) {
  width: 100%;
}

.config-form :deep(.el-radio-group) {
  display: flex;
  width: 100%;
}

.config-form :deep(.el-radio-button) {
  flex: 1;
}

.config-form :deep(.el-radio-button__inner) {
  width: 100%;
}

.account-filter {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(220px, 260px) auto;
  align-items: end;
  gap: 18px;
  margin-bottom: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #f8fafc;
  padding: 16px;
}

.filter-item {
  display: grid;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.management-table {
  width: 100%;
}

.management-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.management-table :deep(.el-table__cell) {
  padding: 12px 0;
}

.table-alert,
.dialog-alert {
  margin-bottom: 16px;
}

.task-output {
  max-height: 280px;
  margin: 16px 0 0;
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 12px;
  background: #111827;
  color: #d1d5db;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .status-grid,
  .config-layout,
  .account-filter {
    grid-template-columns: 1fr;
  }

  .config-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }

  .status-grid {
    margin-top: 0;
    padding: 0;
  }
}

@media (max-width: 720px) {
  .class-timetable-page {
    gap: 14px;
  }

  .page-head,
  .card-header,
  .page-actions,
  .filter-buttons {
    align-items: stretch;
    flex-direction: column;
  }

  .config-form {
    grid-template-columns: 1fr;
  }

  .page-head {
    min-height: 0;
    padding: 20px;
  }

  .page-head h1 {
    font-size: 24px;
  }
}
</style>
