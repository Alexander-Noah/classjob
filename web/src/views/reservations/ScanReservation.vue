<!--
  Web 管理平台的实验室预约视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Calendar, Check, Clock, InfoFilled, Location, OfficeBuilding, Phone, Refresh, Tickets, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getStoredUser, isAuthenticated } from '../../api/services/auth'
import { getScanReservationProfile, getScanReservationStatus, submitScanReservation } from '../../api/services/scanReservation'
import { getAcademicWeek, loadTeachingCalendar } from '../../modules/teaching-calendar/teachingCalendar'
import { createContactValidator, createMeaningfulTextValidator, getContactError, normalizeInputText } from '../../utils/reservationValidation'

defineOptions({
  name: 'ScanReservation',
})

const route = useRoute()
const formRef = ref()
const lab = ref(null)
const loading = ref(false)
const submitting = ref(false)
const statusLoading = ref(false)
const submittedReservation = ref(null)
const statusContact = ref('')
const loadError = ref('')
let statusPollingTimer = null

const sceneOptions = ['课程', '自主', '考试', '竞赛']
const statusStorageKey = 'smart-lab:scan-reservation:last-status'
const teachingCalendar = ref(null)
const weekdayNames = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const quickSlots = [
  { label: '第1节', period: '上午', value: '08:30-10:00' },
  { label: '第2节', period: '上午', value: '10:20-11:50' },
  { label: '第3节', period: '下午', value: '14:00-15:30' },
  { label: '第4节', period: '下午', value: '15:50-17:20' },
  { label: '第5节', period: '晚上', value: '18:40-20:10' },
  { label: '第6节', period: '晚上', value: '20:20-21:50' },
]

const formModel = reactive({
  applicant: '',
  department: '',
  contact: '',
  scene: '自主',
  date: '',
  timeRange: '',
  attendees: 1,
  reason: '',
  note: '',
  confirmed: false,
})

const queryModel = reactive({
  reservationId: '',
  contact: '',
})

const timeRangeModel = computed({
  /**
   * 获取当前操作。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  get() {
    const [startTime, endTime] = String(formModel.timeRange || '').split('-')
    return startTime && endTime ? [startTime, endTime] : []
  },
  /**
   * 设置当前操作。
   * 入参：value（值）。
   */
  set(value) {
    formModel.timeRange = Array.isArray(value) && value[0] && value[1] ? `${value[0]}-${value[1]}` : ''
  },
})

const storedUser = computed(/** 计算派生状态。 */ () => getStoredUser() || {})
const attendeeMax = computed(/** 计算派生状态。 */ () => Number(lab.value?.capacity) || 120)

const today = computed(/** 计算派生状态。 */ () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
})

/**
 * 解析时间目标Minutes。
 * 入参：timeText（时间文本）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseTimeToMinutes = (timeText) => {
  const match = String(timeText || '').match(/^(\d{1,2}):(\d{2})$/)
  if (!match) return null
  return Number(match[1]) * 60 + Number(match[2])
}

/**
 * 解析时间范围。
 * 入参：timeRange（时间区间）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseTimeRange = (timeRange) => {
  const [startTime, endTime] = String(timeRange || '').split('-')
  const start = parseTimeToMinutes(startTime)
  const end = parseTimeToMinutes(endTime)
  if (start === null || end === null || end <= start) return null
  return { start, end, startTime, endTime }
}

/**
 * 解析日期。
 * 入参：dateText（日期文本）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseDate = (dateText) => {
  if (!dateText) return null
  const date = new Date(`${dateText}T00:00:00`)
  return Number.isNaN(date.getTime()) ? null : date
}

const openRange = computed(/** 计算派生状态。 */ () => {
  const text = lab.value?.openTime || '08:00-22:00'
  return parseTimeRange(text) || { start: 8 * 60, end: 22 * 60, startTime: '08:00', endTime: '22:00' }
})

const openRangeLabel = computed(/** 计算派生状态。 */ () => `${openRange.value.startTime}-${openRange.value.endTime}`)

const labLocation = computed(/** 计算派生状态。 */ () =>
  [lab.value?.building, lab.value?.floor, lab.value?.room]
    .filter(Boolean)
    .join(' · ') || '位置待完善',
)

const labStatusType = computed(/** 计算派生状态。 */ () => {
  const status = String(lab.value?.status || '')
  if (status.includes('开放')) return 'success'
  if (status.includes('使用') || status.includes('预约')) return 'warning'
  if (status.includes('维护') || status.includes('停用')) return 'danger'
  return 'info'
})

const labFacts = computed(/** 计算派生状态。 */ () => [
  { label: '实验室编号', value: lab.value?.code || '-' },
  { label: '空间容量', value: `${attendeeMax.value} 人` },
  { label: '实验室类型', value: lab.value?.type || '基础机房' },
  { label: '负责人', value: lab.value?.manager || '管理员' },
  { label: '开放时段', value: openRangeLabel.value },
  { label: '所在位置', value: labLocation.value },
])

