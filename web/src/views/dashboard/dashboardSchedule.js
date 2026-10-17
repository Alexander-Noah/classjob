/*
 * 课表与时间占用公共逻辑模块，供相关视图或服务复用。
 * 主要导出：parseScheduleTime、parseScheduleTimeRange、getMinutesUntilStart、getScheduleStatus、isUsingNow 等。
 */
const DEFAULT_LIMIT = 8
const SOON_MINUTES = 15
const DOOR_REMINDER_LIMIT = 5

const SECTION_DEFS = [
  { key: 'ongoing', title: '正在进行' },
  { key: 'soon', title: '即将开始' },
  { key: 'later', title: '今日后续安排' },
]

const SCHEDULE_GROUP_ORDER = {
  ongoing: 1,
  soon: 2,
  later: 3,
  ended: 4,
}

/**
 * 解析时间Part。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseTimePart = (value = '') => {
  const match = String(value).match(/(\d{1,2})[:：](\d{2})/)
  if (!match) return null

  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return null

  return { hours, minutes }
}

/**
 * 转换为日期On基础Day。
 * 入参：timePart（时间Part）；baseDate（基础日期）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const toDateOnBaseDay = (timePart, baseDate) => {
  if (!timePart) return null
  const date = new Date(baseDate)
  date.setHours(timePart.hours, timePart.minutes, 0, 0)
  return date
}

/**
 * 格式化日期键。
 * 入参：date（日期）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 解析日期文本。
 * 入参：dateText（日期文本）；fallbackDate（fallback日期）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseDateText = (dateText, fallbackDate) => {
  const match = String(dateText || '').match(/(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
  if (!match) return new Date(fallbackDate)

  const year = Number(match[1])
  const month = Number(match[2]) - 1
  const day = Number(match[3])
  const date = new Date(fallbackDate)
  date.setFullYear(year, month, day)
  date.setHours(0, 0, 0, 0)
  return date
}

/**
 * 获取条目日期文本。
 * 入参：item（条目）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getItemDateText = (item = {}) => {
  if (typeof item === 'string') return ''
  return item.date || item.reservationDate || item.reservation_date || item.scheduleDate || item.day || ''
}

/**
 * 判断是否为Today条目。
 * 入参：item（条目）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const isTodayItem = (item = {}, now = new Date()) => {
  const dateText = getItemDateText(item)
  if (!dateText) return true
  return String(dateText).slice(0, 10).replaceAll('/', '-') === formatDateKey(now)
}

/**
 * 获取范围文本。
 * 入参：item（条目）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getRangeText = (item = {}) => {
  if (typeof item === 'string') return item
  if (item.startTime && item.endTime) return `${item.startTime}-${item.endTime}`

  const directRange = item.timeRange || item.time || item.period || item.timeSlot || item.sectionText
  if (String(directRange || '').includes('-')) return directRange

  const descRange = String(item.desc || item.description || '').match(
    /(\d{1,2}[:：]\d{2})\s*[-~—至]\s*(\d{1,2}[:：]\d{2})/,
  )
  if (descRange) return `${descRange[1]}-${descRange[2]}`

  return directRange || ''
}

/**
 * 解析课表时间。
 * 入参：item（条目）；now（now）。
 * @returns 组装的键值对象。
 */
export const parseScheduleTime = (item = {}, now = new Date()) => {
  const [startText, endText] = String(getRangeText(item)).split(/\s*[-~—至]\s*/)
  const baseDate = parseDateText(getItemDateText(item), now)
  const start = toDateOnBaseDay(parseTimePart(startText), baseDate)
  const end = toDateOnBaseDay(parseTimePart(endText), baseDate)

  if (start && end && end < start) {
    end.setDate(end.getDate() + 1)
  }

  return { start, end }
}

