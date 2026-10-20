// @vitest-environment happy-dom
import { shallowMount as mountPage } from '@vue/test-utils'
import { panelStubs } from '../support/formStubs'
import Page from '../../src/views/devices/DeviceManagement.vue'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useDeviceManagement } from '../../src/composables/useDeviceManagement'
import { deviceAssetService as service } from '../../src/api/services/deviceAssets'
import { listLabs } from '../../src/api/services/lab'
import { listUserOptions } from '../../src/api/services/users'
import { ElMessage, ElMessageBox } from 'element-plus'

vi.mock('../../src/api/services/lab', () => ({ listLabs: vi.fn() }))
vi.mock('../../src/api/services/users', () => ({ listUserOptions: vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn(), info: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
const scopes = []
it('unmatched asset lab hints require a meaningful signal and handled option failures stay silent',async()=>{
 const state=await setup();await state.openEditDialog({})
 expect(state.formModel.labId).toBe('');expect(ElMessage.info).not.toHaveBeenCalled()
 await state.openEditDialog({remark:'来源待核实'});expect(state.formModel.labId).toBe('')
 expect(ElMessage.info).toHaveBeenCalledWith('未自动匹配到实验室，请手动选择所属实验室')
 listLabs.mockRejectedValueOnce({__handled:true});ElMessage.error.mockClear();await state.loadLabOptions(true)
 expect(ElMessage.error).not.toHaveBeenCalled();expect(state.labOptions.value).toEqual([])
})
it('empty inventory and transfer targets do not send writes, and empty import counts display zero',async()=>{
 const state=await setup();await state.submitInventory();await state.submitTransfer()
 expect(service.createInventoryRecord).not.toHaveBeenCalled();expect(service.transfer).not.toHaveBeenCalled()
 service.importFile.mockResolvedValueOnce(undefined);await state.importDevicesFromFile(new File(['xlsx'],'devices.xlsx'))
 expect(ElMessage.success).toHaveBeenCalledWith('导入完成，共处理 0 条设备资产')
 service.stats.mockRejectedValueOnce(Error('offline'));service.page.mockResolvedValueOnce({records:rows(),total:0});await state.search()
 expect(state.metrics.value[0].value).toBe(2)
})
it('asset page connects filter, selection, pagination and dialog state contracts',async()=>{const w=mountPage(Page,{global:{stubs:panelStubs(),directives:{loading:()=>{}}}});try{await flushPromises();expect(w.text()).toContain('设备资产台账');const filters=w.findComponent({name:'DeviceFilterBar'});for(const [key,value] of [['keyword','电脑'],['labFilter','7'],['categoryFilter','计算终端'],['statusFilter','正常']])filters.vm.$emit(`update:${key}`,value);filters.vm.$emit('search');await flushPromises();expect(service.page).toHaveBeenLastCalledWith(expect.objectContaining({keyword:'电脑',labId:'7',category:'计算终端',status:'正常'}));const table=w.findComponent({name:'DeviceListTable'});table.vm.$emit('selectionChange',[rows()[0]]);table.vm.$emit('view',rows()[0]);await flushPromises();expect(w.findComponent({name:'DeviceDetailPanel'}).props('modelValue')).toBe(true);w.findComponent({name:'DeviceDetailPanel'}).vm.$emit('update:modelValue',false);w.findComponent({name:'DeviceManagementHeader'}).vm.$emit('create');await flushPromises();expect(w.findComponent({name:'DeviceEditorDialog'}).props('modelValue')).toBe(true);w.findComponent({name:'DeviceEditorDialog'}).vm.$emit('update:modelValue',false);w.findComponent({name:'DeviceInventoryDialog'}).vm.$emit('update:modelValue',true);w.findComponent({name:'DeviceTransferDialog'}).vm.$emit('update:modelValue',true);await flushPromises();expect(w.findComponent({name:'DeviceTransferDialog'}).props('modelValue')).toBe(true)}finally{w.unmount()}})
const rows = () => [{ id: 1, assetNo: 'DEV-1', deviceName: '电脑', category: '计算终端', labId: 7, labName: '机房', roomNo: 'A101', quantity: 2, unit: '台', status: '正常' }, { id: 2, deviceName: '交换机', status: '维修中' }]
beforeEach(() => {
  vi.resetAllMocks()
  for (const key of ['page','stats','detail','create','update','remove','createInventoryRecord','transfer','importFile','export','downloadTemplate']) vi.spyOn(service, key)
  service.page.mockResolvedValue({ records: rows(), total: 2 })
  service.stats.mockResolvedValue({ total: 2, normal: 1, repairing: 1, idle: 0, scrapped: 0 })
  service.detail.mockResolvedValue(rows()[0]); service.create.mockResolvedValue(rows()[0]); service.update.mockResolvedValue(rows()[0]); service.remove.mockResolvedValue()
  service.createInventoryRecord.mockResolvedValue({}); service.transfer.mockResolvedValue({}); service.importFile.mockResolvedValue({ count: 3 }); service.export.mockResolvedValue({}); service.downloadTemplate.mockReturnValue(1)
  listLabs.mockResolvedValue([{ id: 7, labName: '机房', roomNo: 'A101' }, { id: 8, labName: '网络机房', roomNo: 'B102' }])
  listUserOptions.mockResolvedValue([{ id: 3, realName: '管理员甲' }])
  ElMessageBox.confirm.mockResolvedValue(true)
})
afterEach(() => { for (const scope of scopes.splice(0)) scope.stop(); vi.restoreAllMocks() })
async function setup() { const scope = effectScope(); scopes.push(scope); const state = scope.run(() => useDeviceManagement()); await flushPromises(); return state }

it('handled and unhandled asset failures preserve state and never leave submission spinners active', async () => {
  const state = await setup()
  await state.openCreateDialog(); Object.assign(state.formModel, rows()[0])
  state.openInventoryDialog(rows()[0]); Object.assign(state.inventoryFormModel, { inspectorName: '盘点员', inspectedAt: '2026-10-18', resultStatus: '正常' })
  await state.openTransferDialog(rows()[0]); state.transferFormModel.operatorName = '经办人'
  for (const [serviceMethod, operation, fallback] of [
    ['create', () => state.submitDevice(), '保存设备资产失败'],
    ['createInventoryRecord', () => state.submitInventory(), '保存盘点记录失败'],
    ['transfer', () => state.submitTransfer(), '保存调拨记录失败'],
    ['importFile', () => state.importDevicesFromFile(new File(['x'], 'x.xlsx')), '导入设备资产失败'],
    ['export', () => state.exportDevices(), '导出设备资产失败'],
  ]) {
    for (const error of [{}, { message: 'specific' }, { __handled: true }]) {
      ElMessage.error.mockClear(); service[serviceMethod].mockRejectedValueOnce(error)
      await operation()
      if (error.__handled) expect(ElMessage.error).not.toHaveBeenCalled()
      else expect(ElMessage.error).toHaveBeenCalledWith(error.message || fallback)
    }
  }
  expect(state.formSubmitting.value).toBe(false)
  expect(state.inventorySubmitting.value).toBe(false)
  expect(state.transferSubmitting.value).toBe(false)
  expect(state.importingDevices.value).toBe(false)
})

it('list failures and empty option outages remain retryable and use safe statistics defaults', async () => {
  service.page.mockRejectedValueOnce({})
  service.stats.mockResolvedValueOnce({})
  const state = await setup()
  expect(ElMessage.error).toHaveBeenCalledWith('加载设备资产列表失败')
  expect(state.metrics.value.map(item => item.value)).toEqual([0,0,0,0,0])
  listLabs.mockRejectedValueOnce({}); listUserOptions.mockRejectedValueOnce(new Error('offline'))
  await state.openCreateDialog()
  await state.openTransferDialog(rows()[0])
  expect(state.ownerOptions.value).toEqual([])
  expect(listUserOptions).toHaveBeenCalled()
  service.page.mockRejectedValueOnce({ __handled: true })
  ElMessage.error.mockClear(); await state.search()
  expect(ElMessage.error).not.toHaveBeenCalled()
  state.openInventoryDialog(null); await state.openTransferDialog(null)
})

it('delete failures distinguish cancellation, handled errors and generic server errors', async () => {
  const state = await setup()
  state.selectedDevices.value = rows()
  for (const operation of [() => state.deleteDevice(rows()[0]), () => state.batchDeleteDevices()]) {
    for (const error of ['cancel', 'close', { __handled: true }, {}, { message: 'specific' }]) {
      ElMessage.error.mockClear(); ElMessageBox.confirm.mockRejectedValueOnce(error)
      await operation()
      if (typeof error === 'string' || error.__handled) expect(ElMessage.error).not.toHaveBeenCalled()
      else expect(ElMessage.error).toHaveBeenCalledWith(error.message || expect.stringContaining('删除设备资产失败'))
    }
  }
})

it('loads paged assets and statistics and translates filters and page events into query parameters', async () => {
  const state = await setup()
  expect(state.devices.value).toHaveLength(2); expect(state.total.value).toBe(2)
  expect(state.metrics.value[1].value).toBe(1)
  state.keyword.value = '  电脑 '; state.labFilter.value = '7'; state.categoryFilter.value = '计算终端'; state.statusFilter.value = '正常'
  await state.search()
  expect(service.page).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: '电脑', labId: '7', labName: '机房', category: '计算终端', status: '正常', pageNum: 1 }))
  await state.handlePageChange(2); expect(state.currentPage.value).toBe(2)
  await state.handlePageSizeChange(25); expect(state.pageSize.value).toBe(25); expect(state.currentPage.value).toBe(1)
  await state.resetFilters(); expect(state.keyword.value).toBe(''); expect(state.labFilter.value).toBe('')
})

