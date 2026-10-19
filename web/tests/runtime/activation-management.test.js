// @vitest-environment happy-dom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import ActivationManagement from '../../src/views/system/ActivationCodeManagement.vue'
import * as api from '../../src/api/services/activationCodes'
import { ElMessage, ElMessageBox } from 'element-plus'
import { panelStubs as tableStubs } from '../support/formStubs'

const navigation = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => navigation }))
vi.mock('../../src/api/services/activationCodes', () => Object.fromEntries(['batchOperateActivationCodes', 'createActivationCodes', 'getActivationCodePage', 'listActivationAuditLogs', 'listActivationCodeBindings', 'remoteBindActivationDevice', 'remoteUnbindActivationDevice', 'updateActivationCodeRemark', 'updateActivationCodeStatus'].map(key => [key, vi.fn()])))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn(), error: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
const wrappers = []
const row = () => ({ id: 'code-1', code: 'abcdefgh-12345678-abcdefgh', maskedCode: 'abcd****efgh', deviceLimit: 5, boundCount: 2, enabled: true, ownerName: '负责人', createdAt: '2026-10-18T09:00:00' })
beforeEach(() => {
  vi.resetAllMocks()
  api.getActivationCodePage.mockResolvedValue({ records: [row()], statistics: { totalCodes: 1, enabledCodes: 1, disabledCodes: 0 } })
  api.listActivationAuditLogs.mockResolvedValue([{ action: '生成' }])
  api.listActivationCodeBindings.mockResolvedValue([{ id: 'binding-1', machineCode: 'host' }])
  api.createActivationCodes.mockResolvedValue({ codes: [{ code: 'full-code' }] })
  api.remoteBindActivationDevice.mockResolvedValue({ record: row() })
  ElMessageBox.confirm.mockResolvedValue(true)
  vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test')
  vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
})
afterEach(() => { for (const wrapper of wrappers.splice(0)) wrapper.unmount(); vi.restoreAllMocks() })
async function setup(render = false) {
  const names = ['alert','button','card','date-picker','dialog','drawer','dropdown','dropdown-item','dropdown-menu','empty','form','form-item','icon','input','input-number','option','select','switch','table','table-column','tag']
  const wrapper = shallowMount(ActivationManagement, { global: { directives: { loading: {} }, stubs: render ? tableStubs() : Object.fromEntries(names.map(name => [`el-${name}`, true])) } })
  wrappers.push(wrapper); await flushPromises(); return wrapper.vm
}

it('rendered authorization rows expose masked quotas and row actions target their actual record', async () => {
  const vm=await setup(true),wrapper=wrappers.at(-1)
  expect(wrapper.text()).toContain('abcd****efgh');expect(wrapper.text()).toContain('2 / 5');expect(wrapper.text()).not.toContain(row().code)
  const click=async text=>{const button=wrapper.findAll('button').find(button=>button.text()===text);expect(button).toBeTruthy();await button.trigger('click');await flushPromises()}
  await click('查看设备');expect(api.listActivationCodeBindings).toHaveBeenCalledWith('code-1');expect(vm.deviceDrawerVisible).toBe(true)
  await click('远程绑定');expect(vm.selectedCode.id).toBe('code-1');expect(vm.remoteBindDialogVisible).toBe(true)
  await wrapper.find('input[placeholder="搜索激活码 / MAC / 设备指纹 / 主机名 / 负责人"]').setValue('机房')
  await click('查询');expect(api.getActivationCodePage).toHaveBeenLastCalledWith(expect.objectContaining({keyword:'机房'}))
})