const selectedRange = computed(/** 计算派生状态。 */ () => parseTimeRange(formModel.timeRange))
const selectedDateMeta = computed(/** 计算派生状态。 */ () => {
  const date = parseDate(formModel.date)
  if (!date || !teachingCalendar.value) return null

  return {
    academicWeek: getAcademicWeek(teachingCalendar.value, date),
    dayText: `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`,
    weekday: weekdayNames[date.getDay()],
  }
})
const selectedSlot = computed(/** 计算派生状态。 */ () => quickSlots.find(/** 判断当前条目是否为查找目标。 入参：slot。 */ (slot) => slot.value === formModel.timeRange) || null)

const durationLabel = computed(/** 计算派生状态。 */ () => {
  const range = selectedRange.value
  if (!range) return ''
  const minutes = range.end - range.start
  if (minutes % 60 === 0) return `${minutes / 60} 小时`
  return minutes > 60 ? `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟` : `${minutes} 分钟`
})

const timeRangeMessage = computed(/** 计算派生状态。 */ () => {
  if (!formModel.timeRange) return ''

  const range = selectedRange.value
  if (!range) return '结束时间需晚于开始时间'

  if (formModel.date === today.value) {
    const now = new Date()
    const currentMinutes = now.getHours() * 60 + now.getMinutes()
    if (range.start < currentMinutes) return '预约开始时间不能早于当前时间'
  }

  if (range.start < openRange.value.start || range.end > openRange.value.end) {
    return `预约时段需在开放时段 ${openRangeLabel.value} 内`
  }

  if (range.end - range.start < 30) return '预约时长至少 30 分钟'

  return ''
})

const availabilityAlert = computed(/** 计算派生状态。 */ () => {
  if (!formModel.date || !formModel.timeRange) return null

  if (timeRangeMessage.value) {
    return {
      type: 'warning',
      title: timeRangeMessage.value,
    }
  }

  return {
    type: 'success',
    title: `已选择 ${formModel.date} ${selectedRange.value.startTime}-${selectedRange.value.endTime}，时长 ${durationLabel.value}`,
  }
})

const submittedSummary = computed(/** 计算派生状态。 */ () => {
  if (!submittedReservation.value) return []
  return [
    { label: '预约编号', value: submittedReservation.value.id || '-' },
    { label: '申请人', value: submittedReservation.value.applicant || formModel.applicant },
    { label: '实验室', value: submittedReservation.value.lab || lab.value?.displayName || lab.value?.name },
    { label: '预约时间', value: `${submittedReservation.value.date || formModel.date} ${submittedReservation.value.timeRange || formModel.timeRange}` },
    { label: '审核状态', value: submittedReservation.value.status || '待审核' },
  ]
})

const reservationIdText = computed(/** 计算派生状态。 */ () => String(submittedReservation.value?.id || '').trim())
const submittedStatus = computed(/** 计算派生状态。 */ () => submittedReservation.value?.status || '待审核')
const submittedStatusType = computed(/** 计算派生状态。 */ () => submittedReservation.value?.tagType || labStatusType.value || 'warning')
const statusStepActive = computed(/** 计算派生状态。 */ () => {
  if (submittedStatus.value === '已通过') return 3
  if (['已驳回', '已取消'].includes(submittedStatus.value)) return 2
  return 1
})
const statusAlert = computed(/** 计算派生状态。 */ () => {
  if (submittedStatus.value === '已通过') {
    return {
      type: 'success',
      title: '管理员已通过，预约已生效。',
      description: '实验室状态已同步更新，按预约时间到场使用即可。',
    }
  }

  if (submittedStatus.value === '已驳回') {
    return {
      type: 'error',
      title: '预约未通过，请查看审核备注。',
      description: submittedReservation.value?.note || '如需继续使用，请修改信息后重新提交。',
    }
  }

  if (submittedStatus.value === '已取消') {
    return {
      type: 'info',
      title: '预约已取消。',
      description: submittedReservation.value?.note || '该预约不再占用实验室。',
    }
  }

  return {
    type: 'warning',
    title: '预约正在等待管理员审核。',
    description: '页面会自动刷新审核状态，也可以手动点击刷新。',
  }
})

/**
 * 判断是否为WaitingForReview。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const isWaitingForReview = () => ['待审核', '冲突'].includes(submittedReservation.value?.status || '待审核')

/**
 * 读取Stored状态查询。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readStoredStatusQuery = () => {
  if (typeof window === 'undefined') return null

  try {
    const storedValue = JSON.parse(window.localStorage.getItem(statusStorageKey) || 'null')
    return storedValue && typeof storedValue === 'object' ? storedValue : null
  } catch {
    return null
  }
}

/**
 * remember状态查询的处理逻辑。
 * 入参：reservation（预约）。
 */
const rememberStatusQuery = (reservation = submittedReservation.value) => {
  if (typeof window === 'undefined' || !reservation?.id) return

  window.localStorage.setItem(
    statusStorageKey,
    JSON.stringify({
      labId: String(lab.value?.id || route.query.labId || ''),
      reservationId: reservation.id,
      contact: statusContact.value || formModel.contact.trim(),
    }),
  )
}

