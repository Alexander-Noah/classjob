/*
 * frontend-service-boundaries.test的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('device asset endpoints are owned by the device asset service', /** 验证当前测试用例的预期行为。 */ () => {
  const basicResources = readSource('../src/api/services/basicResources.js')
  const deviceAssets = readSource('../src/api/services/deviceAssets.js')

  assert.doesNotMatch(basicResources, /deviceResourceService/)
  assert.doesNotMatch(basicResources, /API_ENDPOINTS\.devices/)
  assert.match(deviceAssets, /deviceAssetService/)
  assert.match(deviceAssets, /API_ENDPOINTS\.devices/)
})

test('feature pages read device assets through deviceAssetService only', /** 验证当前测试用例的预期行为。 */ () => {
  const rolePortal = readSource('../src/views/roles/components/RolePortalPage.vue')

  assert.match(rolePortal, /deviceAssetService/)
  assert.doesNotMatch(rolePortal, /deviceResourceService/)
})

test('retired repair-management frontend is removed from the runtime surface', /** 验证当前测试用例的预期行为。 */ () => {
  const basicResources = readSource('../src/api/services/basicResources.js')
  const rolePortal = readSource('../src/views/roles/components/RolePortalPage.vue')
  const obsoleteFiles = [
    '../src/composables/useRepairManagement.js',
    '../src/views/repairs/RepairManagement.vue',
    '../src/views/repairs/components/RepairDetailPanel.vue',
    '../src/views/repairs/components/RepairEditorDialog.vue',
    '../src/views/repairs/components/RepairFilterBar.vue',
    '../src/views/repairs/components/RepairListTable.vue',
    '../src/views/repairs/components/RepairManagementHeader.vue',
  ]

  assert.doesNotMatch(basicResources, /repairResourceService|RepairManagement/)
  assert.match(rolePortal, /listMaintenanceRecords/)
  assert.doesNotMatch(rolePortal, /repairResourceService/)
  for (const file of obsoleteFiles) {
    assert.equal(existsSync(new URL(file, import.meta.url)), false, file)
  }
})

test('device asset feature stays separate from runtime device status ownership', /** 验证当前测试用例的预期行为。 */ () => {
  const deviceManagement = readSource('../src/views/devices/DeviceManagement.vue')
  const deviceManagementComposable = readSource('../src/composables/useDeviceManagement.js')
  const deviceStatus = readSource('../src/views/device-status/DeviceStatusPage.vue')
  const deviceAssets = readSource('../src/api/services/deviceAssets.js')

  assert.match(deviceManagement, /useDeviceManagement/)
  assert.match(deviceManagementComposable, /deviceAssetService/)
  assert.doesNotMatch(deviceManagementComposable, /iotService|runtimeStatus/)
  assert.doesNotMatch(deviceStatus, /deviceAssetService/)
  assert.doesNotMatch(deviceAssets, /iot|runtimeStatus/)
})

test('status, iot and activation services do not call device asset endpoints', /** 验证当前测试用例的预期行为。 */ () => {
  const serviceSources = [
    readSource('../src/api/services/deviceStatus.js'),
    readSource('../src/api/services/iot.js'),
    readSource('../src/api/services/deviceActivation.js'),
  ].join('\n')

  assert.doesNotMatch(serviceSources, /API_ENDPOINTS\.devices/)
  assert.doesNotMatch(serviceSources, /['"`]\/devices/)
})

test('legacy iot hardware service is removed from frontend api surface', /** 验证当前测试用例的预期行为。 */ () => {
  const legacyService = new URL('../src/api/iotHardware.js', import.meta.url)

  assert.equal(existsSync(legacyService), false)
})

test('dashboard and permission labels keep asset, runtime and access authorization concepts separated', /** 验证当前测试用例的预期行为。 */ () => {
  const dashboard = readSource('../src/constants/dashboard.js')
  const permissions = readSource('../src/constants/permissions.js')
  const rolePortals = readSource('../src/constants/rolePortals.js')

  assert.doesNotMatch(dashboard, /资产台账与设备状态/)
  assert.doesNotMatch(rolePortals, /资产台账与设备状态/)
  assert.match(dashboard, /资产台账与生命周期/)
  assert.match(permissions, /接入授权:\s*\['\/lab-admin\/device-status'\]/)
  assert.match(permissions, /设备绑定:\s*\['\/lab-admin\/device-status'\]/)
})
