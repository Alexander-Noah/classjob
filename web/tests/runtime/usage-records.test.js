// @vitest-environment happy-dom
import { shallowMount as mountPage } from '@vue/test-utils'
import { panelStubs } from '../support/formStubs'
import Page from '../../src/views/records/UsageRecords.vue'
// @vitest-environment happy-dom
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useUsageRecords } from '../../src/composables/useUsageRecords'
import { usageRecordResourceService as api, labResourceService } from '../../src/api/services/basicResources'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage, ElMessageBox } from 'element-plus'
vi.mock('../../src/api/services/basicResources', () => ({ usageRecordResourceService: { list: vi.fn(), action: vi.fn(), reset: vi.fn() }, labResourceService: { list: vi.fn() } }))
vi.mock('../../src/utils/exportExcel', () => ({ exportTableToExcel: vi.fn(() => 2) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn() }, ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() } }))
const scopes = []
it('usage page binds filters and routes record review through table actions',async()=>{const w=mountPage(Page,{global:{stubs:panelStubs()}});try{await flushPromises();const filter=w.findComponent({name:'UsageRecordFilterBar'});for(const [key,value] of [['keyword','张同学'],['labFilter','A101 机房'],['statusFilter','all'],['typeFilter','all'],['dateRange',[]],['abnormalOnly',false]])filter.vm.$emit(`update:${key}`,value);await flushPromises();const table=w.findComponent({name:'UsageRecordListTable'});expect(table.props('records').length).toBeGreaterThan(0);table.vm.$emit('review',table.props('records')[0]);await flushPromises();expect(api.action).toHaveBeenCalled();w.findComponent({name:'UsageRecordsHeader'}).vm.$emit('export');expect(exportTableToExcel).toHaveBeenCalled()}finally{w.unmount()}})
const rows = () => [{ id: 1, person: '张同学', lab: 'A101 机房', type: '课程上机', entryTime: '10-18 08:00', status: '待复核' }, { id: 2, person: '李同学', resource: 'A101 机房', type: '自主使用', status: '使用中', entryTime: '10-18 09:00' }, { id: 'timetable-3', person: '课表班级', status: '正常' }]
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-10-18T10:30:00'))
  api.list.mockResolvedValue(rows()); api.reset.mockResolvedValue(rows()); api.action.mockResolvedValue({})
  labResourceService.list.mockResolvedValue([{ id: 7, code: 'A101', name: '机房' }]); ElMessageBox.prompt.mockResolvedValue({ value: '设备尚未归还' }); ElMessageBox.confirm.mockResolvedValue(true)
})
afterEach(() => { scopes.splice(0).forEach(scope => scope.stop()); vi.useRealTimers() })
async function setup() { const scope = effectScope(); scopes.push(scope); const state = scope.run(() => useUsageRecords()); await flushPromises(); return state }
it('normalizes records, read-only sources and record numbers and computes metrics and filters', async () => {
  const state = await setup()
  expect(state.records.value[0]).toMatchObject({ id: '1', displayId: 'UR-0001', readonly: false, abnormalReason: '待补充异常原因', tagType: 'warning' })
  expect(state.records.value[2].readonly).toBe(true); expect(state.records.value[1].lab).toBe('A101 机房')
  expect(state.metrics.value[1].value).toBe('1'); expect(state.statusSummary.value[2].count).toBe(1)
  expect(state.typeOptions.value).toContain('自主使用'); expect(state.labOptions.value).toContain('A101 机房')
  state.keyword.value = '李同学'; await flushPromises(); expect(state.selectedRecord.value.id).toBe('2')
  state.keyword.value = ''; state.statusFilter.value = '使用中'; state.typeFilter.value = '自主使用'; state.labFilter.value = 'A101 机房'; state.dateRange.value = ['2026-10-18','2026-10-18']
  expect(state.filteredRecords.value).toHaveLength(1)
  state.filterAbnormalRecords(); expect(state.abnormalOnly.value).toBe(true)
  state.resetFilters(); expect(state.filteredRecords.value).toHaveLength(3)
  state.selectRecord(null); expect(state.selectedRecordId.value).toBe('')
})
it('review and abnormal annotations persist local patches and send server status actions', async () => {
  const state = await setup(); state.markReviewed(); await flushPromises()
  expect(state.records.value[0]).toMatchObject({ status: '正常', accessResult: '复核通过', abnormalReason: '-' })
  expect(api.action).toHaveBeenCalledWith('1', 'review', expect.objectContaining({ status: '正常' }))
  expect(JSON.parse(localStorage.getItem('smart-lab:usage-records:local-patches:v1'))['1'].status).toBe('正常')
  await state.markAbnormal(); await flushPromises()
  expect(state.selectedRecord.value).toMatchObject({ status: '异常', abnormalReason: '设备尚未归还' })
  const validator = ElMessageBox.prompt.mock.calls[0][2].inputValidator
  expect(validator(' ')).toBe('异常原因不能为空'); expect(validator('有效原因')).toBe(true)
  ElMessageBox.prompt.mockRejectedValueOnce('cancel'); const before = state.records.value[0]
  await state.markAbnormal(); expect(state.records.value[0]).toEqual(before)
})
it('read-only synthetic records reject mutations and remote outages retain local patches', async () => {
  const state = await setup(); state.markReviewed(state.records.value[2]); await flushPromises()
  expect(api.action).not.toHaveBeenCalled(); expect(ElMessage.warning).toHaveBeenCalledWith('课表和预约来源记录为只读展示，请在对应业务模块处理')
  api.action.mockRejectedValueOnce(new Error('offline')); state.markReviewed(state.records.value[0]); await flushPromises()
  expect(state.records.value[0].status).toBe('正常'); expect(ElMessage.warning).toHaveBeenCalledWith('页面状态已更新，后端状态同步稍后可重试')
})
it('finish records duration and archive requires confirmation before updating status', async () => {
  const state = await setup(); state.closeUsingRecord(state.records.value[1]); await flushPromises()
  expect(state.records.value[1]).toMatchObject({ status: '正常', leaveTime: '10-18 10:30', duration: '1小时30分钟' })
  ElMessageBox.confirm.mockRejectedValueOnce('cancel'); await state.archiveRecord(); expect(state.selectedRecord.value.status).toBe('正常')
  await state.archiveRecord(); await flushPromises(); expect(state.selectedRecord.value.status).toBe('已归档')
})
it.each([['10-18 10:00','30分钟'], ['10-18 08:30','2小时'], ['bad','已结束']])('finish computes duration for %s', async (entryTime, duration) => {
  api.list.mockResolvedValueOnce([{ ...rows()[0], entryTime }]); const state = await setup(); state.closeUsingRecord(); await flushPromises(); expect(state.records.value[0].duration).toBe(duration)
})
it('loads saved local review patches and resets them only after confirmed reset', async () => {
  localStorage.setItem('smart-lab:usage-records:local-patches:v1', JSON.stringify({ 1: { status: '已归档' } }))
  const state = await setup(); expect(state.records.value[0].status).toBe('已归档')
  ElMessageBox.confirm.mockRejectedValueOnce('cancel'); await state.resetRecords(); expect(api.reset).not.toHaveBeenCalled()
  await state.resetRecords(); expect(state.records.value[0].status).toBe('待复核'); expect(localStorage.getItem('smart-lab:usage-records:local-patches:v1')).toBe('{}')
})
it('exports filtered data, handles empty results and safely loads malformed local patches', async () => {
  localStorage.setItem('smart-lab:usage-records:local-patches:v1', 'broken')
  const state = await setup(); state.exportRecords(); expect(exportTableToExcel.mock.calls[0][0].rows).toHaveLength(3)
  state.keyword.value = 'missing'; state.exportRecords(); expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的使用记录')
})