/**
 * 停止状态Polling。
 */
const stopStatusPolling = () => {
  if (!statusPollingTimer) return
  window.clearInterval(statusPollingTimer)
  statusPollingTimer = null
}

/**
 * 刷新Submitted状态。
 * 入参：解构参数（silent、false）。
 */
const refreshSubmittedStatus = async ({ silent = false } = {}) => {
  const reservationId = submittedReservation.value?.id
  const contact = statusContact.value || formModel.contact.trim() || submittedReservation.value?.contact

  if (!reservationId || !contact) return

  statusLoading.value = true
  try {
    const nextStatus = await getScanReservationStatus({ id: reservationId, contact })
    submittedReservation.value = {
      ...submittedReservation.value,
      ...nextStatus,
    }
    statusContact.value = contact
    rememberStatusQuery(submittedReservation.value)

    if (!isWaitingForReview()) {
      stopStatusPolling()
    }
  } catch (error) {
    if (!silent && !error?.__handled) {
      ElMessage.error(error?.message || '审核状态查询失败')
    }
  } finally {
    statusLoading.value = false
  }
}

/**
 * 启动状态Polling。
 */
const startStatusPolling = () => {
  stopStatusPolling()
  if (!submittedReservation.value?.id || !isWaitingForReview()) return

  statusPollingTimer = window.setInterval(/** 执行周期安排的任务。 */ () => {
    void refreshSubmittedStatus({ silent: true })
  }, 15000)
}

/**
 * 恢复Submitted预约。
 */
const restoreSubmittedReservation = () => {
  const storedStatus = readStoredStatusQuery()
  const currentLabId = String(lab.value?.id || route.query.labId || '')

  if (!storedStatus?.reservationId || !storedStatus?.contact || String(storedStatus.labId || '') !== currentLabId) {
    return
  }

  statusContact.value = storedStatus.contact
  submittedReservation.value = {
    id: storedStatus.reservationId,
    contact: storedStatus.contact,
    lab: lab.value?.displayName || [lab.value?.code, lab.value?.name].filter(Boolean).join(' '),
    status: '待审核',
    tagType: 'warning',
  }
  void refreshSubmittedStatus({ silent: true }).then(startStatusPolling)
}

/**
 * 应用Stored用户默认。
 */
const applyStoredUserDefaults = () => {
  const user = storedUser.value
  if (!formModel.applicant) {
    formModel.applicant = user.realName || user.name || user.username || ''
  }
  if (!formModel.department) {
    formModel.department = user.department || user.roleName || ''
  }
  if (!formModel.contact) {
    formModel.contact = user.phone || user.email || ''
  }
}

/**
 * 校验时间范围。
 * 入参：_rule（规则）；value（值）；callback（回调）。
 */
const validateTimeRange = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择预约时段'))
    return
  }

  const message = timeRangeMessage.value
  if (message) {
    callback(new Error(message))
    return
  }

  callback()
}

/**
 * 校验Scene。
 * 入参：_rule（规则）；value（值）；callback（回调）。
 */
const validateScene = (_rule, value, callback) => {
  if (!sceneOptions.includes(value)) {
    callback(new Error('请选择预约类型'))
    return
  }

  callback()
}

/**
 * 校验Attendees。
 * 入参：_rule（规则）；value（值）；callback（回调）。
 */
const validateAttendees = (_rule, value, callback) => {
  const count = Number(value)

  if (!Number.isInteger(count) || count < 1) {
    callback(new Error('预约人数至少为 1 人'))
    return
  }

  if (attendeeMax.value && count > attendeeMax.value) {
    callback(new Error(`预约人数不能超过 ${attendeeMax.value} 人`))
    return
  }

  callback()
}

/**
 * 校验Confirmed。
 * 入参：_rule（规则）；value（值）；callback（回调）。
 */
const validateConfirmed = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认预约信息真实有效'))
    return
  }

  callback()
}

const rules = {
  applicant: [{ validator: createMeaningfulTextValidator({ label: '申请人姓名', min: 2, max: 50 }), trigger: 'blur' }],
  department: [{ validator: createMeaningfulTextValidator({ label: '学院、班级或组织', min: 2, max: 80 }), trigger: 'blur' }],
  contact: [{ validator: createContactValidator({ label: '联系方式', strict: true }), trigger: 'blur' }],
  scene: [{ validator: validateScene, trigger: 'change' }],
  date: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  timeRange: [{ validator: validateTimeRange, trigger: 'change' }],
  attendees: [{ validator: validateAttendees, trigger: 'change' }],
  reason: [{ validator: createMeaningfulTextValidator({ label: '预约用途', min: 4, max: 160 }), trigger: 'blur' }],
  note: [{ validator: createMeaningfulTextValidator({ label: '补充说明', min: 4, max: 100, required: false }), trigger: 'blur' }],
  confirmed: [{ validator: validateConfirmed, trigger: 'change' }],
}

/**
 * 加载实验室Profile。
 */
