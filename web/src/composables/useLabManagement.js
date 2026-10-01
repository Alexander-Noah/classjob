import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { labResourceService } from '../api/services/basicResources'
import { listUserOptions } from '../api/services/users'
import { exportTableToExcel } from '../utils/exportExcel'

const STATUS_OPTIONS = [
  { label: '开放', type: 'success' },
  { label: '预约中', type: 'primary' },
  { label: '使用中', type: 'warning' },
  { label: '维护中', type: 'warning' },
  { label: '停用', type: 'info' },
]

const TYPE_OPTIONS = ['教学实训', '科研创新', '网络安全', '人工智能', '基础机房']

const SAFETY_OPTIONS = ['正常', '关注', '风险', '告警', '停用']

const INITIAL_LABS = [
  {
    id: 'lab-a101',
    code: 'A101',
    name: 'A101 云计算实验室',
    building: '信息楼 A 区',
    floor: '1F',
    room: 'A101',
    capacity: 72,
    manager: '张明',
    type: '教学实训',
    status: '开放',
    devices: 78,
    openTime: '08:00-22:00',
    safety: '正常',
    features: ['云计算', '课程上机', '虚拟化'],
    schedule: ['今日暂无排课'],
  },
  {
    id: 'lab-b203',
    code: 'B203',
    name: 'B203 网络安全实验室',
    building: '实训楼 B 区',
    floor: '2F',
    room: 'B203',
    capacity: 56,
    manager: '李婷',
    type: '网络安全',
    status: '预约中',
    devices: 64,
    openTime: '08:30-21:30',
    safety: '正常',
    features: ['攻防演练', '隔离网络', '日志审计'],
    schedule: ['今日暂无排课'],
  },
  {
    id: 'lab-c305',
    code: 'C305',
    name: 'C305 人工智能实验室',
    building: '创新楼 C 区',
    floor: '3F',
    room: 'C305',
    capacity: 64,
    manager: '王磊',
    type: '人工智能',
    status: '维护中',
    devices: 82,
    openTime: '09:00-22:00',
    safety: '烟雾关注',
    features: ['GPU 集群', '深度学习', '模型训练'],
    schedule: ['今日暂无排课'],
  },
]

const STATUS_TAG_TYPES = {
  开放: 'success',
  预约中: 'primary',
  使用中: 'warning',
  维护中: 'warning',
  停用: 'info',
}

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

const openStatus = '开放'
const reservedStatus = '预约中'
const busyStatus = '使用中'
const maintenanceStatus = '维护中'
const disabledStatus = '停用'
const lockedScheduleStatuses = new Set([maintenanceStatus, disabledStatus])
const SECTION_TIME_MAP = {
  1: '08:30-10:00',
  2: '10:20-11:50',
  3: '14:00-15:30',
  4: '15:50-17:20',
  5: '18:40-20:10',
  6: '20:20-21:50',
}
const SECTION_TIME_RANGES = {
  1: { start: 8 * 60 + 30, end: 10 * 60 },
  2: { start: 10 * 60 + 20, end: 11 * 60 + 50 },
  3: { start: 14 * 60, end: 15 * 60 + 30 },
  4: { start: 15 * 60 + 50, end: 17 * 60 + 20 },
  5: { start: 18 * 60 + 40, end: 20 * 60 + 10 },
  6: { start: 20 * 60 + 20, end: 21 * 60 + 50 },
}
const PRE_CLASS_BUSY_MINUTES = 10
const cloneLabs = () => JSON.parse(JSON.stringify(INITIAL_LABS))

const splitFeatures = (features) =>
  Array.isArray(features)
    ? features
    : String(features || '')
        .split(/[,，]/)
        .map((feature) => feature.trim())
        .filter(Boolean)

const getStatusType = (status) =>
  STATUS_TAG_TYPES[status] || STATUS_OPTIONS.find((option) => option.label === status)?.type || 'info'

