import { readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('core role portals keep retained workflows visible', () => {
  const portalSources = [
    read('../src/views/roles/LabAdminPortal.vue'),
    read('../src/views/roles/SystemAdminPortal.vue'),
    read('../src/views/roles/TeacherPortal.vue'),
    read('../src/constants/rolePortals.js'),
  ].join('\n')
  const routeSources = [
    read('../src/constants/routeModules.js'),
    read('../src/router/index.js'),
  ].join('\n')

  for (const core of [
    'maintenance-records',
    'notices',
    'statistics',
    'class-timetable',
    'schedule',
    'users',
  ]) {
    assert.match(routeSources, new RegExp(core), core)
  }

  for (const retired of [
    'consumables',
    'ai-assistant',
    'room-control',
    'device-status',
    'course-environments',
    'activation-codes',
  ]) {
    assert.doesNotMatch(`${routeSources}\n${portalSources}`, new RegExp(retired), retired)
  }
})

test('teacher schedule remains a timetable-only page', () => {
  const schedule = read('../src/views/teacher/TeacherSchedule.vue')

  assert.match(schedule, /class-timetable|AcademicSchedule|schedule/i)
  for (const retired of [
    'TeacherAdjustments',
    'TeacherCourseEnvironments',
    'scheduleAdjustments',
    'course-environment',
    'environment-template',
  ]) {
    assert.doesNotMatch(schedule, new RegExp(retired, 'i'), retired)
  }
})

test('dashboard shortcuts do not target retired modules', () => {
  const dashboard = read('../src/constants/dashboard.js')

  for (const retired of [
    'consumables',
    'device-status',
    'room-control',
    'environments',
    'repairs',
  ]) {
    assert.doesNotMatch(dashboard, new RegExp(retired), retired)
  }
})
