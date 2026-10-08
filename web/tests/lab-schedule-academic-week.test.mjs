import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'

const academicWeekUrl = new URL('../src/utils/academicWeek.js', import.meta.url)
const labManagementUrl = new URL('../src/views/labs/LabManagement.vue', import.meta.url)

test('calculates the current teaching week from the configured semester start date', async () => {
  assert.ok(existsSync(academicWeekUrl), 'academic week utility must exist')

  const { calculateAcademicWeek } = await import(academicWeekUrl.href)

  assert.equal(calculateAcademicWeek(new Date('2026-10-08T15:59:01'), '2026-08-31'), 6)
})

test('lab management loads the authoritative semester start date before timetable rows', () => {
  const source = readFileSync(labManagementUrl, 'utf8')

  assert.match(source, /getClassTimetableCrawlerConfig/)
  assert.doesNotMatch(source, /const semesterStartDate = ['"]2026-03-02['"]/)
  assert.match(source, /await loadScheduleConfiguration\(\)/)
  assert.match(source, /await loadScheduleRows\(\)/)
})