const normalizeText = (value) =>
  String(value || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')

const parseNumberList = (value) => {
  const text = String(value || '')
  const numbers = new Set()

  text.split(',').forEach((part) => {
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

const getRecordWeeks = (record) => {
  const expanded = parseNumberList(record.weekExpanded)
  if (expanded.length) return expanded
  return parseNumberList(record.weekText || record.weekRaw)
}

const getRecordStartSection = (record) => {
  const start = Number(record.startSection || 0)
  if (start > 0) return start
  const parsed = parseNumberList(record.sectionCode || record.sectionText)
  return parsed[0] || 0
}

const getRecordEndSection = (record) => {
  const end = Number(record.endSection || 0)
  if (end > 0) return end
  const parsed = parseNumberList(record.sectionCode || record.sectionText)
  return parsed[parsed.length - 1] || getRecordStartSection(record)
}

const getRecordTimeText = (record) => {
  const start = getRecordStartSection(record)
  if (!start) return ''

  const unit = Math.ceil(start / 2)
  const time = SECTION_TIME_MAP[unit]
  return time ? `第${unit}节 ${time}` : `第${unit}节`
}

const getRecordTimeUnit = (record) => {
  const start = getRecordStartSection(record)
  return start ? Math.ceil(start / 2) : 0
}

const isRecordInBusyWindow = (record, currentMinute) => {
  const range = SECTION_TIME_RANGES[getRecordTimeUnit(record)]
  if (!range) return false

  return currentMinute >= range.start - PRE_CLASS_BUSY_MINUTES && currentMinute < range.end
}

const escapeRegExp = (value) => String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const hasLabCodeToken = (classroom, code) => {
  const normalizedCode = String(code || '').trim()
  if (!normalizedCode) return false
  return new RegExp(`(^|[^0-9A-Za-z])${escapeRegExp(normalizedCode)}([^0-9A-Za-z]|$)`).test(String(classroom || ''))
}

const isRecordForLab = (record, lab) => {
  const classroomRaw = String(record.classroom || '')
  const classroom = normalizeText(classroomRaw)
  if (!classroom) return false

  const labName = normalizeText(lab.name || lab.labName)
  if (labName && (classroom.includes(labName) || labName.includes(classroom))) {
    return true
  }

  return hasLabCodeToken(classroomRaw, lab.code || lab.labCode || lab.room)
}

const getReservationLabText = (reservation = {}) =>
  reservation.lab || reservation.labName || reservation.labDisplayName || reservation.roomName || reservation.classroom || ''

const getReservationDate = (reservation = {}) =>
  reservation.date || reservation.reservationDate || reservation.useDate || reservation.startDate || ''

const getReservationTimeRangeText = (reservation = {}) => {
  const rangeText = reservation.timeRange || reservation.timeSlot || reservation.period || ''
  if (rangeText) return String(rangeText)

  const startTime = reservation.startTime || reservation.beginTime || ''
  const endTime = reservation.endTime || reservation.finishTime || ''
  return startTime && endTime ? `${startTime}-${endTime}` : ''
}

const parseTimeToMinutes = (timeText) => {
  const match = String(timeText || '').match(/(\d{1,2}):(\d{2})/)
  if (!match) return 0
  return Number(match[1]) * 60 + Number(match[2])
}

const getReservationTimeRange = (reservation = {}) => {
  const [startText, endText] = getReservationTimeRangeText(reservation).split('-')
  const start = parseTimeToMinutes(startText)
  const end = parseTimeToMinutes(endText)
  return start && end && end > start ? { start, end } : null
}

const isReservationForLab = (reservation = {}, lab = {}) => {
  const labTextRaw = String(getReservationLabText(reservation) || '')
  const labText = normalizeText(labTextRaw)
  if (!labText) return false

  const labName = normalizeText(lab.name || lab.labName)
  if (labName && (labText.includes(labName) || labName.includes(labText))) return true

  return hasLabCodeToken(labTextRaw, lab.code || lab.labCode || lab.room)
}

const isReservationApproved = (reservation = {}) => ['已通过', 'approved', 'APPROVED'].includes(reservation.status)

const isReservationForDate = (reservation = {}, dateText) => {
  if (!dateText) return false
  return String(getReservationDate(reservation) || '') === String(dateText)
}

const isReservationInBusyWindow = (reservation = {}, currentMinute) => {
  const range = getReservationTimeRange(reservation)
  if (!range) return false
  return currentMinute >= range.start - PRE_CLASS_BUSY_MINUTES && currentMinute < range.end
}

const formatReservationItem = (reservation = {}) =>
  [
    '预约',
    getReservationTimeRangeText(reservation),
    reservation.scene || reservation.type || reservation.reservationType,
    reservation.applicant || reservation.userName || reservation.contact,
  ]
    .filter(Boolean)
    .join(' · ')

const isRecordInScheduleContext = (record, context = {}) => {
  const week = Number(context.week || 0)
  const weekday = Number(context.weekday || 0)
  const section = Number(context.section || 0)

  if (!week && !weekday && !section) return false

  if (week > 0) {
    const weeks = getRecordWeeks(record)
    if (weeks.length && !weeks.includes(week)) return false
  }

  if (weekday > 0 && weekdayOrder[record.weekday] !== weekday) return false

  if (section > 0) {
    const start = getRecordStartSection(record)
    const end = getRecordEndSection(record)
    if (!start || section < start || section > end) return false
  }

  return true
}

const formatScheduleItem = (record) =>
  [record.weekday, record.timeText || getRecordTimeText(record), record.sectionText, record.courseName, record.teacher]
    .filter(Boolean)
    .join(' · ')

const sortTimetableRows = (rows = []) =>
  [...rows].sort((left, right) => {
    const leftSection = getRecordStartSection(left)
    const rightSection = getRecordStartSection(right)
    if (leftSection !== rightSection) return leftSection - rightSection

    const leftEnd = getRecordEndSection(left)
    const rightEnd = getRecordEndSection(right)
    if (leftEnd !== rightEnd) return leftEnd - rightEnd

    return String(left.courseName || '').localeCompare(String(right.courseName || ''), 'zh-Hans-CN')
  })

const getTimetableRowKey = (record = {}) =>
  [
    record.semester,
    record.weekday,
    record.sectionText || record.sectionCode,
    record.courseName,
    record.teacher,
    record.className || record.rowClassName,
    record.weekText || record.weekRaw || record.weekExpanded,
    record.classroom,
  ].join('|')

const dedupeTimetableRows = (rows = []) => {
  const seen = new Set()
  return rows.filter((row) => {
    const key = getTimetableRowKey(row)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export function useLabManagement() {
  const keyword = ref('')
  const buildingFilter = ref('all')
  const statusFilter = ref('all')
  const typeFilter = ref('all')
  const availableOnly = ref(false)
  const labs = ref([])
  const managerOptions = ref([])
  const selectedLabId = ref('')
  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const formSubmitting = ref(false)
  const formModel = reactive({})
  const labScheduleMap = ref({})
  const currentScheduleRecords = ref([])
  const currentReservationRecords = ref([])
  const currentScheduleContext = ref({})

  const findManager = (managerUserId) =>
    managerOptions.value.find((user) => String(user.id) === String(managerUserId))

  const getUserDisplayName = (user) => user?.realName || user?.username || ''

  const normalizeLab = (lab) => {
    const capacity = Number(lab.capacity) || 0
    const status = lab.status || lab.openStatus || openStatus
    const managerUserId = lab.managerUserId ?? ''

    return {
      ...lab,
      id: lab.id || `lab-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      code: lab.code || lab.labCode || '',
      name: lab.name || lab.labName || '',
      building: lab.building || '',
      floor: lab.floor || '',
      room: lab.room || lab.roomNo || '',
      capacity,
      managerUserId,
      manager: lab.manager || lab.managerName || getUserDisplayName(findManager(managerUserId)),
      type: lab.type || lab.labType || TYPE_OPTIONS[0],
      status,
      devices: Number(lab.devices ?? lab.deviceCount) || 0,
      openTime: lab.openTime || '08:00-22:00',
      safety: lab.safety || '正常',
      features: splitFeatures(lab.features),
      schedule: Array.isArray(lab.schedule) && lab.schedule.length ? lab.schedule : ['今日暂无排课'],
      tagType: getStatusType(status),
    }
  }

  const toServerLabPayload = (lab) => {
    const numericId = Number(lab.id)
    const hasNumericId = String(lab.id ?? '').trim() !== '' && Number.isInteger(numericId)

    return {
      ...(hasNumericId ? { id: numericId } : {}),
      labCode: lab.code,
      labName: lab.name,
      building: lab.building,
      floor: lab.floor,
      roomNo: lab.room,
      labType: lab.type,
      capacity: lab.capacity,
      managerUserId: lab.managerUserId ? Number(lab.managerUserId) : null,
      managerName: lab.manager,
      openStatus: lab.status,
      remark: lab.remark || '',
    }
  }

  const loadManagerOptions = async () => {
    try {
      managerOptions.value = await listUserOptions()
    } catch {
      managerOptions.value = []
    }
  }

  const loadLabs = async () => {
    const loadedRecords = await labResourceService.list({
      fallbackRecords: cloneLabs(),
    })
    labs.value = loadedRecords.map(normalizeLab)
    selectedLabId.value = labs.value[0]?.id || ''
    bindLabSchedules(currentScheduleRecords.value, currentScheduleContext.value)
  }

  const persistLabs = async () => {
    const persistedRecords = await labResourceService.replace(labs.value.map(toServerLabPayload))
    labs.value = persistedRecords.map(normalizeLab)
  }

  const selectedLab = computed(() => labs.value.find((lab) => lab.id === selectedLabId.value) || labs.value[0])

  const getBoundLabSchedule = (lab = selectedLab.value) => {
    if (!lab) return []
    return labScheduleMap.value[String(lab.id)] || []
  }

  const buildingOptions = computed(() => [...new Set(labs.value.map((lab) => lab.building).filter(Boolean))])

  const filteredLabs = computed(() => {
    const searchText = keyword.value.trim().toLowerCase()

    return labs.value.filter((lab) => {
      const matchesKeyword =
        !searchText ||
        [lab.name, lab.code, lab.building, lab.manager, lab.type, lab.status].some((value) =>
          String(value || '').toLowerCase().includes(searchText),
        )
      const matchesBuilding = buildingFilter.value === 'all' || lab.building === buildingFilter.value
      const matchesStatus = statusFilter.value === 'all' || lab.status === statusFilter.value
      const matchesType = typeFilter.value === 'all' || lab.type === typeFilter.value
      const matchesAvailable = !availableOnly.value || lab.status === openStatus

      return matchesKeyword && matchesBuilding && matchesStatus && matchesType && matchesAvailable
    })
  })

  const statusSummary = computed(() =>
    STATUS_OPTIONS.map((option) => ({
      ...option,
      count: labs.value.filter((lab) => lab.status === option.label).length,
    })),
  )

  const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增实验室' : '编辑实验室'))

  const assignFormModel = (lab = {}) => {
    Object.keys(formModel).forEach((key) => {
      delete formModel[key]
    })

    const capacity = lab.capacity ?? 48

    Object.assign(formModel, {
      id: lab.id || '',
      code: lab.code || '',
      name: lab.name || '',
      building: lab.building || '',
      floor: lab.floor || '',
      room: lab.room || '',
      capacity,
      managerUserId: lab.managerUserId || '',
      manager: lab.manager || '',
      type: lab.type || TYPE_OPTIONS[0],
      status: lab.status || openStatus,
      devices: lab.devices ?? 0,
      openTime: lab.openTime || '08:00-22:00',
      safety: lab.safety || '正常',
      featuresText: Array.isArray(lab.features) ? lab.features.join('，') : '',
    })
  }

  const validateForm = () => {
    const requiredFields = [
      ['code', '实验室编号'],
      ['name', '实验室名称'],
      ['building', '所在楼宇'],
      ['managerUserId', '负责人'],
    ]
    const invalidField = requiredFields.find(([prop]) => !String(formModel[prop] || '').trim())

    if (invalidField) {
      ElMessage.warning(`请填写${invalidField[1]}`)
      return false
    }

    return true
  }

  const normalizeFormLab = () => {
    const selectedManager = findManager(formModel.managerUserId)
    const capacity = Number(formModel.capacity)

    return normalizeLab({
      ...formModel,
      capacity,
      manager: getUserDisplayName(selectedManager),
      devices: dialogMode.value === 'edit' ? labs.value.find((lab) => lab.id === formModel.id)?.devices || 0 : 0,
      features: splitFeatures(formModel.featuresText),
      schedule:
        dialogMode.value === 'edit'
          ? labs.value.find((lab) => lab.id === formModel.id)?.schedule
          : ['今日暂无排课'],
    })
  }

  const selectLab = (lab) => {
    selectedLabId.value = lab.id
  }

  const openCreateDialog = () => {
    dialogMode.value = 'create'
    assignFormModel()
    dialogVisible.value = true
  }

  const openEditDialog = (lab = selectedLab.value) => {
    if (!lab) return
    dialogMode.value = 'edit'
    assignFormModel(lab)
    dialogVisible.value = true
  }

  const submitLab = async () => {
    if (!validateForm()) return

    formSubmitting.value = true
    const nextLab = normalizeFormLab()
    const previousLabs = [...labs.value]

    try {
      if (dialogMode.value === 'create') {
        const createdLab = await labResourceService.create(toServerLabPayload(nextLab))
        const normalizedCreatedLab = normalizeLab({
          ...nextLab,
          ...createdLab,
          id: createdLab?.id || nextLab.id || `lab-${Date.now()}`,
        })
        labs.value.unshift(normalizedCreatedLab)
        selectedLabId.value = normalizedCreatedLab.id
      } else {
        labs.value = labs.value.map((lab) => (lab.id === nextLab.id ? nextLab : lab))
        selectedLabId.value = nextLab.id
        const updatedLab = await labResourceService.update(nextLab.id, toServerLabPayload(nextLab))
        labs.value = labs.value.map((lab) =>
          lab.id === nextLab.id ? normalizeLab({ ...nextLab, ...updatedLab, id: nextLab.id }) : lab,
        )
      }

      dialogVisible.value = false
      ElMessage.success(`${dialogMode.value === 'create' ? '新增' : '更新'}实验室成功`)
    } catch (error) {
      labs.value = previousLabs
      if (!error?.__handled) {
        ElMessage.error(error?.message || '保存实验室失败')
      }
    } finally {
      formSubmitting.value = false
    }
  }

  const deleteLab = async (lab = selectedLab.value) => {
    if (!lab) return

    try {
      await ElMessageBox.confirm(`确认删除“${lab.name}”吗？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    const previousLabs = [...labs.value]

    try {
      labs.value = labs.value.filter((item) => item.id !== lab.id)
      selectedLabId.value = labs.value[0]?.id || ''
      const nextLabs = await labResourceService.remove(lab.id)
      labs.value = nextLabs.map(normalizeLab)
      selectedLabId.value = labs.value[0]?.id || ''
    } catch (error) {
      labs.value = previousLabs
      selectedLabId.value = previousLabs[0]?.id || ''
      if (!error?.__handled) {
        ElMessage.error(error?.message || '删除实验室失败')
      }
      return
    }

    ElMessage.success('已删除实验室')
  }

  const changeLabStatus = async (status) => {
    if (!selectedLab.value) return

    labs.value = labs.value.map((lab) =>
      lab.id === selectedLab.value.id
        ? {
            ...lab,
            status,
            tagType: getStatusType(status),
          }
        : lab,
    )
    await persistLabs()
    ElMessage.success(`${selectedLab.value.name} 已调整为 ${status}`)
  }

  const bindLabSchedules = (records = [], context = {}, reservations = currentReservationRecords.value) => {
    currentScheduleRecords.value = Array.isArray(records) ? records : []
    currentReservationRecords.value = Array.isArray(reservations) ? reservations : []
    currentScheduleContext.value = { ...context }
    const nextScheduleMap = {}

    labs.value = labs.value.map((lab) => {
      const matchedRows = currentScheduleRecords.value.filter((record) => isRecordForLab(record, lab))
      const visibleRows = sortTimetableRows(
        dedupeTimetableRows(matchedRows.filter((record) => isRecordInScheduleContext(record, context))),
      ).map((record) => ({
        ...record,
        timeText: getRecordTimeText(record),
      }))
      nextScheduleMap[String(lab.id)] = visibleRows

      const statusRows = sortTimetableRows(
        dedupeTimetableRows(
          matchedRows.filter((record) =>
            isRecordInScheduleContext(record, {
              week: context.week,
              weekday: context.weekday,
            }),
          ),
        ),
      )
      const visibleReservations = currentReservationRecords.value
        .filter((reservation) => isReservationApproved(reservation))
        .filter((reservation) => isReservationForLab(reservation, lab))
        .filter((reservation) => isReservationForDate(reservation, context.date))
        .sort((left, right) => {
          const leftRange = getReservationTimeRange(left)
          const rightRange = getReservationTimeRange(right)
          return (leftRange?.start || 0) - (rightRange?.start || 0)
        })
      const scheduleItems = [
        ...visibleRows.map(formatScheduleItem),
        ...visibleReservations.map(formatReservationItem),
      ].filter(Boolean)
      const schedule = scheduleItems.length ? scheduleItems.slice(0, 6) : ['今日暂无排课']
      const currentMinute = Number(context.currentMinute || 0)
      const shouldAutoStatus = context.isToday !== false && !lockedScheduleStatuses.has(lab.status)
      const hasActiveCourse =
        shouldAutoStatus && currentMinute >= 0 && statusRows.some((record) => isRecordInBusyWindow(record, currentMinute))
      const hasActiveReservation =
        shouldAutoStatus &&
        currentMinute >= 0 &&
        visibleReservations.some((reservation) => isReservationInBusyWindow(reservation, currentMinute))
      const nextStatus = lockedScheduleStatuses.has(lab.status)
        ? lab.status
        : shouldAutoStatus
          ? hasActiveCourse || hasActiveReservation
            ? busyStatus
            : openStatus
          : lab.status

      return {
        ...lab,
        schedule,
        scheduleCount: visibleRows.length,
        status: nextStatus,
        tagType: getStatusType(nextStatus),
      }
    })

    labScheduleMap.value = nextScheduleMap
  }

  const resetLabs = async () => {
    try {
      await ElMessageBox.confirm('确认恢复实验室初始数据吗？', '恢复数据', {
        type: 'info',
        confirmButtonText: '恢复',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    keyword.value = ''
    buildingFilter.value = 'all'
    statusFilter.value = 'all'
    typeFilter.value = 'all'
    availableOnly.value = false
    const resetRecords = await labResourceService.reset({
      fallbackRecords: cloneLabs(),
    })
    labs.value = resetRecords.map(normalizeLab)
    selectedLabId.value = labs.value[0]?.id || ''
    ElMessage.success('已恢复初始实验室数据')
  }

  const exportLabs = () => {
    const exportRows = filteredLabs.value

    if (!exportRows.length) {
      ElMessage.warning('暂无可导出的实验室数据')
      return
    }

    const exportedCount = exportTableToExcel({
      filename: '实验室清单',
      sheetName: '实验室清单',
      rows: exportRows,
      columns: [
        { label: '编号', prop: 'code' },
        { label: '名称', prop: 'name' },
        { label: '楼宇', prop: 'building' },
        { label: '楼层', prop: 'floor' },
        { label: '房间', prop: 'room' },
        { label: '容量', prop: 'capacity' },
        { label: '负责人', prop: 'manager' },
        { label: '类型', prop: 'type' },
        { label: '状态', prop: 'status' },
        { label: '设备数量', prop: 'devices' },
        { label: '开放时间', prop: 'openTime' },
        { label: '安全状态', prop: 'safety' },
        { label: '实验室能力', prop: 'features' },
        { label: '今日安排', prop: 'schedule' },
      ],
    })
    ElMessage.success(`已导出 ${exportedCount} 条实验室数据`)
  }

  watch(filteredLabs, (nextLabs) => {
    if (!nextLabs.length) return
    if (!nextLabs.some((lab) => lab.id === selectedLabId.value)) {
      selectedLabId.value = nextLabs[0].id
    }
  })

  void loadManagerOptions().then(loadLabs)

  return {
    availableOnly,
    bindLabSchedules,
    buildingFilter,
    buildingOptions,
    changeLabStatus,
    deleteLab,
    dialogTitle,
    dialogVisible,
    exportLabs,
    filteredLabs,
    formModel,
    formSubmitting,
    keyword,
    managerOptions,
    openCreateDialog,
    openEditDialog,
    resetLabs,
    selectLab,
    selectedLab,
    selectedLabId,
    safetyOptions: SAFETY_OPTIONS,
    statusFilter,
    statusOptions: STATUS_OPTIONS,
    statusSummary,
    submitLab,
    typeFilter,
    typeOptions: TYPE_OPTIONS,
  }
}
