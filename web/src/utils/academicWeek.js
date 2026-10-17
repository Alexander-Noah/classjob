/*
 * academicWeek公共逻辑模块，供相关视图或服务复用。
 * 主要导出：calculateAcademicWeek。
 */
const DATE_TEXT_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const DAY_MILLISECONDS = 24 * 60 * 60 * 1000

// 使用年月日构造本地日期，避免直接解析 YYYY-MM-DD 时发生 UTC 与本地日期偏移。
const parseLocalDate = (value) => {
  const match = DATE_TEXT_PATTERN.exec(String(value || '').trim())
  if (!match) return null

  const year = Number(match[1])
  const monthIndex = Number(match[2]) - 1
  const day = Number(match[3])
  const date = new Date(year, monthIndex, day)
  if (date.getFullYear() !== year || date.getMonth() !== monthIndex || date.getDate() !== day) return null
  return date
}

/** 开学日起每七天计一周，结果限制在 1..maximumWeek；无有效配置时显示第一周。 */
export const calculateAcademicWeek = (date, semesterStartDate, maximumWeek = 25) => {
  const currentDate = date instanceof Date && !Number.isNaN(date.getTime()) ? date : null
  const startDate = parseLocalDate(semesterStartDate)
  if (!currentDate || !startDate) return 1

  const currentDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate())
  const diffDays = Math.floor((currentDay.getTime() - startDate.getTime()) / DAY_MILLISECONDS)
  return Math.min(maximumWeek, Math.max(1, Math.floor(diffDays / 7) + 1))
}
