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
  const rolePortal = readSource('../src/views/roles/components/RolePortalPage.vue')

  assert.match(rolePortal, /deviceAssetService/)
  assert.doesNotMatch(rolePortal, /deviceResourceService/)
})

test('retired repair-management frontend is removed from the runtime surface', () => {
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

test('dashboard and permission labels expose only retained asset authorization concepts', () => {
  const dashboard = readSource('../src/constants/dashboard.js')
  const permissions = readSource('../src/constants/permissions.js')

  assert.match(dashboard, /资产台账与生命周期/)
  assert.doesNotMatch(permissions, /设备状态|接入授权|设备绑定|物联网扩展/)
})
