import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { effectScope, nextTick, reactive, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useBusinessModuleRecords } from '../../src/composables/useBusinessModuleRecords'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exportTableToExcel } from '../../src/utils/exportExcel'

const service = vi.hoisted(() => Object.fromEntries(['list', 'replace', 'create', 'update', 'remove', 'reset'].map(key => [key, vi.fn()])))
vi.mock('../../src/api/services/collectionService', () => ({ createCollectionService: () => service }))
vi.mock('../../src/utils/exportExcel', () => ({ exportTableToExcel: vi.fn(() => 2) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
const scopes = []
const rows = () => [
  { id: 1, name: '课程一', lab: 'A1 A1 机房', time: '08:00', status: '待复核', owner: '甲', extra: '数据' },
  { id: 2, name: '课程二', lab: 'B2 机房', time: '10:00', status: '正常', owner: '乙' },
]
beforeEach(() => {
  vi.clearAllMocks()
  service.list.mockResolvedValue(rows())
  service.replace.mockImplementation(async records => records)
  service.create.mockImplementation(async record => ({ ...record, id: 3 }))
  service.update.mockImplementation(async (id, record) => ({ ...record, id }))
  service.remove.mockResolvedValue([])
  service.reset.mockResolvedValue(rows())
  ElMessageBox.confirm.mockResolvedValue(true)
})
afterEach(() => { for (const scope of scopes.splice(0)) scope.stop() })
async function setup(overrides = {}) {
  const scope = effectScope()
  scopes.push(scope)
  const route = reactive({ name: 'notices' })
  const config = ref({
    recordName: '通知', primaryKey: 'name',
    tableColumns: [
      { prop: 'name', label: '名称' }, { prop: 'lab', label: '实验室', required: false },
      { prop: 'status', label: '状态', type: 'tag' }, { prop: 'owner', label: '负责人', required: false },
      { prop: 'category', label: '分类', options: [{ label: '设备' }, { value: '课程' }, '其他'], required: false },
      { prop: 'locked', label: '只读', readonly: true },
    ],
    statusOptions: [{ label: '正常', type: 'success' }, { label: '待复核', type: 'warning' }, { label: '冲突', type: 'danger' }],
    ...overrides,
  })
  const state = scope.run(() => useBusinessModuleRecords({ route, moduleConfig: config, pageTitle: ref('通知台账') }))
  await flushPromises()
  return { ...state, config, route }
}

it('loads and normalizes records, derives status counts, options and pagination', async () => {
  const state = await setup()
  expect(state.totalRecordCount.value).toBe(2)
  expect(state.records.value[0].lab).toBe('A1 机房')
  expect(state.records.value[0]._recordId).toBeTruthy()
  expect(state.formFieldOptions.value).toMatchObject({ lab: ['A1 机房', 'B2 机房'], owner: ['甲', '乙'], category: ['设备', '课程', '其他'] })
  expect(state.statusSummary.value[0]).toMatchObject({ count: 1, percentage: 50 })
  expect(state.editableColumns.value.some(column => column.prop === 'locked')).toBe(false)
  expect(state.tableColumns.value).toHaveLength(6)
  expect(state.recordName.value).toBe('通知')
  expect(state.statusProp.value).toBe('status')
  expect(state.actions.value).toEqual([])
  expect(state.getRowTagType({ status: '正常' })).toBe('success')
  expect(state.getRowTagType({ status: '未知' })).toBe('info')
  expect(state.getRowTagType({ status: '正常', tagType: 'danger' })).toBe('danger')
  state.handlePageSizeChange(1)
  state.handleCurrentPageChange(2)
  expect(state.pagedRecords.value[0].id).toBe(2)
  state.keyword.value = '课程一'
  await nextTick()
  expect(state.currentPage.value).toBe(1)
  expect(state.filteredRecordCount.value).toBe(1)
  state.keyword.value = state.records.value[0]._recordId
  expect(state.filteredRecords.value).toEqual([])
  state.keyword.value = ''
  state.statusFilter.value = '正常'
  expect(state.filteredRecords.value.map(record => record.id)).toEqual([2])
  state.handleCurrentPageChange(99)
  state.statusFilter.value = '不存在'
  await nextTick()
  expect(state.currentPage.value).toBe(1)
})

it.each(['batch', 'single'])('create/edit in %s mode persist fields and keep unrelated records', async mode => {
  const state = await setup({ persistMode: mode })
  state.openCreateDialog()
  expect(state.dialogTitle.value).toBe('新增通知')
  await state.submitRecord()
  expect(ElMessage.warning).toHaveBeenCalledWith('请填写名称')
  expect(service.create).not.toHaveBeenCalled()
  state.formModel.name = '新增记录'
  await state.submitRecord()
  expect(state.dialogVisible.value).toBe(false)
  expect(state.formSubmitting.value).toBe(false)
  expect(state.records.value[0].name).toBe('新增记录')
  if (mode === 'single') expect(service.create).toHaveBeenCalledWith(expect.objectContaining({ name: '新增记录', status: '正常' }))
  else expect(service.replace.mock.calls[0][0].some(record => '_recordId' in record)).toBe(false)
  const target = state.records.value.find(record => record.id === 1)
  const identity = target._recordId
  state.openEditDialog(target)
  expect(state.dialogTitle.value).toBe('编辑通知')
  state.formModel.name = '编辑记录'
  await state.submitRecord()
  expect(state.records.value.find(record => record.id === 1).name).toBe('编辑记录')
  if (mode === 'single') expect(state.records.value.find(record => record.id === 1)._recordId).toBe(identity)
  expect(state.records.value.find(record => record.id === 2).name).toBe('课程二')
  state.viewRecordDetail(state.records.value[0])
  expect(state.detailVisible.value).toBe(true)
  expect(state.activeRecord.value).toEqual(state.records.value[0])
})

it('failed submit resets busy state and leaves dialog open', async () => {
  const state = await setup({ persistMode: 'single' })
  state.openCreateDialog()
  state.formModel.name = '记录'
  service.create.mockRejectedValueOnce(new Error('保存失败'))
  await expect(state.submitRecord()).rejects.toThrow('保存失败')
  expect(state.formSubmitting.value).toBe(false)
  expect(state.dialogVisible.value).toBe(true)
  expect(state.totalRecordCount.value).toBe(2)
})

it.each(['batch', 'single'])('delete and batch status in %s mode affect only selected records', async mode => {
  const state = await setup({ persistMode: mode, actions: [{ command: 'batchStatus', targetStatus: '正常' }] })
  expect(state.batchTargetStatus.value).toBe('正常')
  await state.applyBatchStatus()
  expect(ElMessage.warning).toHaveBeenCalledWith('请先选择需要处理的通知')
  state.handleSelectionChange([state.records.value[0]])
  await state.applyBatchStatus()
  expect(state.records.value[0]).toMatchObject({ status: '正常', tagType: 'success' })
  expect(state.selectedRows.value).toEqual([])
  state.handleSelectionChange([state.records.value[0]])
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.deleteSelectedRecords()
  expect(state.totalRecordCount.value).toBe(2)
  await state.deleteSelectedRecords()
  expect(state.records.value.map(record => record.id)).toEqual([2])
  expect(state.selectedRows.value).toEqual([])
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.deleteRecord(state.records.value[0])
  expect(state.totalRecordCount.value).toBe(1)
  await state.deleteRecord(state.records.value[0])
  expect(state.totalRecordCount.value).toBe(0)
  await state.deleteSelectedRecords()
  expect(ElMessage.warning).toHaveBeenCalledWith('请先选择需要删除的通知')
})

it('export includes extra business fields, omits internal fields and handles empty results', async () => {
  const state = await setup()
  state.exportRecords()
  const exported = exportTableToExcel.mock.calls[0][0]
  expect(exported.filename).toBe('通知台账')
  expect(exported.columns).toContainEqual({ label: 'extra', prop: 'extra' })
  expect(exported.columns.some(column => column.prop.startsWith('_') || column.prop === 'tagType')).toBe(false)
  state.keyword.value = 'no match'
  state.exportRecords()
  expect(exportTableToExcel.mock.calls[1][0].rows).toHaveLength(2)
  await nextTick()
  state.handleSelectionChange(state.records.value)
  await state.deleteSelectedRecords()
  state.exportRecords()
  expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的数据')
})

it('reset honors cancellation then restores records and filters', async () => {
  const state = await setup()
  state.keyword.value = 'x'
  state.statusFilter.value = '冲突'
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.resetModuleRecords()
  expect(service.reset).not.toHaveBeenCalled()
  await state.resetModuleRecords()
  expect(state.keyword.value).toBe('')
  expect(state.statusFilter.value).toBe('all')
  expect(state.totalRecordCount.value).toBe(2)
})

it('conflict detection marks shared resources, preserves unrelated records and supports no conflicts', async () => {
  const state = await setup()
  state.handleModuleAction({ command: 'detectConflicts' })
  await flushPromises()
  expect(ElMessage.success).toHaveBeenCalledWith('未发现预约时间或资源冲突')
  service.list.mockResolvedValueOnce([
    { id: 1, name: 'a', lab: 'A', time: '08:00', status: '正常' },
    { id: 2, name: 'b', lab: 'A', time: '08:00', status: '正常' },
    { id: 3, name: 'c', status: '冲突待处理' },
    { id: 4, name: 'd', status: '正常' },
  ])
  state.route.name = 'devices'
  await flushPromises()
  state.handleModuleAction({ command: 'detectConflicts' })
  await flushPromises()
  expect(state.statusFilter.value).toBe('冲突')
  expect(state.filteredRecordCount.value).toBe(3)
  expect(state.records.value[3].status).toBe('正常')
})

it('module commands select abnormal and permission-review records and dispatch other actions', async () => {
  const state = await setup()
  state.handleModuleAction({ command: 'filterAbnormal' })
  expect(state.statusFilter.value).toBe('待复核')
  state.handleModuleAction({ command: 'validatePermissions' })
  expect(ElMessage.warning).toHaveBeenCalledWith('发现 1 项权限配置待复核')
  state.handleModuleAction({ command: 'showSchedule' })
  expect(state.keyword.value).toBe('课程')
  expect(state.statusFilter.value).toBe('all')
  state.handleModuleAction({ label: '新建通知' })
  expect(state.dialogVisible.value).toBe(true)
  state.handleModuleAction({ label: '导出报表' })
  expect(exportTableToExcel).toHaveBeenCalled()
  state.handleModuleAction({ label: '同步' })
  expect(ElMessage.success).toHaveBeenCalledWith('同步操作已完成')
  await nextTick()
  state.handleSelectionChange([state.records.value[0]])
  state.handleModuleAction({ command: 'batchStatus', targetStatus: '正常' })
  await flushPromises()
  expect(state.records.value[0].status).toBe('正常')
  state.handleModuleAction({ command: 'validatePermissions' })
  expect(ElMessage.success).toHaveBeenCalledWith('权限配置校验通过')
  state.handleModuleAction({ command: 'filterAbnormal' })
  expect(ElMessage.success).toHaveBeenCalledWith('当前没有异常或待复核记录')
})

it('form aliases submit separate identifiers and blank configuration remains usable', async () => {
  const state = await setup({ tableColumns: [{ prop: 'name', formProp: 'userId', label: '用户' }], defaultRecord: { name: '甲', userId: 7 } })
  state.openCreateDialog()
  expect(state.formModel.userId).toBe(7)
  await state.submitRecord()
  expect(state.records.value[0]).toMatchObject({ name: '甲', userId: 7 })
  service.list.mockResolvedValueOnce([])
  state.config.value = null
  await flushPromises()
  expect(state.statusSummary.value).toEqual([])
  expect(state.formFieldOptions.value).toEqual({})
  expect(state.recordName.value).toBe('记录')
  expect(state.batchTargetStatus.value).toBe('')
  state.handleSelectionChange([{ _recordId: 'missing' }])
  await state.applyBatchStatus()
  expect(ElMessage.warning).toHaveBeenCalledWith('当前业务暂无可流转状态')
})
