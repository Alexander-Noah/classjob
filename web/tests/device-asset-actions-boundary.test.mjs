/*
 * 设备资产的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('device asset page wires inventory and transfer dialogs to asset service', () => {
  const page = readSource('../src/views/devices/DeviceManagement.vue')
  const composable = readSource('../src/composables/useDeviceManagement.js')
  const table = readSource('../src/views/devices/components/DeviceListTable.vue')

  assert.match(page, /DeviceInventoryDialog/)
  assert.match(page, /DeviceTransferDialog/)
  assert.match(page, /@inventory="openInventoryDialog"/)
  assert.match(page, /@transfer="openTransferDialog"/)

  assert.match(table, /defineEmits\(\[[^\]]*'inventory'[^\]]*'transfer'/s)
  assert.match(table, /\$emit\('inventory', row\)/)
  assert.match(table, /\$emit\('transfer', row\)/)

  assert.match(composable, /deviceAssetService\.createInventoryRecord/)
  assert.match(composable, /deviceAssetService\.transfer/)
  assert.match(composable, /submitInventory/)
  assert.match(composable, /submitTransfer/)
})

test('device asset page supports batch delete, inventory and transfer actions', () => {
  const page = readSource('../src/views/devices/DeviceManagement.vue')
  const composable = readSource('../src/composables/useDeviceManagement.js')
  const table = readSource('../src/views/devices/components/DeviceListTable.vue')

  assert.match(table, /type="selection"/)
  assert.match(table, /@selection-change="\$emit\('selectionChange', \$event\)"/)
  assert.match(table, /defineEmits\(\[[^\]]*'selectionChange'/s)

  assert.match(page, /selectedDevices\.length/)
  assert.match(page, /@selection-change="handleSelectionChange"/)
  assert.match(page, /@click="batchDeleteDevices"/)
  assert.match(page, /@click="openBatchInventoryDialog"/)
  assert.match(page, /@click="openBatchTransferDialog"/)
  assert.match(page, /:devices="inventoryDevices"/)

  assert.match(composable, /selectedDevices\s*=\s*ref\(\[\]\)/)
  assert.match(composable, /handleSelectionChange/)
  assert.match(composable, /batchDeleteDevices/)
  assert.match(composable, /openBatchInventoryDialog/)
  assert.match(composable, /openBatchTransferDialog/)
})

test('device asset lifecycle statuses include idle assets in options and metrics', () => {
  const service = readSource('../src/api/services/deviceAssets.js')
  const stats = readSource('../src/api/services/deviceAssetStats.js')
  const composable = readSource('../src/composables/useDeviceManagement.js')

  assert.match(service, /const STATUS_OPTIONS\s*=\s*\[[^\]]*,[^\]]*,[^\]]*,[^\]]*\]/s)
  assert.match(stats, /idle:/)
  assert.match(composable, /idle:\s*0/)
  assert.match(composable, /stats\.value\.idle/)
})

test('device asset code dialog stays focused on asset ledger data', () => {
  const dialog = readSource('../src/views/devices/components/DeviceAssetCodeDialog.vue')

  assert.doesNotMatch(dialog, /activeDevice\.online/)
  assert.doesNotMatch(dialog, /在线状态/)
  assert.match(dialog, /最近盘点/)
})
