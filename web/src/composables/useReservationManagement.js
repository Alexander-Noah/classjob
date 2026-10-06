import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { labResourceService, reservationResourceService } from '../api/services/basicResources'
import { API_USE_MOCK } from '../api/config'
import { applyAuthoritativeReservationRecord } from '../api/services/authoritativeRecord'
import { getClassTimetableSemesters, listClassTimetables } from '../api/services/classTimetable'
import { exportTableToExcel } from '../utils/exportExcel'
import { formatLabDisplayName } from '../utils/labDisplay'
import { resolveReservationLabId } from '../utils/reservationLabId'
import { getContactError, getMeaningfulTextError, normalizeInputText } from '../utils/reservationValidation'
import {
  RESERVATION_STATUS,
  getReservationActions,
  getReservationStatusType,
  isReservationExpired,
  normalizeReservationForDisplay,
  normalizeReservationStatus,
  reservationStatusOptions,
} from '../views/reservations/reservationRules'

const statusOptions = reservationStatusOptions

const sceneOptions = ['课程', '自主', '考试', '竞赛']
const fallbackLabs = [
  { code: 'A101', name: 'A101 云计算实验室', capacity: 72 },
  { code: 'B203', name: 'B203 网络安全实验室', capacity: 56 },
  { code: 'C305', name: 'C305 人工智能实验室', capacity: 64 },
  { code: 'D407', name: 'D407 物联网实验室', capacity: 48 },
]
const fallbackLabOptions = fallbackLabs.map((lab) => formatLabDisplayName(lab))
const semesterStartDate = '2026-03-02'

const weekdayOrder = {
  星期一: 1,
  星期二: 2,
  星期三: 3,
  星期四: 4,
  星期五: 5,
  星期六: 6,
  星期日: 7,
  周一: 1,
  周二: 2,
  周三: 3,
  周四: 4,
  周五: 5,
  周六: 6,
  周日: 7,
}

const sectionTimeRanges = {
  1: { start: 8 * 60 + 30, end: 10 * 60 },
  2: { start: 10 * 60 + 20, end: 11 * 60 + 50 },
  3: { start: 14 * 60, end: 15 * 60 + 30 },
  4: { start: 15 * 60 + 50, end: 17 * 60 + 20 },
  5: { start: 18 * 60 + 40, end: 20 * 60 + 10 },
  6: { start: 20 * 60 + 20, end: 21 * 60 + 50 },
}

const initialReservations = [
  {
    id: 'RSV-20260423-001',
    applicant: '软件工程 2 班',
    department: '软件学院',
    contact: '刘老师',
    lab: 'A101 云计算实验室',
    date: '2026-04-23',
    timeRange: '08:00-10:00',
    scene: '课程',
    attendees: 68,
    reason: '云计算平台实训课程',
    status: '已通过',
    tagType: 'success',
    conflict: false,
    reviewer: '张明',
    submittedAt: '04-22 16:10',
    reviewedAt: '04-22 17:30',
    note: '课程预约，门禁已同步放行。',
  },
  {
    id: 'RSV-20260423-002',
    applicant: '李同学',
    department: '人工智能协会',
    contact: '188****0321',
    lab: 'C305 人工智能实验室',
    date: '2026-04-23',
    timeRange: '14:00-17:00',
    scene: '自主',
    attendees: 6,
    reason: '模型训练与竞赛调试',
    status: '待审核',
    tagType: 'warning',
    conflict: false,
    reviewer: '待审核',
    submittedAt: '04-23 09:12',
    reviewedAt: '-',
    note: '需确认 GPU 服务器空闲情况。',
  },
  {
    id: 'RSV-20260423-003',
    applicant: '数据科学 1 班',
    department: '大数据学院',
    contact: '周老师',
    lab: 'B203 网络安全实验室',
    date: '2026-04-24',
    timeRange: '10:00-12:00',
    scene: '课程',
    attendees: 54,
    reason: '网络安全攻防实训',
    status: '冲突',
    tagType: 'danger',
    conflict: true,
    reviewer: '系统预警',
    submittedAt: '04-23 10:20',
    reviewedAt: '-',
    note: '与学院考试占用时段重叠。',
  },
  {
    id: 'RSV-20260423-004',
    applicant: '电子信息 3 班',
    department: '信息工程学院',
    contact: '陈老师',
    lab: 'B203 网络安全实验室',
    date: '2026-04-24',
    timeRange: '10:00-12:00',
    scene: '考试',
    attendees: 48,
    reason: '期中上机考试',
    status: '待审核',
    tagType: 'warning',
    conflict: true,
    reviewer: '待审核',
    submittedAt: '04-23 11:02',
    reviewedAt: '-',
    note: '需机房管理员确认冲突优先级。',
  },
  {
    id: 'RSV-20260423-005',
    applicant: '创新创业团队',
    department: '创新学院',
    contact: '王同学',
    lab: 'D407 物联网实验室',
    date: '2026-04-25',
    timeRange: '19:00-21:00',
    scene: '竞赛',
    attendees: 12,
    reason: '物联网挑战赛赛前联调',
    status: '待审核',
    tagType: 'warning',
    conflict: false,
    reviewer: '待审核',
    submittedAt: '04-23 13:46',
    reviewedAt: '-',
    note: '晚间预约，需确认值班人员。',
  },
]