it('create validates required fields then saves and reloads', async () => {
  const state = await setup(); await state.openCreateDialog()
  expect(state.dialogTitle.value).toBe('新增设备资产')
  await state.submitDevice(); expect(ElMessage.warning).toHaveBeenCalledWith('请填写设备名称')
  Object.assign(state.formModel, rows()[0]); await state.submitDevice()
  expect(service.create).toHaveBeenCalledWith(expect.objectContaining({ deviceName: '电脑', labId: 7 }), state.labOptions.value)
  expect(state.dialogVisible.value).toBe(false); expect(state.formSubmitting.value).toBe(false)
  await state.openEditDialog(rows()[0]); expect(state.dialogTitle.value).toBe('编辑设备资产')
  await state.submitDevice(); expect(service.update).toHaveBeenCalledWith(1, expect.any(Object), state.labOptions.value)
})

it.each([['category','请选择设备分类'], ['labName','请填写所属实验室'], ['quantity','请填写大于 0 的设备数量'], ['unit','请填写单位']])('device validation rejects invalid %s', async (field, message) => {
  const state = await setup(); await state.openCreateDialog(); Object.assign(state.formModel, rows()[0], { [field]: '' })
  await state.submitDevice(); expect(ElMessage.warning).toHaveBeenCalledWith(message); expect(service.create).not.toHaveBeenCalled()
})

