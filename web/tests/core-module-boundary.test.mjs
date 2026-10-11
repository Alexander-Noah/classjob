import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('router exposes only approved core modules', () => {
  const modules = read('../src/constants/routeModules.js')
  const router = read('../src/router/index.js')
  const combined = `${modules}\n${router}`

  for (const retired of [
    'ai-assistant',
    'consumables',
    'environments',
    'room-control',
    'device-status',
    'activation-codes',
    'course-environments',
    'system-admin/system',
  ]) {
    assert.doesNotMatch(combined, new RegExp(retired), retired)
  }

  for (const core of [
    'labs',
    'devices',
    'reservations',
    'maintenance-records',
    'records',
    'statistics',
    'notices',
    'class-timetable',
    'users',
    'schedule',
  ]) {
    assert.match(combined, new RegExp(core), core)
  }
})

test('public scan reservation remains public', () => {
  const router = read('../src/router/index.js')
  assert.match(router, /path: '\/scan\/reserve'/)
  assert.match(router, /title: '扫码预约',\s*public: true/)
})

test('retired frontend pages, services, and composables are removed', () => {
  const retiredFiles = [
    '../src/views/ai-assistant/AiAssistant.vue',
    '../src/views/consumables/ConsumableInventory.vue',
    '../src/views/device-status/DeviceStatusPage.vue',
    '../src/views/environments/EnvironmentManagement.vue',
    '../src/views/environments/components/CourseEnvironmentDialog.vue',
    '../src/views/environments/components/EnvironmentTemplateDialog.vue',
    '../src/views/environments/components/SoftwareEnvironmentDialog.vue',
    '../src/views/room-control/RoomControlCenter.vue',
    '../src/views/room-control/components/DeviceControlPanel.vue',
    '../src/views/room-control/components/EventRecordPanel.vue',
    '../src/views/room-control/components/LabSafetyPanel.vue',
    '../src/views/room-control/components/LabSelectorPanel.vue',
    '../src/views/room-control/components/RoomControlHeader.vue',
    '../src/views/system/ActivationCodeManagement.vue',
    '../src/views/system/SystemSettings.vue',
    '../src/views/teacher/TeacherAdjustments.vue',
    '../src/views/teacher/TeacherCourseEnvironments.vue',
    '../src/views/teacher/components/TeacherAdjustmentDialog.vue',
    '../src/views/teacher/components/TeacherEnvironmentDrawer.vue',
    '../src/views/teacher/components/TeacherEnvironmentRequestDialog.vue',
    '../src/api/services/activationCodes.js',
    '../src/api/services/aiAssistantConfig.js',
    '../src/api/services/aiAssistantHistory.js',
    '../src/api/services/deviceActivation.js',
    '../src/api/services/deviceStatus.js',
    '../src/api/services/iot.js',
    '../src/api/services/scheduleAdjustments.js',
    '../src/api/services/systemSettings.js',
    '../src/composables/useBusinessModuleRecords.js',
    '../src/composables/useRoomControl.js',
  ]

  for (const file of retiredFiles) {
    assert.equal(existsSync(new URL(file, import.meta.url)), false, file)
  }
})