it('rendered authorization editors bind all fields and selected batch actions',async()=>{
  const vm=await setup(true),w=wrappers.at(-1);vm.selectedCode=row();vm.selectedBinding={id:'binding-1',machineCode:'host'};vm.selectionRows=[row()];vm.generatedCodes=[{id:'new',code:'full-code'}];await flushPromises()
  for(const control of w.findAll('input')){if(control.attributes('type')==='checkbox')await control.setValue(false);else await control.setValue('2')}
  for(const control of w.findAll('textarea'))await control.setValue('测试原因')
  expect(vm.createForm).toMatchObject({deviceLimit:2,quantity:2,ownerName:'2',enabled:false,remark:'测试原因'});expect(vm.remoteBindForm).toMatchObject({machineCode:'2',macAddress:'2',hostname:'2',clientSource:'2',remark:'测试原因'});expect(vm.remoteUnbindForm.reason).toBe('测试原因');expect(vm.remarkForm.remark).toBe('测试原因')
  for(const select of w.findAll('select')){const option=select.findAll('option')[0];if(option)await select.setValue(option.element.value)}
  const click=async label=>{await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()}
  await click('复制');expect(navigator.clipboard.writeText).toHaveBeenCalledWith('full-code');await click('复制脱敏码列表');await click('复制授权摘要');await click('批量导出');await click('导出授权');await click('批量修改负责人');expect(vm.batchDialogVisible).toBe(true);await click('批量延长到期');expect(vm.batchDialogTitle).toContain('到期');await click('清空选择');expect(vm.selectionRows).toEqual([])
})
it('copy falls back to temporary textarea after clipboard denial and reports blocked copy',async()=>{const vm=await setup();document.execCommand=vi.fn().mockReturnValueOnce(true).mockReturnValueOnce(false);navigator.clipboard.writeText.mockRejectedValue(new Error('denied'));await vm.copyText('CODE');expect(ElMessage.success).toHaveBeenCalledWith('已复制');await vm.copyText('CODE');expect(ElMessage.warning).toHaveBeenCalledWith('当前浏览器不允许自动复制，请手动复制');expect(document.querySelector('textarea')).toBeNull()})

it('loads codes and audit data, derives quota and health, and submits filters', async () => {
  const vm = await setup()
  expect(vm.activationCodes).toHaveLength(1)
  expect(vm.auditLogs).toHaveLength(1)
  expect(vm.authorizationUsage).toEqual({ available: 3, bound: 2, percent: 40, totalLimit: 5 })
  expect(vm.healthStatusText).toBe('授权运行正常')
  expect(vm.healthStatusType).toBe('success')
  expect(vm.quickStatusFilters[0].value).toBe(1)
  vm.keyword = '负责人'; vm.applyStatusShortcut('disabled', 'expired'); await flushPromises()
  expect(api.getActivationCodePage).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: '负责人', status: 'disabled', expireStatus: 'expired' }))
  vm.resetFilters(); await flushPromises()
  expect(vm.keyword).toBe(''); expect(vm.statusFilter).toBe('all')
  vm.statistics = { totalDeviceLimit: 5, boundDevices: 5, availableDevices: 0 }
  expect(vm.healthStatusText).toBe('设备额度已满'); expect(vm.healthStatusType).toBe('warning')
  vm.statistics = { disabledCodes: 3, enabledCodes: 1 }
  expect(vm.healthStatusText).toBe('停用授权偏多'); expect(vm.healthStatusType).toBe('danger')
  vm.activationCodes = []; vm.statistics = {}
  expect(vm.healthStatusText).toBe('等待授权数据')
})

it('formatting masks codes, reports statuses and clamps quota without altering records', async () => {
  const vm = await setup()
  expect(vm.maskActivationCode('')).toBe('-')
  expect(vm.maskActivationCode('short')).toBe('sh****rt')
  expect(vm.maskActivationCode(row())).toBe('abcd****efgh')
  expect(vm.maskActivationCode(row().code)).toBe('abcd****efgh')
  expect(vm.formatDateTime('bad')).toBe('bad')
  expect(vm.formatDateTime('2026-10-18T09:00:00')).toBe('2026-10-18 09:00')
  expect(vm.formatExpireAt('')).toBe('长期有效')
  expect(vm.getQuotaPercent({})).toBe(0)
  expect(vm.getQuotaPercent({ deviceLimit: 1, boundCount: 2 })).toBe(100)
  for (const [statusKey, text, type] of [['disabled', '停用', 'danger'], ['expired', '已过期', 'warning'], ['quota_full', '额度已满', 'info'], ['enabled', '启用', 'success']]) {
    expect(vm.getStatusText({ statusKey })).toBe(text); expect(vm.getStatusType({ statusKey })).toBe(type)
  }
  for (const [status, text, type] of [['REMOTE_UNBOUND','被强制解绑','danger'],['UNBOUND','已解绑','info'],['EXPIRED','已过期','warning'],['ACTIVE','正常','success']]) {
    expect(vm.getBindingStatusText(status)).toBe(text); expect(vm.getBindingStatusType(status)).toBe(type)
  }
})