/**
 * 解析课表时间范围。
 * 入参：timeText（时间文本）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const parseScheduleTimeRange = (timeText, now = new Date()) => parseScheduleTime(timeText, now)

/**
 * 格式化时间Part。
 * 入参：date（日期）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatTimePart = (date) =>
  date ? `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}` : '待确认'

/**
 * 格式化课表时间。
 * 入参：item（条目）；start（开始）；end（结束）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatScheduleTime = (item, start, end) => {
  if (start && end) return `${formatTimePart(start)}-${formatTimePart(end)}`
  return getRangeText(item) || '待确认'
}

/**
 * 获取MinutesUntil开始。
 * 入参：item（条目）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getMinutesUntilStart = (item = {}, now = new Date()) => {
  const { start } = parseScheduleTime(item, now)
  if (!start) return Number.POSITIVE_INFINITY
  return Math.floor((start.getTime() - now.getTime()) / 60000)
}

/**
 * 获取课表状态。
 * 入参：item（条目）；now（now）。
 * @returns 组装的键值对象。
 */
export const getScheduleStatus = (item = {}, now = new Date()) => {
  const { start, end } = parseScheduleTime(item, now)
  if (!start || !end) {
    return { status: '未开始', statusType: 'info', group: 'later' }
  }

  if (now > end) {
    return { status: '已结束', statusType: 'info', group: 'ended' }
  }

  if (now >= start && now <= end) {
    return { status: '进行中', statusType: 'success', group: 'ongoing' }
  }

  const minutesUntilStart = getMinutesUntilStart(item, now)
  if (minutesUntilStart >= 0 && minutesUntilStart <= SOON_MINUTES) {
    return { status: '即将开始', statusType: 'warning', group: 'soon' }
  }

  return { status: '未开始', statusType: 'info', group: 'later' }
}

/**
 * 判断是否为UsingNow。
 * 入参：item（条目）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const isUsingNow = (item = {}, now = new Date()) => getScheduleStatus(item, now).group === 'ongoing'

/**
 * 判断是否为NeedOpenDoor。
 * 入参：item（条目）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const isNeedOpenDoor = (item = {}, now = new Date()) => {
  if (!isTodayItem(item, now)) return false
  const { start, end } = parseScheduleTime(item, now)
  if (!start || !end || now > end) return false
  const minutesUntilStart = (start.getTime() - now.getTime()) / 60000
  return minutesUntilStart > 0 && minutesUntilStart <= SOON_MINUTES
}

/**
 * 提取实验室编码。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const extractLabCode = (value = '') => {
  const text = String(value || '').trim()
  const match = text.match(/([A-Za-z]?\d+[A-Za-z]?(?:-\d+)?|\d+-\d+)/)
  return match?.[1] || text || '-'
}

/**
 * 提取Applicant。
 * 入参：item（条目）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const extractApplicant = (item = {}) => {
  const directApplicant = item.applicant || item.applicantName || item.userName || item.contactName || item.person
  if (directApplicant) return directApplicant

  const match = String(item.desc || item.description || '').match(/(?:预约人|申请人)[:：]\s*([^，,。；;\s]+)/)
  return match?.[1] || ''
}

/**
 * 提取ReminderTitle。
 * 入参：item（条目）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const extractReminderTitle = (item = {}) => {
  const directTitle = item.course || item.courseName || item.reason || item.purpose || item.scene
  if (directTitle) return directTitle

  const desc = String(item.desc || item.description || '')
  const beforeRange = desc.split(/\d{1,2}[:：]\d{2}\s*[-~—至]\s*\d{1,2}[:：]\d{2}/)[0]?.trim()
  if (beforeRange) return beforeRange

  return item.title || item.name || '未命名安排'
}

/**
 * 归一化课表条目。
 * 入参：item（条目）；index（索引）；now（now）。
 * @returns 组装的键值对象。
 */
const normalizeScheduleItem = (item, index, now) => {
  const { start, end } = parseScheduleTime(item, now)
  const statusMeta = getScheduleStatus(item, now)

  return {
    id: item.id || `${formatScheduleTime(item, start, end)}-${item.lab || item.roomNo || item.classroom || 'lab'}-${index}`,
    time: formatScheduleTime(item, start, end),
    lab: item.lab || item.roomNo || item.labCode || item.classroom || '-',
    course: item.course || item.courseName || item.title || item.name || '未命名安排',
    className: item.className || item.class || item.studentClass || item.department || '-',
    teacher: item.teacher || item.teacherName || item.person || '-',
    startAt: start?.getTime() || Number.MAX_SAFE_INTEGER,
    endAt: end?.getTime() || Number.MAX_SAFE_INTEGER,
    ...statusMeta,
  }
}

