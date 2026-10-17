/*
 * 教学日历公共逻辑模块，供相关视图或服务复用。
 * 主要导出：MAXIMUM_ACADEMIC_WEEK、normalizeTeachingCalendar、loadTeachingCalendar、getAcademicWeek、getTeachingWeekStartDate。
 */
import { getClassTimetableCrawlerConfig } from '../../api/services/classTimetable'
import { calculateAcademicWeek } from '../../utils/academicWeek'

export const MAXIMUM_ACADEMIC_WEEK = 25

/**
 * 解析本地日期。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseLocalDate = (value) => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || '').trim())
  if (!match) return null

  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  return Number.isNaN(date.getTime()) ? null : date
}

/**
 * 归一化TeachingCalendar。
 * 入参：config（配置）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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

/**
 * 加载TeachingCalendar。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const loadTeachingCalendar = async () =>
  normalizeTeachingCalendar(await getClassTimetableCrawlerConfig())

/**
 * 获取教务周次。
 * 入参：calendar（calendar）；date（日期）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getAcademicWeek = (calendar, date = new Date()) =>
  calculateAcademicWeek(date, calendar?.semesterStartDate, calendar?.maximumWeek || MAXIMUM_ACADEMIC_WEEK)

/**
 * 获取Teaching周次开始日期。
 * 入参：calendar（calendar）；week（周次）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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
