const DATE_TEXT_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const DAY_MILLISECONDS = 24 * 60 * 60 * 1000

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

export const calculateAcademicWeek = (date, semesterStartDate, maximumWeek = 25) => {
  const currentDate = date instanceof Date && !Number.isNaN(date.getTime()) ? date : null
  const startDate = parseLocalDate(semesterStartDate)
  if (!currentDate || !startDate) return 1

  const currentDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate())
  const diffDays = Math.floor((currentDay.getTime() - startDate.getTime()) / DAY_MILLISECONDS)
  return Math.min(maximumWeek, Math.max(1, Math.floor(diffDays / 7) + 1))
}