const loadLabProfile = async () => {
  const labId = route.query.labId
  const code = route.query.code

  if (!labId) {
    loadError.value = '预约二维码缺少实验室信息'
    return
  }

  loading.value = true
  loadError.value = ''
  applyStoredUserDefaults()

  try {
    lab.value = await getScanReservationProfile({ labId, code })
    formModel.attendees = 1
    restoreSubmittedReservation()
  } catch (error) {
    loadError.value = error?.message || '实验室信息加载失败'
  } finally {
    loading.value = false
  }
}

/**
 * 设置QuickSlot。
 * 入参：slot（slot）。
 */
const setQuickSlot = (slot) => {
  formModel.timeRange = slot.value
  formRef.value?.validateField('timeRange')
}

/**
 * 重置预约Fields。
 */
const resetReservationFields = () => {
  formModel.date = ''
  formModel.timeRange = ''
  formModel.attendees = 1
  formModel.reason = ''
  formModel.note = ''
  formModel.confirmed = false
  formRef.value?.clearValidate()
}

/**
 * 处理Continue。
 */
const handleContinue = () => {
  stopStatusPolling()
  submittedReservation.value = null
  resetReservationFields()
}

/**
 * 处理状态Lookup。
 */
const handleStatusLookup = async () => {
  const reservationId = String(queryModel.reservationId || '').trim().replace(/^#/, '')
  const contact = normalizeInputText(queryModel.contact)

  if (!/^\d+$/.test(reservationId)) {
    ElMessage.warning('请填写数字预约编号')
    return
  }

  if (!contact) {
    ElMessage.warning('请填写提交时的联系方式')
    return
  }

  const contactError = getContactError(contact, { label: '联系方式', strict: true })
  if (contactError) {
    ElMessage.warning(contactError)
    return
  }

  statusLoading.value = true
  try {
    submittedReservation.value = await getScanReservationStatus({ id: reservationId, contact })
    statusContact.value = contact
    rememberStatusQuery(submittedReservation.value)
    startStatusPolling()
    ElMessage.success('已查询到预约状态')
  } catch (error) {
    if (!error?.__handled) {
      ElMessage.error(error?.message || '预约状态查询失败')
    }
  } finally {
    statusLoading.value = false
  }
}

/**
 * 复制预约标识。
 */
const copyReservationId = async () => {
  if (!reservationIdText.value) {
    ElMessage.warning('暂无预约编号')
    return
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(reservationIdText.value)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = reservationIdText.value
      textarea.setAttribute('readonly', 'readonly')
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('预约编号已复制')
  } catch {
    ElMessage.warning(`请手动保存预约编号：${reservationIdText.value}`)
  }
}

/**
 * 提交Form。
 */
const submitForm = async () => {
  if (!lab.value || !formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const submittedContact = normalizeInputText(formModel.contact)
    submittedReservation.value = await submitScanReservation({
      applicant: normalizeInputText(formModel.applicant),
      department: normalizeInputText(formModel.department),
      contact: submittedContact,
      lab: lab.value.displayName || [lab.value.code, lab.value.name].filter(Boolean).join(' '),
      labId: lab.value.id,
      labCode: lab.value.code,
      date: formModel.date,
      timeRange: formModel.timeRange,
      scene: formModel.scene,
      attendees: Number(formModel.attendees) || 1,
      reason: normalizeInputText(formModel.reason),
      note: normalizeInputText(formModel.note) || '扫码提交，待管理员审核。',
    })
    statusContact.value = submittedContact
    rememberStatusQuery(submittedReservation.value)
    startStatusPolling()
    ElMessage.success('预约已提交，等待管理员审核')
  } catch (error) {
    if (!error?.__handled) {
      ElMessage.error(error?.message || '预约提交失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(/** 在组件挂载后初始化相关状态。 */ async () => {
  await loadLabProfile()

  if (isAuthenticated()) {
    try {
      teachingCalendar.value = await loadTeachingCalendar()
    } catch (error) {
      ElMessage.warning(error?.message || '教学日历加载失败')
    }
  }
})
onBeforeUnmount(stopStatusPolling)
</script>

<template>
  <main class="scan-reservation-page">
    <section class="scan-workspace">
      <aside class="lab-panel">
        <div class="brand-lockup">
          <span class="brand-icon" aria-hidden="true">
            <el-icon><OfficeBuilding /></el-icon>
          </span>
          <div>
            <span class="eyebrow">SMART LAB RESERVATION</span>
            <h1>实验室预约</h1>
          </div>
        </div>

        <el-skeleton v-if="loading" :rows="7" animated />

        <el-alert v-else-if="loadError" :closable="false" show-icon :title="loadError" type="error" />

        <template v-else-if="lab">
          <div class="lab-title-block">
            <span>{{ lab.code }}</span>
            <h2>{{ lab.name }}</h2>
            <p>
              <el-icon><Location /></el-icon>
              {{ labLocation }}
            </p>
            <el-tag :type="labStatusType" effect="light">{{ lab.status || '待确认' }}</el-tag>
          </div>

          <dl class="lab-facts">
            <div v-for="item in labFacts" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>

          <div class="audit-flow">
            <div>
              <strong>1</strong>
              <span>填写申请</span>
            </div>
            <div>
              <strong>2</strong>
              <span>管理员审核</span>
            </div>
            <div>
              <strong>3</strong>
              <span>通过后使用</span>
            </div>
          </div>

          <div class="lab-note">
            <el-icon><InfoFilled /></el-icon>
            <span>提交后将进入“预约管理”待审核列表，系统会在提交时校验课表占用、已有预约和容量限制。</span>
          </div>
        </template>
      </aside>

      <section class="form-panel">
        <header class="form-header">
          <div>
            <span class="eyebrow">SCAN ENTRY</span>
            <h2>预约申请单</h2>
          </div>
          <el-tag effect="plain" type="info">扫码入口</el-tag>
        </header>

        <el-skeleton v-if="loading" :rows="10" animated />

        <el-alert v-else-if="loadError" :closable="false" show-icon title="无法打开预约单，请重新扫描二维码或联系管理员。" type="error" />

        <template v-else-if="lab">
          <div v-if="!submittedReservation" class="lookup-card">
            <div class="lookup-card__copy">
              <strong>查询审核结果</strong>
              <span>提交后可用预约编号和联系方式查看待审核、已通过或已驳回原因。</span>
            </div>
            <div class="lookup-card__form">
              <el-input v-model="queryModel.reservationId" clearable inputmode="numeric" placeholder="预约编号">
                <template #prefix>#</template>
              </el-input>
              <el-input v-model="queryModel.contact" clearable placeholder="提交时的联系方式">
                <template #prefix>
                  <el-icon><Phone /></el-icon>
                </template>
              </el-input>
              <el-button :icon="Refresh" :loading="statusLoading" @click="handleStatusLookup">查询</el-button>
            </div>
          </div>

          <el-result
            v-if="submittedReservation"
            class="submit-result"
            icon="success"
            title="预约已提交"
            sub-title="管理员审核通过后，实验室预约才会正式生效。"
          >
            <template #extra>
              <div class="reservation-ticket">
                <span>请保存预约编号，用于后续查询审核结果</span>
                <strong>{{ reservationIdText || '-' }}</strong>
                <el-button type="primary" plain :icon="Tickets" @click="copyReservationId">复制编号</el-button>
              </div>

              <div class="status-board">
                <div class="status-board__head">
                  <span>审核状态</span>
                  <el-tag :type="submittedStatusType" effect="light">{{ submittedStatus }}</el-tag>
                </div>
                <el-steps :active="statusStepActive" align-center finish-status="success" class="status-steps">
                  <el-step title="已提交" />
                  <el-step title="审核中" />
                  <el-step title="已通过" />
                </el-steps>
                <el-alert
                  :closable="false"
                  show-icon
                  :description="statusAlert.description"
                  :title="statusAlert.title"
                  :type="statusAlert.type"
                />
              </div>
              <dl class="submitted-summary">
                <div v-for="item in submittedSummary" :key="item.label">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                </div>
              </dl>
              <el-space wrap class="result-actions">
                <el-button :icon="Refresh" :loading="statusLoading" @click="refreshSubmittedStatus()">刷新状态</el-button>
                <el-button type="primary" :icon="Check" @click="handleContinue">继续预约</el-button>
              </el-space>
            </template>
          </el-result>

          <el-form v-else ref="formRef" :model="formModel" :rules="rules" label-position="top" class="scan-form">
            <div class="form-section-title">
              <el-icon><User /></el-icon>
              <span>申请信息</span>
            </div>

            <div class="form-grid two-columns">
              <el-form-item label="申请人" prop="applicant">
                <el-input v-model="formModel.applicant" clearable placeholder="姓名、班级或团队名称" />
              </el-form-item>

              <el-form-item label="学院/班级/组织" prop="department">
                <el-input v-model="formModel.department" clearable placeholder="例如：软件学院 2 班" />
              </el-form-item>

              <el-form-item label="联系方式" prop="contact">
                <el-input v-model="formModel.contact" clearable placeholder="手机号或邮箱">
                  <template #prefix>
                    <el-icon><Phone /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="预约类型" prop="scene">
                <el-select v-model="formModel.scene" class="full-field" placeholder="请选择预约类型">
                  <el-option v-for="scene in sceneOptions" :key="scene" :label="scene" :value="scene" />
                </el-select>
              </el-form-item>
            </div>

            <div class="form-section-title">
              <el-icon><Calendar /></el-icon>
              <span>使用安排</span>
            </div>

            <div class="form-grid three-columns">
              <el-form-item label="预约日期" prop="date">
                <el-date-picker
                  v-model="formModel.date"
                  class="full-field"
                  clearable
                  format="YYYY-MM-DD"
                  :disabled-date="/**
 * 处理当前调用传入的回调逻辑。
 * 入参：date。
 */
(date) => date.getTime() < new Date(`${today}T00:00:00`).getTime()"
                  placeholder="请选择日期"
                  type="date"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>

              <el-form-item label="自定义时段" prop="timeRange">
                <el-time-picker
                  v-model="timeRangeModel"
                  class="full-field"
                  clearable
                  end-placeholder="结束时间"
                  format="HH:mm"
                  is-range
                  range-separator="至"
                  start-placeholder="开始时间"
                  value-format="HH:mm"
                  @change="formRef?.validateField('timeRange')"
                />
              </el-form-item>

              <el-form-item label="使用人数" prop="attendees">
                <el-input-number
                  v-model="formModel.attendees"
                  class="full-field"
                  :max="attendeeMax"
                  :min="1"
                  :precision="0"
                  :step="1"
                  controls-position="right"
                />
              </el-form-item>
            </div>

            <div v-if="selectedDateMeta" class="date-meta">
              <span>{{ selectedDateMeta.dayText }}</span>
              <strong>第{{ selectedDateMeta.academicWeek }}周 · {{ selectedDateMeta.weekday }}</strong>
            </div>

            <div class="period-picker" aria-label="按节次选择预约时段">
              <button
                v-for="slot in quickSlots"
                :key="slot.value"
                class="period-card"
                :class="{ active: formModel.timeRange === slot.value }"
                type="button"
                @click="setQuickSlot(slot)"
              >
                <span>{{ slot.period }}</span>
                <strong>{{ slot.label }}</strong>
                <em>{{ slot.value }}</em>
              </button>
            </div>

            <el-alert
              v-if="availabilityAlert"
              class="availability-alert"
              :closable="false"
              show-icon
              :title="availabilityAlert.title"
              :type="availabilityAlert.type"
            />

            <el-form-item label="预约用途" prop="reason">
              <el-input
                v-model="formModel.reason"
                :rows="4"
                maxlength="160"
                placeholder="填写课程、自主实验、考试、竞赛或活动说明"
                show-word-limit
                type="textarea"
              />
            </el-form-item>

            <el-form-item label="补充说明" prop="note">
              <el-input
                v-model="formModel.note"
                :rows="2"
                maxlength="100"
                placeholder="如需软件环境、设备支持或特殊安排，可在此说明"
                show-word-limit
                type="textarea"
              />
            </el-form-item>

            <el-form-item class="confirm-item" prop="confirmed">
              <el-checkbox v-model="formModel.confirmed">
                我确认预约信息真实有效，并了解提交后需等待管理员审核。
              </el-checkbox>
            </el-form-item>

            <div class="submit-bar">
              <div>
                <strong>{{ lab.name }}</strong>
                <span>
                  {{ selectedSlot ? `${selectedSlot.label} · ${selectedSlot.value}` : `${attendeeMax} 人容量 · ${openRangeLabel} 开放` }}
                </span>
              </div>
              <el-button class="submit-button" type="primary" :icon="Tickets" :loading="submitting" @click="submitForm">
                提交预约
              </el-button>
            </div>
          </el-form>
        </template>
      </section>
    </section>
  </main>
</template>

<style scoped>
.scan-reservation-page {
  min-height: 100vh;
  padding: 32px 20px;
  background:
    linear-gradient(180deg, rgb(239 246 255 / 72%) 0%, rgb(248 250 252 / 96%) 48%, #eef2f7 100%);
}

.scan-workspace {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 680px);
  gap: 18px;
  width: min(100%, 1060px);
  margin: 0 auto;
  align-items: start;
}

.lab-panel,
.form-panel {
  border: 1px solid #dce6f2;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgb(15 23 42 / 8%);
}

.lab-panel {
  position: sticky;
  top: 24px;
  display: grid;
  gap: 16px;
  padding: 20px;
}

.form-panel {
  display: grid;
  gap: 18px;
  padding: 22px;
}

.form-panel :deep(.el-date-editor),
.form-panel :deep(.el-select),
.form-panel :deep(.el-input-number) {
  width: 100%;
}

.form-panel :deep(.el-input__wrapper),
.form-panel :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.brand-lockup,
.form-header,
.lab-title-block p,
.form-section-title,
.submit-bar,
.lab-note {
  display: flex;
  align-items: center;
}

.brand-lockup,
.form-header,
.submit-bar {
  justify-content: space-between;
  gap: 16px;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  color: #2563eb;
  background: #eff6ff;
  font-size: 24px;
}

.brand-lockup {
  justify-content: flex-start;
}

.eyebrow {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.brand-lockup h1,
.form-header h2,
.lab-title-block h2 {
  margin: 0;
  color: #0f172a;
  font-weight: 800;
  letter-spacing: 0;
}

.brand-lockup h1 {
  font-size: 24px;
}

.form-header {
  padding-bottom: 16px;
  border-bottom: 1px solid #e5edf6;
}

.form-header h2 {
  margin-top: 4px;
  font-size: 24px;
}

.form-header p,
.lab-title-block span,
.lab-title-block p,
.lab-facts dt,
.submit-bar span,
.lab-note {
  color: #64748b;
  font-size: 13px;
}

.form-header p {
  margin: 6px 0 0;
}

.lab-title-block {
  position: relative;
  display: grid;
  gap: 7px;
  padding: 16px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.lab-title-block h2 {
  font-size: 20px;
}

.lab-title-block p {
  gap: 6px;
  margin: 0;
}

.lab-title-block .el-tag {
  position: absolute;
  top: 14px;
  right: 14px;
}

.lab-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.lab-facts div {
  min-width: 0;
  padding: 12px;
  border: 1px solid #e7eef7;
  border-radius: 8px;
  background: #fbfdff;
}

.lab-facts dt {
  margin-bottom: 7px;
}

.lab-facts dd {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  word-break: break-word;
}

.audit-flow {
  display: grid;
  gap: 10px;
}

.audit-flow div {
  display: grid;
  grid-template-columns: 28px 1fr;
  align-items: center;
  gap: 10px;
  color: #334155;
  font-size: 13px;
}

.audit-flow strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 13px;
}

.lab-note {
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #fefce8;
  color: #854d0e;
  line-height: 1.55;
}

.scan-form {
  display: grid;
  gap: 10px;
}

.lookup-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.lookup-card__copy {
  display: grid;
  gap: 4px;
}

.lookup-card__copy strong {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
}

.lookup-card__copy span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.55;
}

.lookup-card__form {
  display: grid;
  grid-template-columns: minmax(110px, 0.8fr) minmax(160px, 1fr) auto;
  gap: 10px;
}

.form-section-title {
  gap: 8px;
  margin-top: 2px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
}

.form-section-title :deep(.el-icon) {
  color: #2563eb;
}

.form-grid {
  display: grid;
  gap: 14px;
}

.two-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.three-columns {
  grid-template-columns: minmax(0, 1fr) minmax(210px, 1.25fr) minmax(130px, 0.7fr);
}

.full-field,
.submit-button {
  width: 100%;
}

.date-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.date-meta span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.date-meta strong {
  color: #0f172a;
  font-size: 14px;
}

.period-picker {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  padding: 2px 0 6px;
}

.period-card {
  min-width: 0;
  min-height: 76px;
  padding: 10px 8px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
  color: #475569;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.period-card span,
.period-card em {
  display: block;
  font-style: normal;
  line-height: 1.35;
}

.period-card span {
  color: #64748b;
  font-size: 12px;
}

.period-card strong {
  display: block;
  margin: 4px 0;
  color: #0f172a;
  font-size: 15px;
}

.period-card em {
  color: #64748b;
  font-size: 12px;
}

.period-card:hover,
.period-card.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 8px 18px rgb(37 99 235 / 12%);
}

.period-card.active span,
.period-card.active strong,
.period-card.active em {
  color: #1d4ed8;
}

.availability-alert {
  margin: 0 0 4px;
}

.confirm-item {
  margin-bottom: 4px;
}

.confirm-item :deep(.el-checkbox) {
  align-items: flex-start;
  height: auto;
  white-space: normal;
}

.confirm-item :deep(.el-checkbox__label) {
  color: #475569;
  line-height: 1.55;
  white-space: normal;
}

.submit-bar {
  margin-top: 2px;
  padding: 14px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.submit-bar div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.submit-bar strong {
  color: #0f172a;
  font-size: 14px;
}

.submit-button {
  max-width: 180px;
  height: 42px;
}

.submit-result {
  padding: 8px 0 0;
}

.reservation-ticket {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 14px;
  align-items: center;
  width: min(100%, 520px);
  margin: 0 auto 16px;
  padding: 16px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  text-align: left;
}

.reservation-ticket span {
  grid-column: 1 / -1;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.reservation-ticket strong {
  color: #0f172a;
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
}

.status-board {
  display: grid;
  gap: 14px;
  width: min(100%, 520px);
  margin: 0 auto 16px;
  padding: 14px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
  text-align: left;
}

.status-board__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.status-steps {
  padding: 2px 0;
}

.status-board :deep(.el-step__title) {
  font-size: 12px;
}

.submitted-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: min(100%, 520px);
  margin: 0 auto 18px;
  text-align: left;
}

.submitted-summary div {
  padding: 11px 12px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: #f8fbff;
}

.submitted-summary dt {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.submitted-summary dd {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  word-break: break-word;
}

.result-actions {
  justify-content: center;
}

@media (max-width: 900px) {
  .scan-reservation-page {
    padding: 16px;
  }

  .scan-workspace,
  .two-columns,
  .three-columns {
    grid-template-columns: 1fr;
  }

  .lab-panel {
    position: static;
  }
}

@media (max-width: 560px) {
  .scan-reservation-page {
    padding: 0;
    background: #ffffff;
  }

  .scan-workspace {
    gap: 0;
  }

  .lab-panel,
  .form-panel {
    border-right: 0;
    border-left: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .lab-panel {
    gap: 12px;
    padding: 16px;
    border-bottom: 8px solid #eef2f7;
  }

  .form-panel {
    gap: 14px;
    padding: 16px 16px 84px;
  }

  .form-header,
  .submit-bar,
  .brand-lockup {
    align-items: flex-start;
    flex-direction: column;
  }

  .brand-lockup {
    gap: 10px;
  }

  .brand-icon {
    width: 42px;
    height: 42px;
    font-size: 21px;
  }

  .brand-lockup h1,
  .form-header h2 {
    font-size: 21px;
  }

  .form-header {
    gap: 10px;
    padding-bottom: 12px;
  }

  .form-header p {
    margin-top: 4px;
    line-height: 1.55;
  }

  .lab-title-block {
    padding: 14px;
  }

  .lab-title-block h2 {
    padding-right: 72px;
    font-size: 18px;
  }

  .lab-facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .lab-facts div {
    padding: 10px;
  }

  .lab-facts dd {
    font-size: 13px;
  }

  .audit-flow {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .audit-flow div {
    grid-template-columns: 1fr;
    justify-items: center;
    gap: 6px;
    padding: 10px 6px;
    border: 1px solid #dbe7f5;
    border-radius: 8px;
    text-align: center;
  }

  .form-grid {
    gap: 10px;
  }

  .lookup-card__form {
    grid-template-columns: 1fr;
  }

  .form-section-title {
    margin-top: 4px;
  }

  .period-picker {
    display: flex;
    margin: 0 -16px;
    padding: 0 16px 8px;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .period-picker::-webkit-scrollbar {
    display: none;
  }

  .period-card {
    flex: 0 0 132px;
  }

  .submit-bar {
    position: sticky;
    bottom: 0;
    z-index: 8;
    margin: 4px -16px -84px;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
    border-right: 0;
    border-bottom: 0;
    border-left: 0;
    border-radius: 0;
    background: rgb(255 255 255 / 96%);
    box-shadow: 0 -10px 30px rgb(15 23 42 / 10%);
    backdrop-filter: blur(12px);
  }

  .submit-button {
    max-width: none;
  }

  .submitted-summary {
    grid-template-columns: 1fr;
  }

  .reservation-ticket {
    grid-template-columns: 1fr;
  }

  .reservation-ticket strong {
    font-size: 30px;
  }

  .status-board {
    padding: 12px;
  }
}

.scan-reservation-page {
  color: #0f172a;
  background:
    linear-gradient(180deg, #f8fbff 0%, #f3f6fb 42%, #edf2f8 100%);
}

.scan-workspace {
  grid-template-columns: minmax(300px, 370px) minmax(0, 760px);
  gap: 20px;
  width: min(100%, 1180px);
}

.lab-panel,
.form-panel {
  border-color: #dbe7f5;
  box-shadow: none;
}

.lab-panel {
  gap: 18px;
  background:
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.form-panel {
  background: #ffffff;
}

.brand-icon {
  background: #eaf2ff;
}

.eyebrow {
  color: #1d4ed8;
}

.form-header {
  align-items: flex-start;
}

.form-header h2 {
  font-size: 25px;
}

.form-header p {
  max-width: 62ch;
  color: #475569;
}

.lab-title-block,
.lookup-card,
.date-meta,
.submit-bar,
.status-board {
  border-color: #dbe7f5;
  background: #f8fbff;
}

.lab-facts div,
.submitted-summary div,
.kpi-card {
  border-color: #e5edf6;
}

.lab-facts dd,
.lookup-card__copy strong,
.submit-bar strong,
.submitted-summary dd,
.status-board__head,
.date-meta strong {
  color: #0f172a;
}

.form-panel :deep(.el-input__wrapper),
.form-panel :deep(.el-textarea__inner),
.form-panel :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

.form-panel :deep(.el-input__wrapper:hover),
.form-panel :deep(.el-select__wrapper:hover),
.form-panel :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.form-panel :deep(.el-input__wrapper.is-focus),
.form-panel :deep(.el-select__wrapper.is-focused),
.form-panel :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #2563eb inset;
}

.form-section-title {
  margin-top: 6px;
  padding-top: 4px;
}

.period-picker {
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
}

.period-card {
  min-height: 82px;
  border-color: #dbe7f5;
  background: #ffffff;
  box-shadow: none;
}

.period-card:hover,
.period-card.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: none;
}

.lookup-card__form {
  grid-template-columns: minmax(120px, 0.85fr) minmax(180px, 1fr) auto;
}

.submit-bar {
  position: sticky;
  bottom: 16px;
  z-index: 6;
}

.submit-button {
  border-radius: 8px;
  font-weight: 700;
}

.reservation-ticket {
  border-color: #bfdbfe;
  background: #eff6ff;
}

@media (max-width: 900px) {
  .scan-workspace {
    grid-template-columns: 1fr;
    width: min(100%, 720px);
  }

  .submit-bar {
    position: static;
  }
}

@media (max-width: 560px) {
  .scan-reservation-page {
    background: #ffffff;
  }

  .scan-workspace {
    width: 100%;
  }

  .lab-panel,
  .form-panel {
    border-color: #e5edf6;
  }

  .period-picker {
    display: flex;
  }

  .submit-bar {
    position: sticky;
    bottom: 0;
  }
}
</style>