it('detail failure preserves the selected row and unmatched lab hints do not mutate lab options', async () => {
  const state = await setup()
  service.detail.mockRejectedValueOnce(new Error('offline'))
  const device = rows()[0]; await state.openDetailDialog(device)
  expect(state.selectedDevice.value).toEqual(device); expect(state.detailVisible.value).toBe(true)
  await state.openEditDialog({ ...device, labId: '', labName: 'unknown', roomNo: 'unknown' })
  expect(state.formModel.labId).toBe('')
  expect(ElMessage.info).toHaveBeenCalledWith('未自动匹配到实验室，请手动选择所属实验室')
  await state.openDetailDialog(null); await state.openEditDialog(null)
})

it('deletion supports cancellation and only deletes selected assets in batch', async () => {
  const state = await setup()
  await state.batchDeleteDevices(); expect(ElMessage.warning).toHaveBeenCalledWith('请先选择需要删除的设备资产')
  ElMessageBox.confirm.mockRejectedValueOnce('cancel'); await state.deleteDevice(rows()[0]); expect(service.remove).not.toHaveBeenCalled()
  await state.deleteDevice(rows()[0]); expect(service.remove).toHaveBeenCalledWith(1)
  state.handleSelectionChange(rows()); await state.batchDeleteDevices()
  expect(service.remove).toHaveBeenCalledWith(2); expect(state.selectedDevices.value).toEqual([])
  await state.deleteDevice(null)
})