/**
 * 构建Dashboard课表视图。
 * 入参：schedules（课表）；now（now）；options（选项）。
 * @returns 组装的键值对象。
 */
export const buildDashboardScheduleView = (schedules = [], now = new Date(), options = {}) => {
  const limit = Number(options.limit || DEFAULT_LIMIT)
  const todayItems = (Array.isArray(schedules) ? schedules : []).filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => isTodayItem(item, now))
  const activeItems = todayItems
    .map(/** 将当前条目转换为目标结果。 入参：item、index。 */ (item, index) => normalizeScheduleItem(item, index, now))
    .filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.group !== 'ended')
    .sort(/** 比较当前两项的排序先后。 入参：first、second。 */ (first, second) => {
      const groupDiff = (SCHEDULE_GROUP_ORDER[first.group] || 99) - (SCHEDULE_GROUP_ORDER[second.group] || 99)
      return groupDiff || first.startAt - second.startAt
    })

  const visibleItems = activeItems.slice(0, limit)
  const sections = SECTION_DEFS.map(/** 将当前条目转换为目标结果。 入参：section。 */ (section) => ({
    ...section,
    items: visibleItems.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.group === section.key),
  })).filter(/** 判断当前条目是否符合筛选条件。 入参：section。 */ (section) => section.items.length > 0)

  return {
    sections,
    visibleItems,
    hasMore: todayItems.length > limit || activeItems.length > visibleItems.length,
    totalActive: activeItems.length,
    totalToday: todayItems.length,
  }
}

/**
 * 获取DoorReminder状态。
 * 入参：item（条目）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getDoorReminderStatus = (item = {}, now = new Date()) => {
  if (!isTodayItem(item, now)) return null

  const { start, end } = parseScheduleTime(item, now)
  if (!start || !end || now > end) return null

  if (now >= start && now <= end) {
    return { status: '使用中', statusType: 'success' }
  }

  if (isNeedOpenDoor(item, now)) {
    return { status: '待开门', statusType: 'warning' }
  }

  return null
}

/**
 * 归一化DoorReminder条目。
 * 入参：item（条目）；index（索引）；now（now）；source（来源）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeDoorReminderItem = (item, index, now, source) => {
  const statusMeta = getDoorReminderStatus(item, now)
  if (!statusMeta) return null

  const { start, end } = parseScheduleTime(item, now)
  const applicant = extractApplicant(item)
  const isReservation = source === 'reservation'
  const lab = extractLabCode(item.labCode || item.roomNo || item.lab || item.classroom)

  return {
    id: item.id || `${source}-${formatTimePart(start)}-${lab}-${index}`,
    lab,
    time: `${formatTimePart(start)}-${formatTimePart(end)}`,
    title: extractReminderTitle(item),
    audience: item.className || item.class || item.studentClass || item.department || applicant || '-',
    person: item.teacher || item.teacherName || applicant || '-',
    path: item.path || (isReservation ? '/lab-admin/reservations' : '/lab-admin/class-timetable-data'),
    startAt: start.getTime(),
    ...statusMeta,
  }
}

/**
 * 构建DoorReminder列表。
 * 入参：schedules（课表）；reservations（预约）；now（now）；options（选项）。
 * @returns 组装的键值对象。
 */
export const buildDoorReminderList = (schedules = [], reservations = [], now = new Date(), options = {}) => {
  const limit = Number(options.limit || DOOR_REMINDER_LIMIT)
  const scheduleItems = Array.isArray(schedules) ? schedules : []
  const reservationItems = Array.isArray(reservations) ? reservations : []

  const activeItems = [
    ...scheduleItems.map(/** 将当前条目转换为目标结果。 入参：item、index。 */ (item, index) => normalizeDoorReminderItem(item, index, now, 'schedule')),
    ...reservationItems.map(/** 将当前条目转换为目标结果。 入参：item、index。 */ (item, index) => normalizeDoorReminderItem(item, index, now, 'reservation')),
  ]
    .filter(Boolean)
    .sort(/** 比较当前两项的排序先后。 入参：first、second。 */ (first, second) => first.startAt - second.startAt)

  return {
    visibleItems: activeItems.slice(0, limit),
    hasMore: activeItems.length > limit,
    totalActive: activeItems.length,
  }
}