const createReservationId = () => `RSV-${new Date().toISOString().slice(0, 10).replaceAll('-', '')}-${Math.random().toString(36).slice(2, 6).toUpperCase()}`

const getStatusType = getReservationStatusType

const cloneRecords = (records) => records.map((record) => ({ ...record }))

const getCurrentLabel = () => {
  const now = new Date()
  return `${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

const formatDate = (date = new Date()) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

const parseDate = (dateText) => {
  if (!dateText) return null
  const date = new Date(`${dateText}T00:00:00`)
  return Number.isNaN(date.getTime()) ? null : date
}

const getAcademicWeek = (dateText) => {
  const date = parseDate(dateText)
  const start = parseDate(semesterStartDate)
  if (!date || !start) return 0
  const diffDays = Math.floor((date.getTime() - start.getTime()) / 86400000)
  return Math.max(1, Math.floor(diffDays / 7) + 1)
}

const getWeekday = (dateText) => {
  const date = parseDate(dateText)
  return date ? date.getDay() || 7 : 0
}

const normalizeText = (value) =>
  String(value || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')

const parseTimeToMinutes = (timeText) => {
  const match = String(timeText || '').match(/^(\d{1,2}):(\d{2})$/)
  if (!match) return 0
  return Number(match[1]) * 60 + Number(match[2])
}

const parseTimeRange = (timeRange) => {
  const [startTime, endTime] = String(timeRange || '').split('-')
  const start = parseTimeToMinutes(startTime)
  const end = parseTimeToMinutes(endTime)
  return start && end && end > start ? { start, end, startTime, endTime } : null
}

const rangesOverlap = (left, right) => Boolean(left && right && left.start < right.end && right.start < left.end)

const parseNumberList = (value) => {
  const numbers = new Set()
  String(value || '')
    .split(',')
    .forEach((part) => {
      const range = part.match(/(\d{1,2})\s*[-~至]\s*(\d{1,2})/)
      if (range) {
        const start = Number(range[1])
        const end = Number(range[2])
        for (let number = Math.min(start, end); number <= Math.max(start, end); number += 1) {
          numbers.add(number)
        }
        return
      }

      const single = part.match(/\d{1,2}/)
      if (single) numbers.add(Number(single[0]))
    })
  return [...numbers]
}

const getRecordWeeks = (record = {}) => {
  const expanded = parseNumberList(record.weekExpanded)
  if (expanded.length) return expanded
  return parseNumberList(record.weekText || record.weekRaw)
}

const getRecordStartSection = (record = {}) => {
  const start = Number(record.startSection || 0)
  if (start > 0) return start
  const parsed = parseNumberList(record.sectionCode || record.sectionText)
  return parsed[0] || 0
}

const getRecordEndSection = (record = {}) => {
  const end = Number(record.endSection || 0)
  if (end > 0) return end
  const parsed = parseNumberList(record.sectionCode || record.sectionText)
  return parsed[parsed.length - 1] || getRecordStartSection(record)
}

const getSectionRange = (section, edge) => {
  const unit = Math.ceil(Number(section || 0) / 2)
  return sectionTimeRanges[unit]?.[edge] || 0
}

const getRecordTimeRange = (record = {}) => {
  const startSection = getRecordStartSection(record)
  const endSection = getRecordEndSection(record)
  const start = getSectionRange(startSection, 'start')
  const end = getSectionRange(endSection, 'end')
  return start && end && end > start ? { start, end } : null
}

const getLabCode = (lab = {}) => lab.code || lab.labCode || lab.room || lab.roomNo || String(lab.name || '').match(/^[A-Z]\d{3}/i)?.[0] || ''

const normalizeLabMeta = (lab = {}) => {
  const name = lab.name || lab.labName || lab.roomName || lab.code || lab.labCode || ''
  const code = getLabCode({ ...lab, name })
  const displayName = formatLabDisplayName({ ...lab, name, code })
  return {
    ...lab,
    name,
    code,
    displayName,
    capacity: Number(lab.capacity) || 0,
  }
}

const buildLabMetaMap = (labs = fallbackLabs) =>
  Object.fromEntries(
    labs
      .map(normalizeLabMeta)
      .filter((lab) => lab.name)
      .flatMap((lab) =>
        [...new Set([lab.displayName, lab.name, lab.code].filter(Boolean))].map((key) => [key, lab]),
      ),
  )

const hasLabCodeToken = (classroom, code) => {
  const normalizedCode = String(code || '').trim()
  if (!normalizedCode) return false
  return new RegExp(`(^|[^0-9A-Za-z])${normalizedCode.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}([^0-9A-Za-z]|$)`).test(
    String(classroom || ''),
  )
}

const isRecordForLab = (record = {}, labName, labMeta = {}) => {
  const classroomRaw = String(record.classroom || '')
  const classroom = normalizeText(classroomRaw)
  if (!classroom) return false

  const selectedLabName = normalizeText(labName)
  if (selectedLabName && (classroom.includes(selectedLabName) || selectedLabName.includes(classroom))) return true

  const code = labMeta.code || getLabCode(labMeta) || String(labName || '').match(/^[A-Z]\d{3}/i)?.[0]
  return hasLabCodeToken(classroomRaw, code)
}

const isRecordForDate = (record = {}, dateText) => {
  const week = getAcademicWeek(dateText)
  const weekday = getWeekday(dateText)
  const weeks = getRecordWeeks(record)

  if (weeks.length && !weeks.includes(week)) return false
  if (weekday > 0 && weekdayOrder[record.weekday] !== weekday) return false

  return true
}

const formatTimetableConflict = (record = {}) =>
  [record.courseName, record.teacher, record.sectionText || record.sectionCode, record.className || record.rowClassName]
    .filter(Boolean)
    .join(' · ')

export function useReservationManagement() {
  const reservations = ref([])
  const selectedReservationId = ref('')
  const keyword = ref('')
  const statusFilter = ref('all')
  const sceneFilter = ref('all')
  const labFilter = ref('all')
  const conflictOnly = ref(false)
  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const formSubmitting = ref(false)
  const editingId = ref('')
  const formModel = reactive({
    applicant: '',
    department: '',
    contact: '',
    lab: '',
    date: '',
    timeRange: '',
    scene: '',
    attendees: 1,
    reason: '',
    note: '',
  })
  const labOptions = ref([...fallbackLabOptions])
  const labMetaMap = ref(buildLabMetaMap())
  const timetableRows = ref([])
  const timetableReady = ref(false)
  const activeSemester = ref('')

  const selectedReservation = computed(() => reservations.value.find((item) => item.id === selectedReservationId.value) || reservations.value[0] || null)
  const selectedLabMeta = computed(() => labMetaMap.value[formModel.lab] || normalizeLabMeta({ name: formModel.lab }))
  const selectedLabId = computed(() => resolveReservationLabId(selectedLabMeta.value))
  const getCanonicalLabValue = (labName) => labMetaMap.value[labName]?.displayName || labName
  const getLabMatchTokens = (labName) => {
    const labMeta = labMetaMap.value[labName] || normalizeLabMeta({ name: labName })

    return [
      labName,
      labMeta.displayName,
      labMeta.name,
      labMeta.labName,
      labMeta.roomName,
      labMeta.classroomName,
      labMeta.code,
      labMeta.labCode,
      labMeta.room,
      labMeta.roomNo,
    ]
      .map(normalizeText)
      .filter(Boolean)
  }
  const isSameLab = (leftLab, rightLab) => {
    if (!leftLab || !rightLab) return false
    const leftTokens = new Set(getLabMatchTokens(leftLab))
    return getLabMatchTokens(rightLab).some((token) => leftTokens.has(token))
  }
  const normalizeReservationRecord = (record = {}) =>
    normalizeReservationForDisplay({
      ...record,
      lab: getCanonicalLabValue(record.lab) || record.lab,
    })
  const normalizeReservationRecords = (records = []) => records.map(normalizeReservationRecord)
  const applyAuthoritativeReservation = (reservation, { insertIfMissing = false, expectedId } = {}) => {
    const normalizedReservation = normalizeReservationRecord(reservation)
    reservations.value = applyAuthoritativeReservationRecord(reservations.value, normalizedReservation, { insertIfMissing, expectedId })
    return normalizedReservation
  }
  const hasDisplayStatusChanges = (records = [], normalizedRecords = []) =>
    records.some((record, index) => {
      const normalizedRecord = normalizedRecords[index]
      return normalizedRecord && (record.status !== normalizedRecord.status || Boolean(record.conflict) !== Boolean(normalizedRecord.conflict))
    })

  const formAvailability = computed(() => {
    const timeRange = parseTimeRange(formModel.timeRange)
    const capacity = Number(selectedLabMeta.value.capacity) || 0
    const baseResult = {
      capacity,
      dateWeek: getAcademicWeek(formModel.date),
      dateWeekday: getWeekday(formModel.date),
      isFree: true,
      isReady: timetableReady.value,
      reservationConflicts: [],
      timetableConflicts: [],
      messages: [],
    }

    if (!formModel.lab || !formModel.date || !timeRange) {
      return {
        ...baseResult,
        isFree: false,
        messages: ['请先选择实验室、日期和时段'],
      }
    }

    const reservationConflicts = reservations.value
      .filter((item) => !['已驳回', '已取消', '已过期', '已完成'].includes(item.status))
      .filter((item) => String(item.id) !== String(editingId.value))
      .filter((item) => isSameLab(item.lab, formModel.lab) && item.date === formModel.date)
      .filter((item) => rangesOverlap(parseTimeRange(item.timeRange), timeRange))

    const timetableConflicts = timetableRows.value
      .filter((record) => isRecordForLab(record, formModel.lab, selectedLabMeta.value))
      .filter((record) => isRecordForDate(record, formModel.date))
      .filter((record) => rangesOverlap(getRecordTimeRange(record), timeRange))

    const capacityExceeded = capacity > 0 && Number(formModel.attendees || 0) > capacity
    const messages = [
      ...reservationConflicts.map((item) => `已有预约：${item.applicant} ${item.timeRange}`),
      ...timetableConflicts.map((item) => `课表占用：${formatTimetableConflict(item) || item.classroom || '课表记录'}`),
      ...(capacityExceeded ? [`预约人数超过实验室容量 ${capacity} 人`] : []),
      ...(!timetableReady.value ? ['课表数据暂不可用，当前仅按已有预约判断'] : []),
    ]

    return {
      ...baseResult,
      isFree: !reservationConflicts.length && !timetableConflicts.length && !capacityExceeded,
      reservationConflicts,
      timetableConflicts,
      messages,
    }
  })

  const getAvailabilityForReservation = (reservation = {}, excludedId = reservation.id) => {
    const timeRange = parseTimeRange(reservation.timeRange)
    const labMeta = labMetaMap.value[reservation.lab] || normalizeLabMeta({ name: reservation.lab })
    const capacity = Number(labMeta.capacity) || 0

    if (!reservation.lab || !reservation.date || !timeRange) {
      return {
        capacity,
        isFree: false,
        reservationConflicts: [],
        timetableConflicts: [],
      }
    }

    const reservationConflicts = reservations.value
      .filter((item) => !['已驳回', '已取消', '已过期', '已完成'].includes(item.status))
      .filter((item) => String(item.id) !== String(excludedId))
      .filter((item) => isSameLab(item.lab, reservation.lab) && item.date === reservation.date)
      .filter((item) => rangesOverlap(parseTimeRange(item.timeRange), timeRange))

    const timetableConflicts = timetableRows.value
      .filter((record) => isRecordForLab(record, reservation.lab, labMeta))
      .filter((record) => isRecordForDate(record, reservation.date))
      .filter((record) => rangesOverlap(getRecordTimeRange(record), timeRange))

    const capacityExceeded = capacity > 0 && Number(reservation.attendees || 0) > capacity

    return {
      capacity,
      isFree: !reservationConflicts.length && !timetableConflicts.length && !capacityExceeded,
      reservationConflicts,
      timetableConflicts,
      capacityExceeded,
    }
  }

  const statusSummary = computed(() =>
    statusOptions.map((status) => ({
      ...status,
      count: reservations.value.filter((item) => item.status === status.label).length,
    })),
  )

  const metrics = computed(() => {
    const pendingCount = reservations.value.filter((item) => item.status === '待审核').length
    const approvedCount = reservations.value.filter((item) => item.status === '已通过').length
    const conflictCount = reservations.value.filter((item) => item.conflict || item.status === '冲突').length
    const expiredCount = reservations.value.filter((item) => item.status === '已过期').length
    const courseCount = reservations.value.filter((item) => item.scene === '课程').length

    return [
      { label: '预约申请', value: reservations.value.length, trend: `课程 ${courseCount} 条` },
      { label: '待管理员审核', value: pendingCount, trend: pendingCount ? '需尽快处理' : '暂无积压' },
      { label: '已通过', value: approvedCount, trend: '门禁同步放行' },
      { label: '冲突预警', value: conflictCount, trend: conflictCount ? '需人工复核' : '时间资源正常' },
      { label: '已过期', value: expiredCount, trend: expiredCount ? '已自动锁定' : '无过期申请' },
    ]
  })

  const filteredReservations = computed(() => {
    const searchText = keyword.value.trim().toLowerCase()

    return reservations.value.filter((item) => {
      const matchesKeyword =
        !searchText ||
        [item.id, item.applicant, item.department, item.contact, item.lab, item.reason, item.note].some((value) =>
          String(value ?? '').toLowerCase().includes(searchText),
        )
      const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
      const matchesScene = sceneFilter.value === 'all' || item.scene === sceneFilter.value
      const matchesLab = labFilter.value === 'all' || isSameLab(item.lab, labFilter.value)
      const matchesConflict = !conflictOnly.value || item.conflict || item.status === '冲突'

      return matchesKeyword && matchesStatus && matchesScene && matchesLab && matchesConflict
    })
  })

  const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新建预约申请' : '编辑预约申请'))

  const persistReservations = async () => {
    const persistedRecords = await reservationResourceService.replace(reservations.value)
    reservations.value = normalizeReservationRecords(Array.isArray(persistedRecords) ? persistedRecords : cloneRecords(initialReservations))
  }

  const persistConflictMarks = () => {
    if (!API_USE_MOCK) return
    reservationResourceService.replace(reservations.value).catch((error) => {
      console.warn('[ReservationManagement] failed to persist conflict marks', error)
    })
  }

  const loadReservations = async () => {
    const loadedRecords = await reservationResourceService.list({
      fallbackRecords: cloneRecords(initialReservations),
    })
    const sourceRecords = Array.isArray(loadedRecords) ? loadedRecords : cloneRecords(initialReservations)
    const normalizedRecords = normalizeReservationRecords(sourceRecords)
    reservations.value = normalizedRecords
    if (API_USE_MOCK && hasDisplayStatusChanges(sourceRecords, normalizedRecords)) {
      await reservationResourceService.replace(normalizedRecords)
    }
    selectedReservationId.value = reservations.value[0]?.id || ''
  }

  const loadLabOptions = async () => {
    try {
      const labs = await labResourceService.list()
      const normalizedLabs = labs.map(normalizeLabMeta).filter((lab) => lab.name)
      const displayNames = normalizedLabs.map((lab) => lab.displayName || lab.name).filter(Boolean)
      labOptions.value = displayNames.length ? [...new Set(displayNames)] : [...fallbackLabOptions]
      labMetaMap.value = displayNames.length ? buildLabMetaMap(normalizedLabs) : buildLabMetaMap()
    } catch {
      labOptions.value = [...fallbackLabOptions]
      labMetaMap.value = buildLabMetaMap()
    }
  }

  const loadTimetableRows = async () => {
    try {
      const semesters = await getClassTimetableSemesters()
      activeSemester.value = semesters?.[0] || ''
      timetableRows.value = await listClassTimetables({
        semester: activeSemester.value || undefined,
        limit: 5000,
      })
      timetableReady.value = true
    } catch {
      timetableRows.value = []
      timetableReady.value = false
    }
  }

  const selectReservation = (reservation) => {
    selectedReservationId.value = reservation?.id || ''
  }

  const assignFormModel = (reservation = {}) => {
    const hasReservation = Boolean(reservation?.id)
    formModel.applicant = reservation.applicant || ''
    formModel.department = reservation.department || ''
    formModel.contact = reservation.contact || ''
    formModel.lab = hasReservation ? getCanonicalLabValue(reservation.lab) : ''
    formModel.date = hasReservation ? reservation.date || '' : ''
    formModel.timeRange = hasReservation ? reservation.timeRange || '' : ''
    formModel.scene = hasReservation ? reservation.scene || '' : ''
    formModel.attendees = reservation.attendees || 1
    formModel.reason = reservation.reason || ''
    formModel.note = reservation.note || ''
  }

  const openCreateDialog = () => {
    dialogMode.value = 'create'
    editingId.value = ''
    assignFormModel()
    dialogVisible.value = true
  }

  const openEditDialog = (reservation) => {
    const actions = getReservationActions(reservation)
    if (!actions.edit) {
      ElMessage.warning('当前状态不能编辑')
      return
    }
    dialogMode.value = 'edit'
    editingId.value = reservation.id
    assignFormModel(reservation)
    dialogVisible.value = true
  }

  const validateForm = () => {
    const applicantError = getMeaningfulTextError(formModel.applicant, { label: '申请人', min: 2, max: 50 })
    if (applicantError) {
      ElMessage.warning(applicantError)
      return false
    }

    const departmentError = getMeaningfulTextError(formModel.department, {
      label: '学院/组织',
      min: 2,
      max: 80,
      required: false,
    })
    if (departmentError) {
      ElMessage.warning(departmentError)
      return false
    }

    const contactError = getContactError(formModel.contact, { label: '联系人', strict: false })
    if (contactError) {
      ElMessage.warning(contactError)
      return false
    }

    if (!formModel.lab) {
      ElMessage.warning('请选择预约实验室')
      return false
    }

    if (!API_USE_MOCK && !selectedLabId.value) {
      ElMessage.warning('实验室标识加载异常，请刷新实验室列表后重试')
      return false
    }

    if (!formModel.date) {
      ElMessage.warning('请选择预约日期')
      return false
    }

    if (!parseTimeRange(formModel.timeRange)) {
      ElMessage.warning('请选择有效的预约时段')
      return false
    }

    const reasonError = getMeaningfulTextError(formModel.reason, { label: '预约用途', min: 4, max: 160 })
    if (reasonError) {
      ElMessage.warning(reasonError)
      return false
    }

    const noteError = getMeaningfulTextError(formModel.note, {
      label: '审核备注',
      min: 4,
      max: 120,
      required: false,
    })
    if (noteError) {
      ElMessage.warning(noteError)
      return false
    }

    if (!formAvailability.value.isReady) {
      ElMessage.warning('课表数据暂不可用，无法确认实验室空闲')
      return false
    }

    if (formAvailability.value.timetableConflicts.length) {
      ElMessage.warning('所选时段已有课表占用，请更换实验室、日期或时段')
      return false
    }

    if (formAvailability.value.reservationConflicts.length) {
      ElMessage.warning('所选时段已有人工预约，请更换实验室、日期或时段')
      return false
    }

    if (formAvailability.value.capacity > 0 && Number(formModel.attendees || 0) > formAvailability.value.capacity) {
      ElMessage.warning(`预约人数不能超过实验室容量 ${formAvailability.value.capacity} 人`)
      return false
    }

    return true
  }

  const submitReservation = async () => {
    if (!validateForm()) return

    formSubmitting.value = true
    const previousReservations = [...reservations.value]
    const previousSelectedReservationId = selectedReservationId.value
    const payload = {
      applicant: normalizeInputText(formModel.applicant),
      department: normalizeInputText(formModel.department) || '未填写',
      contact: normalizeInputText(formModel.contact) || '未填写',
      lab: formModel.lab,
      ...(API_USE_MOCK ? {} : { labId: selectedLabId.value }),
      date: formModel.date,
      timeRange: String(formModel.timeRange || '').trim(),
      scene: formModel.scene,
      attendees: Number(formModel.attendees) || 1,
      reason: normalizeInputText(formModel.reason),
      note: normalizeInputText(formModel.note) || '待管理员审核。',
    }

    try {
      if (dialogMode.value === 'create') {
        const savedReservation = await reservationResourceService.create({
          ...(API_USE_MOCK ? { id: createReservationId() } : {}),
          ...payload,
          status: '待审核',
          tagType: 'warning',
          conflict: false,
          reviewer: '待审核',
          submittedAt: getCurrentLabel(),
          reviewedAt: '-',
        })
        const displayedReservation = applyAuthoritativeReservation(savedReservation, { insertIfMissing: true })
        selectedReservationId.value = displayedReservation.id
      } else {
        const currentRecord = reservations.value.find((item) => String(item.id) === String(editingId.value))
        if (!currentRecord) throw new Error('未找到要更新的预约记录')

        const nextRecord = {
          ...currentRecord,
          ...payload,
          status: currentRecord.status === '已通过' ? '待审核' : currentRecord.status,
          tagType: currentRecord.status === '已通过' ? 'warning' : currentRecord.tagType,
          reviewer: currentRecord.status === '已通过' ? '待审核' : currentRecord.reviewer,
          reviewedAt: currentRecord.status === '已通过' ? '-' : currentRecord.reviewedAt,
        }

        const savedReservation = await reservationResourceService.update(editingId.value, nextRecord)
        const displayedReservation = applyAuthoritativeReservation(savedReservation, { expectedId: editingId.value })
        selectedReservationId.value = displayedReservation.id
      }

      dialogVisible.value = false
      ElMessage.success(`${dialogMode.value === 'create' ? '新增' : '更新'}预约成功`)
    } catch (error) {
      reservations.value = previousReservations
      selectedReservationId.value = previousSelectedReservationId
      ElMessage.error(error?.message || '保存预约失败')
    } finally {
      formSubmitting.value = false
    }
  }

  const isCancellationAllowed = (reservation) => {
    const status = normalizeReservationStatus(reservation)
    return [RESERVATION_STATUS.pending, RESERVATION_STATUS.approved].includes(status) && !isReservationExpired(reservation)
  }

  const buildReviewPayload = (note) => ({
    reviewer: '张明',
    reviewerName: '张明',
    note,
    reviewRemark: note,
  })

  const approveReservation = async (reservation = selectedReservation.value) => {
    if (!reservation) return

    const currentReservation = normalizeReservationForDisplay(reservation)
    if (currentReservation.status === RESERVATION_STATUS.expired) {
      ElMessage.warning('该预约已过期，无法通过')
      return
    }

    if (currentReservation.conflict || currentReservation.status === RESERVATION_STATUS.conflict) {
      ElMessage.warning('该预约存在时间冲突，无法通过')
      return
    }

    if (currentReservation.status !== RESERVATION_STATUS.pending) {
      ElMessage.warning('只能通过待审核预约')
      return
    }

    const availability = getAvailabilityForReservation(currentReservation, currentReservation.id)
    if (!timetableReady.value) {
      ElMessage.warning('课表数据暂不可用，无法确认实验室空闲')
      return
    }

    if (availability.timetableConflicts.length || availability.reservationConflicts.length) {
      ElMessage.warning('该预约存在时间冲突，无法通过')
      return
    }

    if (availability.capacityExceeded) {
      ElMessage.warning(`预约人数超过实验室容量 ${availability.capacity} 人，不能通过`)
      return
    }

    const previousReservations = [...reservations.value]
    const previousSelectedReservationId = selectedReservationId.value
    try {
      await ElMessageBox.confirm(`确认通过“${currentReservation.applicant}”的预约申请吗？`, '通过预约', {
        type: 'success',
        confirmButtonText: '通过',
        cancelButtonText: '取消',
      })
      const approvedReservation = await reservationResourceService.approve(
        currentReservation.id,
        buildReviewPayload('管理员审核通过，已同步门禁与课表。'),
      )
      const displayedReservation = applyAuthoritativeReservation(approvedReservation, { expectedId: currentReservation.id })
      selectedReservationId.value = displayedReservation.id
      ElMessage.success('已通过预约申请')
    } catch (error) {
      reservations.value = previousReservations
      selectedReservationId.value = previousSelectedReservationId
      if (error !== 'cancel' && error !== 'close') {
        ElMessage.error(error?.message || '通过预约失败')
      }
    }
  }

  const rejectReservation = async (reservation = selectedReservation.value) => {
    if (!reservation) return

    const currentReservation = normalizeReservationForDisplay(reservation)
    if (![RESERVATION_STATUS.pending, RESERVATION_STATUS.conflict].includes(currentReservation.status)) {
      ElMessage.warning(currentReservation.status === RESERVATION_STATUS.expired ? '已过期预约不能驳回' : '当前状态不能驳回')
      return
    }

    if (currentReservation.status === RESERVATION_STATUS.expired) return

    const previousReservations = [...reservations.value]
    const previousSelectedReservationId = selectedReservationId.value
    try {
      const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回预约', {
        confirmButtonText: '驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：预约时段冲突或申请信息不完整',
        inputValidator: (value) => (!normalizeInputText(value) ? '请填写驳回原因' : getMeaningfulTextError(value, { label: '驳回原因', min: 4, max: 120 }) || true),
      })
      const reason = normalizeInputText(value)
      if (!reason) {
        ElMessage.warning('请填写驳回原因')
        return
      }
      const rejectedReservation = await reservationResourceService.reject(currentReservation.id, buildReviewPayload(reason))
      const displayedReservation = applyAuthoritativeReservation(rejectedReservation, { expectedId: currentReservation.id })
      selectedReservationId.value = displayedReservation.id
      ElMessage.success('已驳回预约申请')
    } catch (error) {
      reservations.value = previousReservations
      selectedReservationId.value = previousSelectedReservationId
      if (error !== 'cancel' && error !== 'close') {
        ElMessage.error(error?.message || '驳回预约失败')
      }
    }
  }

  const cancelReservation = async (reservation = selectedReservation.value) => {
    if (!reservation) return

    const currentReservation = normalizeReservationForDisplay(reservation)
    if (!isCancellationAllowed(currentReservation)) {
      ElMessage.warning('当前状态不能取消预约')
      return
    }

    const previousReservations = [...reservations.value]
    const previousSelectedReservationId = selectedReservationId.value
    try {
      const { value } = await ElMessageBox.prompt(`请填写取消“${currentReservation.applicant}”预约的原因`, '取消预约', {
        confirmButtonText: '取消预约',
        cancelButtonText: '返回',
        inputPlaceholder: '例如：课程调整或申请人主动取消',
        inputValidator: (value) => (!normalizeInputText(value) ? '请填写取消原因' : getMeaningfulTextError(value, { label: '取消原因', min: 2, max: 120 }) || true),
      })
      const reason = normalizeInputText(value)
      if (!reason) {
        ElMessage.warning('请填写取消原因')
        return
      }
      const cancelledReservation = await reservationResourceService.update(currentReservation.id, {
        ...currentReservation,
        status: '已取消',
        tagType: 'info',
        conflict: false,
        reviewer: '张明',
        reviewedAt: getCurrentLabel(),
        cancelReason: reason,
        note: `取消原因：${reason}`,
      })
      const displayedReservation = applyAuthoritativeReservation(cancelledReservation, { expectedId: currentReservation.id })
      selectedReservationId.value = displayedReservation.id
      ElMessage.success('预约已取消')
    } catch (error) {
      reservations.value = previousReservations
      selectedReservationId.value = previousSelectedReservationId
      if (error !== 'cancel' && error !== 'close') {
        ElMessage.error(error?.message || '取消预约失败')
      }
    }
  }

  const deleteReservation = async (reservation) => {
    if (!reservation) return

    try {
      await ElMessageBox.confirm(`确认删除“${reservation.applicant}”的预约申请吗？`, '删除预约', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
      await reservationResourceService.remove(reservation.id)
      await loadReservations()
      ElMessage.success('已删除预约申请')
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') {
        ElMessage.error(error?.message || '删除预约失败')
      }
    }
  }
  const detectConflicts = (showMessage = true) => {
    const activeRecords = reservations.value.filter((item) => !['已驳回', '已取消', '已过期', '已完成'].includes(item.status))
    const conflictIds = new Set()

    activeRecords.forEach((item, index) => {
      const itemRange = parseTimeRange(item.timeRange)
      activeRecords.slice(index + 1).forEach((other) => {
        if (!isSameLab(item.lab, other.lab) || item.date !== other.date) return
        if (!rangesOverlap(itemRange, parseTimeRange(other.timeRange))) return
        conflictIds.add(item.id)
        conflictIds.add(other.id)
      })

      const availability = getAvailabilityForReservation(item, item.id)
      if (availability.timetableConflicts.length || availability.capacityExceeded) {
        conflictIds.add(item.id)
      }
    })

    reservations.value = reservations.value.map((item) => {
      if (['已驳回', '已取消', '已过期', '已完成'].includes(item.status)) {
        return { ...item, conflict: false, tagType: getStatusType(item.status) }
      }
      const isConflict = conflictIds.has(item.id)
      if (!isConflict && item.status === '冲突') {
        return { ...item, conflict: false, status: '待审核', tagType: 'warning', reviewer: '待审核', reviewedAt: '-' }
      }
      if (isConflict) {
        return { ...item, conflict: true, status: '冲突', tagType: 'danger', reviewer: '系统预警', reviewedAt: '-' }
      }
      return { ...item, conflict: false }
    })

    persistConflictMarks()

    if (showMessage) {
      if (conflictIds.size) {
        conflictOnly.value = true
        ElMessage.warning(`发现 ${conflictIds.size} 条冲突预约，已切换到冲突视图`)
      } else {
        ElMessage.success('未发现时间或资源冲突')
      }
    }
  }

  const resetReservations = async () => {
    try {
      await ElMessageBox.confirm('确认恢复预约管理初始数据吗？', '恢复数据', {
        type: 'info',
        confirmButtonText: '恢复',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    keyword.value = ''
    statusFilter.value = 'all'
    sceneFilter.value = 'all'
    labFilter.value = 'all'
    conflictOnly.value = false
    reservationResourceService
      .reset({
        fallbackRecords: cloneRecords(initialReservations),
      })
      .then((resetRecords) => {
        reservations.value = normalizeReservationRecords(Array.isArray(resetRecords) ? resetRecords : cloneRecords(initialReservations))
        selectedReservationId.value = reservations.value[0]?.id || ''
      })
    ElMessage.success('已恢复预约初始数据')
  }

  const exportReservations = () => {
    const exportRows = filteredReservations.value

    if (!exportRows.length) {
      ElMessage.warning('暂无可导出的预约记录')
      return
    }

    const exportedCount = exportTableToExcel({
      filename: '预约审核',
      sheetName: '预约审核',
      rows: exportRows,
      columns: [
        { label: '预约编号', prop: 'id' },
        { label: '申请人', prop: 'applicant' },
        { label: '学院/组织', prop: 'department' },
        { label: '联系方式', prop: 'contact' },
        { label: '实验室', prop: 'lab' },
        { label: '日期', prop: 'date' },
        { label: '时段', prop: 'timeRange' },
        { label: '类型', prop: 'scene' },
        { label: '人数', prop: 'attendees' },
        { label: '状态', prop: 'status' },
        { label: '冲突状态', formatter: (item) => (item.conflict ? '存在冲突' : '无冲突') },
        { label: '审核人', prop: 'reviewer' },
        { label: '提交时间', prop: 'submittedAt' },
        { label: '审核时间', prop: 'reviewedAt' },
        { label: '用途', prop: 'reason' },
        { label: '备注', prop: 'note' },
      ],
    })
    ElMessage.success(`已导出 ${exportedCount} 条预约`)
  }

  watch(
    () => [formModel.lab, formModel.attendees],
    () => {
      const capacity = Number(selectedLabMeta.value.capacity) || 0
      if (capacity > 0 && Number(formModel.attendees || 0) > capacity) {
        formModel.attendees = capacity
      }
    },
  )

  watch(filteredReservations, async (records) => {
    if (!records.length) return
    if (!records.some((item) => item.id === selectedReservationId.value)) {
      await nextTick()
      selectedReservationId.value = records[0].id
    }
  })

  void Promise.all([loadLabOptions(), loadTimetableRows()]).then(async () => {
    await loadReservations()
  })

  return {
    conflictOnly,
    dialogTitle,
    dialogVisible,
    filteredReservations,
    formAvailability,
    formModel,
    formSubmitting,
    keyword,
    labMetaMap,
    labFilter,
    labOptions,
    metrics,
    reservations,
    sceneFilter,
    sceneOptions,
    selectedReservation,
    selectedReservationId,
    statusFilter,
    statusOptions,
    statusSummary,
    approveReservation,
    cancelReservation,
    deleteReservation,
    detectConflicts,
    exportReservations,
    openCreateDialog,
    openEditDialog,
    rejectReservation,
    resetReservations,
    selectReservation,
    submitReservation,
  }
}
