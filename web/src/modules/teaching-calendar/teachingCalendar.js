/*
 * 教学日历公共逻辑模块，供相关视图或服务复用。
 * 主要导出：MAXIMUM_ACADEMIC_WEEK、normalizeTeachingCalendar、loadTeachingCalendar、getAcademicWeek、getTeachingWeekStartDate。
 */
import { getClassTimetableCrawlerConfig } from '../../api/services/classTimetable'
import { calculateAcademicWeek } from '../../utils/academicWeek'

export const MAXIMUM_ACADEMIC_WEEK = 25

const parseLocalDate = (value) => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || '').trim())
  if (!match) return null

  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  return Number.isNaN(date.getTime()) ? null : date
}

export const normalizeTeachingCalendar = (config = {}) => {
  const semester = String(config.semester || '').trim()
  const semesterStartDate = String(config.semesterStartDate || '').trim()
  if (!semester || !parseLocalDate(semesterStartDate)) {
    throw new Error('教学日历尚未配置，请先设置当前学期和学期开始日期')
  }

  return Object.freeze({
    semester,
    semesterStartDate,
    maximumWeek: MAXIMUM_ACADEMIC_WEEK,
  })
}

export const loadTeachingCalendar = async () =>
  normalizeTeachingCalendar(await getClassTimetableCrawlerConfig())

export const getAcademicWeek = (calendar, date = new Date()) =>
  calculateAcademicWeek(date, calendar?.semesterStartDate, calendar?.maximumWeek || MAXIMUM_ACADEMIC_WEEK)

export const getTeachingWeekStartDate = (calendar, week) => {
  const startDate = parseLocalDate(calendar?.semesterStartDate)
  if (!startDate) return null

  const normalizedWeek = Math.min(
    calendar?.maximumWeek || MAXIMUM_ACADEMIC_WEEK,
    Math.max(1, Number(week) || 1),
  )
  startDate.setDate(startDate.getDate() + (normalizedWeek - 1) * 7)
  return startDate
}