it('inventory validates inspector, time and result and saves single or multiple audit records', async () => {
  const state = await setup()
  state.openBatchInventoryDialog(); expect(ElMessage.warning).toHaveBeenCalledWith('请先选择需要盘点的设备资产')
  state.openInventoryDialog(rows()[0]); await state.submitInventory()
  expect(ElMessage.warning).toHaveBeenCalledWith('请填写盘点人')
  state.inventoryFormModel.inspectorName = '管理员'; state.inventoryFormModel.inspectedAt = ''
  await state.submitInventory(); expect(ElMessage.warning).toHaveBeenCalledWith('请选择盘点时间')
  state.inventoryFormModel.inspectedAt = '2026-10-18'; state.inventoryFormModel.resultStatus = ''
  await state.submitInventory(); expect(ElMessage.warning).toHaveBeenCalledWith('请选择盘点结果')
  state.inventoryFormModel.resultStatus = '已盘点'; await state.submitInventory()
  expect(service.createInventoryRecord).toHaveBeenCalledWith(1, expect.objectContaining({ inspectorName: '管理员', deviceCode: 'DEV-1' }))
  expect(state.inventoryDialogVisible.value).toBe(false)
  state.handleSelectionChange(rows()); state.openBatchInventoryDialog(); state.inventoryFormModel.inspectorName = '管理员'; await state.submitInventory()
  expect(service.createInventoryRecord).toHaveBeenCalledWith(2, expect.any(Object))
  expect(state.inventorySubmitting.value).toBe(false)
})

it('transfers load owners and labs once, resolve names and submit all selected devices', async () => {
  const state = await setup()
  await state.openBatchTransferDialog(); expect(ElMessage.warning).toHaveBeenCalledWith('请先选择需要调拨的设备资产')
  await state.openTransferDialog(rows()[0]); await state.submitTransfer()
  expect(ElMessage.warning).toHaveBeenCalledWith('请填写经办人')
  Object.assign(state.transferFormModel, { operatorName: '管理员', toLabId: 8, toOwnerUserId: 3 })
  await state.submitTransfer()
  expect(service.transfer).toHaveBeenCalledWith(1, expect.objectContaining({ toLabName: '网络机房', toOwnerName: '管理员甲' }))
  expect(state.transferDialogVisible.value).toBe(false)
  state.handleSelectionChange(rows()); await state.openBatchTransferDialog()
  state.transferFormModel.operatorName = '管理员'; await state.submitTransfer()
  expect(service.transfer).toHaveBeenCalledWith(2, expect.any(Object))
  expect(listUserOptions).toHaveBeenCalledTimes(1)
  expect(state.selectedDevices.value).toEqual([])
})

it('import, export and template actions pass current filters and reset busy state', async () => {
  const state = await setup()
  await state.importDevicesFromFile(null); expect(service.importFile).not.toHaveBeenCalled()
  const file = { name: 'devices.xlsx' }; await state.importDevicesFromFile(file)
  expect(service.importFile).toHaveBeenCalledWith(file, state.labOptions.value)
  expect(state.importingDevices.value).toBe(false)
  await state.exportDevices(); expect(service.export).toHaveBeenCalled()
  state.downloadDeviceImportTemplate(); expect(service.downloadTemplate).toHaveBeenCalled()
})

it.each([['create','submitDevice'], ['createInventoryRecord','submitInventory'], ['transfer','submitTransfer'], ['importFile','importDevicesFromFile'], ['export','exportDevices'], ['remove','deleteDevice']])('%s failures keep dialogs and clear busy indicators', async (apiName, method) => {
  const state = await setup()
  await state.openCreateDialog(); Object.assign(state.formModel, rows()[0])
  state.openInventoryDialog(rows()[0]); state.inventoryFormModel.inspectorName = '管理员'
  await state.openTransferDialog(rows()[0]); state.transferFormModel.operatorName = '管理员'
  service[apiName].mockRejectedValueOnce(new Error('业务失败'))
  await state[method](rows()[0])
  expect(ElMessage.error).toHaveBeenCalledWith('业务失败')
  expect([state.formSubmitting.value, state.inventorySubmitting.value, state.transferSubmitting.value, state.importingDevices.value]).toEqual([false,false,false,false])
})

it('load failures retain an empty page, compute fallback stats and allow lab-option retry', async () => {
  service.stats.mockRejectedValue(new Error('offline'))
  listLabs.mockRejectedValueOnce(new Error('offline'))
  const state = await setup()
  expect(state.metrics.value[0].value).toBe(2); expect(state.metrics.value[1].value).toBe(1)
  expect(state.labOptions.value).toEqual([])
  await state.loadLabOptions(true); expect(state.labOptions.value).toHaveLength(2)
  service.page.mockRejectedValueOnce({ __handled: true }); ElMessage.error.mockClear()
  await state.search(); expect(state.loading.value).toBe(false); expect(ElMessage.error).not.toHaveBeenCalled()
})
