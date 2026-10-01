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

const parseTimePart = (value = '') => {
  const match = String(value).match(/(\d{1,2})[:：](\d{2})/)
  if (!match) return null

  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return null

  return { hours, minutes }
}

const toDateOnBaseDay = (timePart, baseDate) => {
  if (!timePart) return null
  const date = new Date(baseDate)
  date.setHours(timePart.hours, timePart.minutes, 0, 0)
  return date
}

const formatDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

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

const getItemDateText = (item = {}) => {
  if (typeof item === 'string') return ''
  return item.date || item.reservationDate || item.reservation_date || item.scheduleDate || item.day || ''
}

const isTodayItem = (item = {}, now = new Date()) => {
  const dateText = getItemDateText(item)
  if (!dateText) return true
  return String(dateText).slice(0, 10).replaceAll('/', '-') === formatDateKey(now)
}

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

export const parseScheduleTimeRange = (timeText, now = new Date()) => parseScheduleTime(timeText, now)

const formatTimePart = (date) =>
  date ? `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}` : '待确认'

const formatScheduleTime = (item, start, end) => {
  if (start && end) return `${formatTimePart(start)}-${formatTimePart(end)}`
  return getRangeText(item) || '待确认'
}

export const getMinutesUntilStart = (item = {}, now = new Date()) => {
  const { start } = parseScheduleTime(item, now)
  if (!start) return Number.POSITIVE_INFINITY
  return Math.floor((start.getTime() - now.getTime()) / 60000)
}

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

export const isUsingNow = (item = {}, now = new Date()) => getScheduleStatus(item, now).group === 'ongoing'

export const isNeedOpenDoor = (item = {}, now = new Date()) => {
  if (!isTodayItem(item, now)) return false
  const { start, end } = parseScheduleTime(item, now)
  if (!start || !end || now > end) return false
  const minutesUntilStart = (start.getTime() - now.getTime()) / 60000
  return minutesUntilStart > 0 && minutesUntilStart <= SOON_MINUTES
}

const extractLabCode = (value = '') => {
  const text = String(value || '').trim()
  const match = text.match(/([A-Za-z]?\d+[A-Za-z]?(?:-\d+)?|\d+-\d+)/)
  return match?.[1] || text || '-'
}

const extractApplicant = (item = {}) => {
  const directApplicant = item.applicant || item.applicantName || item.userName || item.contactName || item.person
  if (directApplicant) return directApplicant

  const match = String(item.desc || item.description || '').match(/(?:预约人|申请人)[:：]\s*([^，,。；;\s]+)/)
  return match?.[1] || ''
}

const extractReminderTitle = (item = {}) => {
  const directTitle = item.course || item.courseName || item.reason || item.purpose || item.scene
  if (directTitle) return directTitle

  const desc = String(item.desc || item.description || '')
  const beforeRange = desc.split(/\d{1,2}[:：]\d{2}\s*[-~—至]\s*\d{1,2}[:：]\d{2}/)[0]?.trim()
  if (beforeRange) return beforeRange

  return item.title || item.name || '未命名安排'
}

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

export const buildDashboardScheduleView = (schedules = [], now = new Date(), options = {}) => {
  const limit = Number(options.limit || DEFAULT_LIMIT)
  const todayItems = (Array.isArray(schedules) ? schedules : []).filter((item) => isTodayItem(item, now))
  const activeItems = todayItems
    .map((item, index) => normalizeScheduleItem(item, index, now))
    .filter((item) => item.group !== 'ended')
    .sort((first, second) => {
      const groupDiff = (SCHEDULE_GROUP_ORDER[first.group] || 99) - (SCHEDULE_GROUP_ORDER[second.group] || 99)
      return groupDiff || first.startAt - second.startAt
    })

  const visibleItems = activeItems.slice(0, limit)
  const sections = SECTION_DEFS.map((section) => ({
    ...section,
    items: visibleItems.filter((item) => item.group === section.key),
  })).filter((section) => section.items.length > 0)

  return {
    sections,
    visibleItems,
    hasMore: todayItems.length > limit || activeItems.length > visibleItems.length,
    totalActive: activeItems.length,
    totalToday: todayItems.length,
  }
}

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

export const buildDoorReminderList = (schedules = [], reservations = [], now = new Date(), options = {}) => {
  const limit = Number(options.limit || DOOR_REMINDER_LIMIT)
  const scheduleItems = Array.isArray(schedules) ? schedules : []
  const reservationItems = Array.isArray(reservations) ? reservations : []

  const activeItems = [
    ...scheduleItems.map((item, index) => normalizeDoorReminderItem(item, index, now, 'schedule')),
    ...reservationItems.map((item, index) => normalizeDoorReminderItem(item, index, now, 'reservation')),
  ]
    .filter(Boolean)
    .sort((first, second) => first.startAt - second.startAt)

  return {
    visibleItems: activeItems.slice(0, limit),
    hasMore: activeItems.length > limit,
    totalActive: activeItems.length,
  }
}