it('generation posts trimmed form, exposes generated code once and resets busy state', async () => {
  const vm = await setup()
  Object.assign(vm.createForm, { ownerName: ' 信息中心 ', remark: ' 教学 ', quantity: 2, deviceLimit: 10 })
  await vm.handleGenerate()
  expect(api.createActivationCodes).toHaveBeenCalledWith(expect.objectContaining({ ownerName: '信息中心', remark: '教学', quantity: 2, deviceLimit: 10 }))
  expect(vm.generatedCodes).toEqual([{ code: 'full-code' }])
  expect(vm.createForm.quantity).toBe(1)
  expect(vm.isGenerating).toBe(false)
  api.createActivationCodes.mockRejectedValueOnce(new Error('生成失败'))
  await vm.handleGenerate()
  expect(ElMessage.error).toHaveBeenCalledWith('生成失败')
})

it('copy and CSV export use masked codes and selected summaries', async () => {
  const vm = await setup()
  vm.copyActivationCode(row()); await flushPromises()
  expect(navigator.clipboard.writeText).toHaveBeenCalledWith(row().code)
  vm.copySelectedSummary()
  expect(ElMessage.warning).toHaveBeenCalledWith('请先勾选激活码')
  vm.selectionRows = [row()]
  vm.copySelectedMaskedCodes(); vm.copySelectedSummary(); vm.copyRowSummary(row()); await flushPromises()
  expect(navigator.clipboard.writeText).toHaveBeenCalledWith('abcd****efgh')
  expect(navigator.clipboard.writeText).toHaveBeenCalledWith(expect.stringContaining('设备 2/5'))
  vm.exportRows()
  const blob = URL.createObjectURL.mock.calls[0][0]
  const csv = await blob.text()
  expect(csv).toContain('abcd****efgh')
  expect(csv).not.toContain(row().code)
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test')
  vm.clearTableSelection(); expect(vm.selectedIds).toEqual([])
})

it('remote binding asks before migration and then refreshes the open device drawer', async () => {
  const vm = await setup()
  await vm.openDeviceDrawer(row())
  expect(vm.deviceBindings).toHaveLength(1)
  vm.openRemoteBindDialog(row()); vm.remoteBindForm.machineCode = 'host'
  api.remoteBindActivationDevice.mockResolvedValueOnce({ conflict: true, activationCode: 'other****code' }).mockResolvedValueOnce({ record: row() })
  await vm.submitRemoteBind()
  expect(api.remoteBindActivationDevice).toHaveBeenNthCalledWith(1, 'code-1', expect.objectContaining({ force: false, machineCode: 'host' }))
  expect(api.remoteBindActivationDevice).toHaveBeenNthCalledWith(2, 'code-1', expect.objectContaining({ force: true }))
  expect(vm.remoteBindDialogVisible).toBe(false)
  expect(vm.actionLoading).toBe(false)
  expect(api.listActivationCodeBindings).toHaveBeenCalledTimes(2)
})

it('remote binding cancellation retains dialog and unbinding submits a reason for the selected device', async () => {
  const vm = await setup(); vm.openRemoteBindDialog(row())
  api.remoteBindActivationDevice.mockResolvedValueOnce({ conflict: true })
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await vm.submitRemoteBind()
  expect(vm.remoteBindDialogVisible).toBe(true)
  expect(api.remoteBindActivationDevice).toHaveBeenCalledTimes(1)
  vm.openRemoteUnbindDialog({ id: 'binding-1' }); vm.remoteUnbindForm.reason = '设备报废'
  await vm.submitRemoteUnbind()
  expect(api.remoteUnbindActivationDevice).toHaveBeenCalledWith('code-1', 'binding-1', { reason: '设备报废' })
  expect(vm.remoteUnbindDialogVisible).toBe(false)
})

