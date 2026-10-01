import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('device asset endpoints are owned by the device asset service', () => {
  const basicResources = readSource('../src/api/services/basicResources.js')
  const deviceAssets = readSource('../src/api/services/deviceAssets.js')

  assert.doesNotMatch(basicResources, /deviceResourceService/)
  assert.doesNotMatch(basicResources, /API_ENDPOINTS\.devices/)
  assert.match(deviceAssets, /deviceAssetService/)
  assert.match(deviceAssets, /API_ENDPOINTS\.devices/)
})

test('feature pages read device assets through deviceAssetService only', () => {
  const repairManagement = readSource('../src/composables/useRepairManagement.js')
  const rolePortal = readSource('../src/views/roles/components/RolePortalPage.vue')

  assert.match(repairManagement, /deviceAssetService/)
  assert.doesNotMatch(repairManagement, /deviceResourceService/)
  assert.match(rolePortal, /deviceAssetService/)
  assert.doesNotMatch(rolePortal, /deviceResourceService/)
})

test('status, iot and activation services do not call device asset endpoints', () => {
  const serviceSources = [
    readSource('../src/api/services/deviceStatus.js'),
    readSource('../src/api/services/iot.js'),
    readSource('../src/api/services/deviceActivation.js'),
  ].join('\n')

  assert.doesNotMatch(serviceSources, /API_ENDPOINTS\.devices/)
  assert.doesNotMatch(serviceSources, /['"`]\/devices/)
})

test('legacy iot hardware service is removed from frontend api surface', () => {
  const legacyService = new URL('../src/api/iotHardware.js', import.meta.url)

  assert.equal(existsSync(legacyService), false)
})

test('dashboard and permission labels keep asset, runtime and access authorization concepts separated', () => {
  const dashboard = readSource('../src/constants/dashboard.js')
  const permissions = readSource('../src/constants/permissions.js')
  const rolePortals = readSource('../src/constants/rolePortals.js')

  assert.doesNotMatch(dashboard, /资产台账与设备状态/)
  assert.doesNotMatch(rolePortals, /资产台账与设备状态/)
  assert.match(dashboard, /资产台账与生命周期/)
  assert.match(permissions, /接入授权:\s*\['\/lab-admin\/device-status'\]/)
  assert.match(permissions, /设备绑定:\s*\['\/lab-admin\/device-status'\]/)
})
