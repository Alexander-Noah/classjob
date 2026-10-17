/*
 * 课表与时间占用的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'

const academicWeekUrl = new URL('../src/utils/academicWeek.js', import.meta.url)
const labManagementUrl = new URL('../src/views/labs/LabManagement.vue', import.meta.url)
const timetableAdminUrl = new URL('../src/views/labs/LabAdminClassTimetable.vue', import.meta.url)
const teachingCalendarUrl = new URL('../src/modules/teaching-calendar/teachingCalendar.js', import.meta.url)
const calendarConsumers = [
  new URL('../src/views/teacher/TeacherSchedule.vue', import.meta.url),
  new URL('../src/views/roles/TeacherPortal.vue', import.meta.url),
  new URL('../src/views/reservations/ScanReservation.vue', import.meta.url),
  new URL('../src/composables/useReservationManagement.js', import.meta.url),
  timetableAdminUrl,
]

test('calculates the current teaching week from the configured semester start date', /** 验证当前测试用例的预期行为。 */ async () => {
  assert.ok(existsSync(academicWeekUrl), 'academic week utility must exist')

  const { calculateAcademicWeek } = await import(academicWeekUrl.href)

  assert.equal(calculateAcademicWeek(new Date('2026-10-08T15:59:01'), '2026-08-31'), 6)
})

test('crawler configuration stores semester and semester start as one calendar selection', /** 验证当前测试用例的预期行为。 */ () => {
  const source = readFileSync(timetableAdminUrl, 'utf8')

  assert.match(source, /v-model="configForm\.semester"/)
  assert.match(source, /semester:\s*configForm\.semester/)
  assert.match(source, /semesterStartDate:\s*configForm\.semesterStartDate/)
})

test('lab management loads the authoritative semester start date before timetable rows', /** 验证当前测试用例的预期行为。 */ () => {
  const source = readFileSync(labManagementUrl, 'utf8')

  assert.match(source, /getClassTimetableCrawlerConfig/)
  assert.doesNotMatch(source, /const semesterStartDate = ['"]2026-03-02['"]/)
  assert.match(source, /await loadScheduleConfiguration\(\)/)
  assert.match(source, /await loadScheduleRows\(\)/)
})

test('all timetable consumers use the shared teaching calendar instead of a fixed semester date', /** 验证当前测试用例的预期行为。 */ () => {
  assert.ok(existsSync(teachingCalendarUrl), 'teaching calendar module must exist')

  for (const consumerUrl of calendarConsumers) {
    const source = readFileSync(consumerUrl, 'utf8')
    assert.doesNotMatch(source, /2026-03-02/, `${consumerUrl.pathname} must not pin a semester start date`)
  }

  const moduleSource = readFileSync(teachingCalendarUrl, 'utf8')
  assert.match(moduleSource, /getClassTimetableCrawlerConfig/)
  assert.match(moduleSource, /calculateAcademicWeek/)
})