it('status changes and deletion require confirmation and refresh authoritative lists', async () => {
  const vm = await setup()
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await vm.toggleStatus(row())
  expect(api.updateActivationCodeStatus).not.toHaveBeenCalled()
  await vm.toggleStatus(row()); await vm.toggleStatus({ ...row(), statusKey: 'disabled' })
  expect(api.updateActivationCodeStatus).toHaveBeenCalledWith('code-1', expect.objectContaining({ enabled: false }))
  expect(api.updateActivationCodeStatus).toHaveBeenCalledWith('code-1', expect.objectContaining({ enabled: true }))
  await vm.deleteCode(row())
  expect(api.batchOperateActivationCodes).toHaveBeenCalledWith(expect.objectContaining({ ids: ['code-1'], action: 'DELETE' }))
})

it('remarks trim fields and batches only submit selected IDs', async () => {
  const vm = await setup()
  await vm.submitRemark(); await vm.submitRemoteBind(); await vm.submitRemoteUnbind()
  expect(api.updateActivationCodeRemark).not.toHaveBeenCalled()
  vm.openRemarkDialog(row()); vm.remarkForm.remark = ' 更新 '
  await vm.submitRemark()
  expect(api.updateActivationCodeRemark).toHaveBeenCalledWith('code-1', expect.objectContaining({ remark: '更新', ownerName: '负责人' }))
  expect(vm.remarkDialogVisible).toBe(false)
  await vm.runSimpleBatch('ENABLE'); vm.openBatchDialog('UPDATE_OWNER')
  expect(api.batchOperateActivationCodes).not.toHaveBeenCalled()
  for (const action of ['ENABLE', 'DISABLE', 'DELETE']) {
    vm.selectionRows = [row()]; await vm.runSimpleBatch(action)
    expect(api.batchOperateActivationCodes).toHaveBeenCalledWith(expect.objectContaining({ ids: ['code-1'], action }))
    expect(vm.selectionRows).toEqual([])
  }
  for (const action of ['UPDATE_OWNER', 'EXTEND_EXPIRE_AT']) {
    vm.selectionRows = [row()]; vm.openBatchDialog(action)
    Object.assign(vm.batchForm, { ownerName: '新负责人', expireAt: '2030-01-01', reason: '调整' })
    await vm.submitBatchDialog()
    expect(api.batchOperateActivationCodes).toHaveBeenCalledWith(expect.objectContaining({ ids: ['code-1'], action, reason: '调整' }))
    expect(vm.batchDialogVisible).toBe(false)
  }
})

it.each([
  ['remoteBindActivationDevice', 'submitRemoteBind'], ['remoteUnbindActivationDevice', 'submitRemoteUnbind'],
  ['updateActivationCodeRemark', 'submitRemark'], ['updateActivationCodeStatus', 'toggleStatus'],
  ['batchOperateActivationCodes', 'deleteCode'], ['batchOperateActivationCodes', 'submitBatchDialog'],
])('%s failure resets busy state and surfaces server error', async (apiName, handler) => {
  const vm = await setup(); vm.selectedCode = row(); vm.selectedBinding = { id: 'binding-1' }
  api[apiName].mockRejectedValueOnce(new Error('服务错误'))
  await vm[handler](row())
  expect(ElMessage.error).toHaveBeenCalledWith('服务错误')
  expect(vm.actionLoading).toBe(false)
})

it('load failures reset all busy flags and hero actions dispatch refresh, generation and back', async () => {
  api.getActivationCodePage.mockRejectedValueOnce(new Error('列表失败'))
  api.listActivationAuditLogs.mockRejectedValueOnce(new Error('审计失败'))
  const vm = await setup()
  expect(vm.isLoading).toBe(false); expect(vm.auditLoading).toBe(false)
  expect(ElMessage.error).toHaveBeenCalledWith('列表失败')
  api.listActivationCodeBindings.mockRejectedValueOnce(new Error('绑定失败'))
  await vm.openDeviceDrawer(row()); expect(vm.deviceLoading).toBe(false)
  vm.handleHeroAction({ command: 'refresh' }); await flushPromises()
  expect(vm.activationCodes).toHaveLength(1)
  vm.handleHeroAction({ command: 'generate' }); await flushPromises()
  expect(vm.createDialogVisible).toBe(true)
  expect(api.createActivationCodes).not.toHaveBeenCalled()
  vm.handleBack(); expect(navigation.push).toHaveBeenCalledWith('/system-admin')
})
