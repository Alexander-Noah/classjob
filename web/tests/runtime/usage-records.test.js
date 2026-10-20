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

it.each([['历史归档','已归档'],['设备异常','异常'],['需要复核','待复核'],['正在使用','使用中'],['未知','正常']])('normalizes legacy status %s preserving assigned record numbers',async(status,expected)=>{
api.list.mockResolvedValueOnce([{id:'UR-legacy',recordCode:'EXT-42',status,useTime:'2026-10-18T08:30:00',resource:'陌生实验室',scene:'特殊场景',remark:'现场说明'}]);const state=await setup();expect(state.records.value[0]).toMatchObject({displayId:'EXT-42',status:expected,entryTime:'10-18 08:30',lab:'陌生实验室',type:'特殊场景',note:'现场说明',readonly:true});expect(state.metrics.value).toHaveLength(4)
})
it('ignores absent selection without prompting or synchronizing stale records',async()=>{const state=await setup();state.markReviewed(null);await state.markAbnormal(null);await state.archiveRecord(null);state.closeUsingRecord(null);await flushPromises();expect(ElMessageBox.prompt).not.toHaveBeenCalled();expect(ElMessageBox.confirm).not.toHaveBeenCalled();expect(api.action).not.toHaveBeenCalled()})
it.each(['null','[]','42'])('ignores invalid stored patch container %s and tolerates lab discovery outages',async(stored)=>{localStorage.setItem('smart-lab:usage-records:local-patches:v1',stored);labResourceService.list.mockRejectedValueOnce(new Error('offline'));api.list.mockResolvedValueOnce([{id:'UR-direct',status:'正常'}]);const state=await setup();expect(state.records.value[0]).toMatchObject({displayId:'UR-direct',lab:'-',entryTime:'-'});expect(state.labOptions.value).toContain('A101 云计算实验室')})
it('filters full and blank dates and moves selection to the visible record',async()=>{api.list.mockResolvedValueOnce([{id:1,entryTime:'2026-10-17 08:00'},{id:2,entryTime:'2026-10-18 09:00'},{id:3,entryTime:'-'}]);const state=await setup();state.dateRange.value=['2026-10-18','2026-10-18'];await flushPromises();expect(state.filteredRecords.value.map(row=>row.id)).toEqual(['2']);expect(state.selectedRecordId.value).toBe('2');state.dateRange.value=[undefined,'2026-10-17'];await flushPromises();expect(state.filteredRecords.value.map(row=>row.id)).toEqual(['1','3']);state.dateRange.value=null;expect(state.filteredRecords.value).toHaveLength(3)})
it('uses seeded records for empty fetch and reset responses',async()=>{api.list.mockResolvedValueOnce([]);labResourceService.list.mockResolvedValueOnce(null);const state=await setup();expect(state.records.value).toHaveLength(5);api.reset.mockResolvedValueOnce([]);await state.resetRecords();expect(state.records.value).toHaveLength(5);expect(ElMessage.success).toHaveBeenCalledWith('已恢复使用记录初始数据')})
it.each([['10-18 11:00','已结束'],['2026-99-99','已结束'],['2026-10-18','10小时30分钟'],['-','已结束']])('ending usage at boundary entry %s produces %s',async(entryTime,duration)=>{api.list.mockResolvedValueOnce([{id:1,entryTime}]);const state=await setup();state.closeUsingRecord();await flushPromises();expect(state.records.value[0].duration).toBe(duration)})

it('preserves unformatted legacy times and creates a stable display identity for missing server IDs', async () => {
  api.list.mockResolvedValueOnce([{ time: '刚刚', status: '使用中' }, { id: 'legacy', time: '2026-10-18T09:45:00' }])
  const state = await setup()
  expect(state.records.value[0]).toMatchObject({ entryTime: '刚刚', readonly: true })
  expect(state.records.value[0].displayId).toMatch(/^UR-/)
  expect(state.records.value[1]).toMatchObject({ displayId: 'legacy', entryTime: '10-18 09:45' })
  state.selectedRecordId.value = 'missing'
  expect(state.selectedRecord.value.id).toBe(state.records.value[0].id)
  state.records.value = []
  expect(state.selectedRecord.value).toBeNull()
})

it('abnormal-only filtering excludes healthy rows and optional search fields tolerate null', async () => {
  api.list.mockResolvedValueOnce([{ id: 1, status: '正常' }, { id: 2, status: '异常', person: '甲' }])
  const state = await setup()
  state.abnormalOnly.value = true
  expect(state.filteredRecords.value.map(row => row.id)).toEqual(['2'])
  state.records.value.push({ id: '3', status: '异常', person: null, resource: null, lab: null, type: null })
  state.keyword.value = '不存在'
  expect(state.filteredRecords.value).toEqual([])
  state.keyword.value = ''
  await state.markAbnormal(state.records.value[0])
  const validate = ElMessageBox.prompt.mock.calls[0][2].inputValidator
  expect(validate(null)).toBe('异常原因不能为空')
})
