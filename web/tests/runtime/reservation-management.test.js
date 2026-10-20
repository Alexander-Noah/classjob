// @vitest-environment happy-dom
import { shallowMount as mountPage } from '@vue/test-utils'
import { panelStubs } from '../support/formStubs'
import Page from '../../src/views/reservations/ReservationManagement.vue'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
it('reservation page connects filter updates, creation and editor visibility',async()=>{const w=mountPage(Page,{global:{stubs:panelStubs()}});try{await flushPromises();const filters=w.findComponent({name:'ReservationFilterBar'});for(const [key,value] of [['keyword','课程'],['labFilter','all'],['sceneFilter','all'],['statusFilter','all'],['conflictOnly',false]])filters.vm.$emit(`update:${key}`,value);await flushPromises();w.findComponent({name:'ReservationManagementHeader'}).vm.$emit('create');await flushPromises();const editor=w.findComponent({name:'ReservationEditorDialog'});expect(editor.props('modelValue')).toBe(true);editor.vm.$emit('update:modelValue',false);await flushPromises();expect(editor.props('modelValue')).toBe(false)}finally{w.unmount()}})
import { flushPromises } from '@vue/test-utils'
import { useReservationManagement } from '../../src/composables/useReservationManagement'
import { labResourceService, reservationResourceService as service } from '../../src/api/services/basicResources'
import { listClassTimetables } from '../../src/api/services/classTimetable'
import { loadTeachingCalendar } from '../../src/modules/teaching-calendar/teachingCalendar'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage, ElMessageBox } from 'element-plus'

vi.mock('../../src/api/config', () => ({ API_USE_MOCK: false }))
vi.mock('../../src/api/services/basicResources', () => ({
  labResourceService: { list: vi.fn() },
  reservationResourceService: Object.fromEntries(['list', 'create', 'update', 'approve', 'reject', 'remove', 'reset', 'replace'].map(key => [key, vi.fn()])),
}))
vi.mock('../../src/api/services/classTimetable', () => ({ listClassTimetables: vi.fn() }))
vi.mock('../../src/modules/teaching-calendar/teachingCalendar', () => ({ loadTeachingCalendar: vi.fn(), getAcademicWeek: vi.fn(() => 3) }))
vi.mock('../../src/utils/exportExcel', () => ({ exportTableToExcel: vi.fn(() => 1) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn(), error: vi.fn() }, ElMessageBox: { confirm: vi.fn(), prompt: vi.fn() } }))
const scopes = []
it('detects timetable occupancy from legacy lesson text and ignores incomplete reservations',async()=>{
  listClassTimetables.mockResolvedValue([
    {classroom:'A101',weekday:'周一',sectionText:'1-2节',weekRaw:'3',courseName:'遗留课程'},
    {classroom:'A101',weekday:'周一',sectionCode:'3,4',weekText:'3'},
    {classroom:'A101',weekday:'周一',sectionText:'无节次'},
  ])
  service.list.mockResolvedValue([record(),record({id:2,timeRange:'invalid'}),record({id:3,lab:''}),record({id:4,date:''})])
  const state=await setup()
  state.detectConflicts()
  expect(state.reservations.value.find(row=>row.id===1)).toMatchObject({conflict:true,status:'冲突'})
  for(const id of [2,3,4]) expect(state.reservations.value.find(row=>row.id===id).conflict).toBe(false)
  expect(service.replace).not.toHaveBeenCalled()
})
it('keeps capacity conflicts visible and allows touching reservation intervals',async()=>{
  service.list.mockResolvedValue([record({attendees:41}),record({id:2,timeRange:'10:00-11:00'})])
  const state=await setup();state.detectConflicts()
  expect(state.reservations.value[0].conflict).toBe(true)
  expect(state.reservations.value[1].conflict).toBe(false)
})
const record = (overrides = {}) => ({ id: 1, applicant: '张同学', department: '信息学院', contact: '13800138000', lab: 'A101 机房', date: '2027-01-04', timeRange: '08:30-10:00', status: '待审核', attendees: 10, scene: '课程', reason: '程序设计课程', ...overrides })
beforeEach(() => {
  vi.resetAllMocks()
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-18T00:00:00Z'))
  labResourceService.list.mockResolvedValue([{ id: 7, code: 'A101', name: '机房', capacity: 40 }])
  loadTeachingCalendar.mockResolvedValue({ semester: '2026-2027-1' })
  listClassTimetables.mockResolvedValue([])
  service.list.mockResolvedValue([record()])
  service.create.mockImplementation(async data => ({ ...data, id: 2 }))
  service.update.mockImplementation(async (id, data) => ({ ...data, id }))
  service.approve.mockImplementation(async id => record({ id, status: '已通过' }))
  service.reject.mockImplementation(async id => record({ id, status: '已驳回' }))
  service.reset.mockResolvedValue([])
  service.replace.mockImplementation(async data => data)
  ElMessageBox.confirm.mockResolvedValue(true)
  ElMessageBox.prompt.mockResolvedValue({ value: '教学计划调整' })
})
afterEach(() => { for (const scope of scopes.splice(0)) scope.stop(); vi.useRealTimers() })
async function setup() {
  const scope = effectScope(); scopes.push(scope)
  const state = scope.run(() => useReservationManagement())
  await flushPromises()
  return state
}
function fill(state, overrides = {}) {
  Object.assign(state.formModel, record({ lab: state.labOptions.value[0], ...overrides }))
}

it('loads canonical lab names, semester timetable and derived metrics with working filters', async () => {
  service.list.mockResolvedValue([record(), record({ id: 2, applicant: '李同学', status: '已通过', scene: '考试' })])
  const state = await setup()
  expect(listClassTimetables).toHaveBeenCalledWith({ semester: '2026-2027-1', limit: 5000 })
  expect(state.selectedReservation.value.id).toBe(1)
  expect(state.metrics.value[0].value).toBe(2)
  expect(state.statusSummary.value.find(status => status.label === '待审核').count).toBe(1)
  state.keyword.value = '李同学'
  await flushPromises()
  expect(state.filteredReservations.value.map(row => row.id)).toEqual([2])
  expect(state.selectedReservationId.value).toBe(2)
  state.keyword.value = ''
  state.statusFilter.value = '待审核'
  expect(state.filteredReservations.value).toHaveLength(1)
  state.statusFilter.value = 'all'; state.sceneFilter.value = '考试'
  expect(state.filteredReservations.value).toHaveLength(1)
  state.sceneFilter.value = 'all'; state.labFilter.value = 'missing'
  expect(state.filteredReservations.value).toEqual([])
  state.labFilter.value = 'A101'; state.conflictOnly.value = true
  expect(state.filteredReservations.value).toEqual([])
  state.selectReservation(null)
  expect(state.selectedReservationId.value).toBe('')
  expect(state.sceneOptions).toContain('课程')
})

it('create starts empty, validates input and inserts only the saved server record', async () => {
  const state = await setup()
  state.openCreateDialog()
  expect(state.dialogTitle.value).toBe('新建预约申请')
  expect(state.formAvailability.value.isFree).toBe(false)
  await state.submitReservation()
  expect(service.create).not.toHaveBeenCalled()
  fill(state, { date: '2027-01-05' })
  await state.submitReservation()
  expect(service.create).toHaveBeenCalledWith(expect.objectContaining({ labId: 7, applicant: '张同学', status: '待审核' }))
  expect(service.create.mock.calls[0][0]).not.toHaveProperty('id')
  expect(state.reservations.value[0].id).toBe(2)
  expect(state.selectedReservationId.value).toBe(2)
  expect(state.dialogVisible.value).toBe(false)
  expect(state.formSubmitting.value).toBe(false)
})

it.each([
  [{ department: 'x' }], [{ contact: 'x' }], [{ lab: '' }], [{ lab: '不存在' }],
  [{ date: '' }], [{ timeRange: 'wrong' }], [{ timeRange: '10:00-08:00' }],
  [{ reason: 'x' }], [{ note: 'x' }],
])('invalid reservation form %j never sends a request', async patch => {
  const state = await setup(); state.openCreateDialog(); fill(state, { date: '2027-01-05', ...patch })
  await state.submitReservation()
  expect(service.create).not.toHaveBeenCalled()
  expect(ElMessage.warning).toHaveBeenCalled()
})

it('capacity watcher clamps headcount and direct excessive submissions reject', async () => {
  const state = await setup(); state.openCreateDialog(); fill(state, { date: '2027-01-05', attendees: 90 })
  expect(state.formAvailability.value.messages).toContain('预约人数超过实验室容量 40 人')
  await state.submitReservation()
  expect(service.create).not.toHaveBeenCalled()
  await nextTick()
  expect(state.formModel.attendees).toBe(40)
})

it('existing reservations block overlapping requests but exclude the record being edited', async () => {
  const state = await setup(); state.openCreateDialog(); fill(state)
  expect(state.formAvailability.value.reservationConflicts).toHaveLength(1)
  await state.submitReservation()
  expect(ElMessage.warning).toHaveBeenCalledWith('所选时段已有人工预约，请更换实验室、日期或时段')
  state.openEditDialog(state.reservations.value[0])
  expect(state.dialogTitle.value).toBe('编辑预约申请')
  expect(state.formAvailability.value.reservationConflicts).toEqual([])
  state.formModel.reason = '更新课程实训用途'
  await state.submitReservation()
  expect(service.update).toHaveBeenCalledWith(1, expect.objectContaining({ reason: '更新课程实训用途' }))
  expect(state.reservations.value[0].reason).toBe('更新课程实训用途')
})

it('timetable conflicts match week, weekday, lab token and lesson sections', async () => {
  listClassTimetables.mockResolvedValue([
    { classroom: 'A101', weekExpanded: '1-4', weekday: '周一', startSection: 1, endSection: 2, courseName: '程序设计', teacher: '教师甲', sectionText: '1-2节' },
    { classroom: 'A101', weekText: '5~8', weekday: '周一', sectionText: '1-2节' },
    { classroom: 'A101', weekRaw: '3', weekday: '周二', sectionCode: '1,2' },
    { classroom: 'XA1010', weekday: '周一', startSection: 1, endSection: 2 },
  ])
  const state = await setup(); state.openCreateDialog(); fill(state)
  expect(state.formAvailability.value.timetableConflicts).toHaveLength(1)
  expect(state.formAvailability.value.messages.some(message => message.includes('程序设计'))).toBe(true)
  await state.submitReservation()
  expect(ElMessage.warning).toHaveBeenCalledWith('所选时段已有课表占用，请更换实验室、日期或时段')
})

it('unavailable timetable blocks save and approval rather than assuming an empty schedule', async () => {
  listClassTimetables.mockRejectedValue(new Error('offline'))
  const state = await setup(); state.openCreateDialog(); fill(state, { date: '2027-01-05' })
  expect(state.formAvailability.value.isReady).toBe(false)
  await state.submitReservation()
  await state.approveReservation()
  expect(service.create).not.toHaveBeenCalled()
  expect(service.approve).not.toHaveBeenCalled()
  expect(ElMessage.warning).toHaveBeenCalledWith('课表数据暂不可用，无法确认实验室空闲')
})

it('save errors and mismatched server identities preserve prior list and dialog', async () => {
  const state = await setup(); state.openEditDialog(state.reservations.value[0])
  const previous = [...state.reservations.value]
  service.update.mockResolvedValueOnce(record({ id: 999 }))
  await state.submitReservation()
  expect(state.reservations.value).toEqual(previous)
  expect(state.dialogVisible.value).toBe(true)
  expect(ElMessage.error).toHaveBeenCalledWith('预约服务返回了错误的目标记录')
  service.update.mockRejectedValueOnce(new Error('保存失败'))
  await state.submitReservation()
  expect(state.reservations.value).toEqual(previous)
  expect(state.formSubmitting.value).toBe(false)
  state.openEditDialog(record({ status: '已取消' }))
  expect(ElMessage.warning).toHaveBeenCalledWith('当前状态不能编辑')
})

it('approve requires confirmation and applies the authoritative approved record', async () => {
  const state = await setup()
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.approveReservation()
  expect(service.approve).not.toHaveBeenCalled()
  await state.approveReservation()
  expect(service.approve).toHaveBeenCalledWith(1, expect.objectContaining({ reviewerName: '张明', reviewRemark: expect.stringContaining('审核通过') }))
  expect(state.selectedReservation.value.status).toBe('已通过')
  await state.approveReservation()
  expect(ElMessage.warning).toHaveBeenCalledWith('只能通过待审核预约')
})

it('approve blocks expired, conflicting, over-capacity and overlapping records', async () => {
  const state = await setup()
  for (const patch of [{ date: '2020-01-01' }, { conflict: true }, { status: '冲突' }, { attendees: 41 }]) await state.approveReservation(record(patch))
  state.reservations.value.push(record({ id: 2 }))
  await state.approveReservation(state.reservations.value[0])
  expect(service.approve).not.toHaveBeenCalled()
  expect(ElMessage.warning).toHaveBeenCalledWith('该预约已过期，无法通过')
  expect(ElMessage.warning).toHaveBeenCalledWith('预约人数超过实验室容量 40 人，不能通过')
})

it.each(['approve', 'reject', 'cancel'])('%s preserves records on API error or prompt cancellation', async action => {
  const state = await setup()
  const method = `${action}Reservation`
  const previous = [...state.reservations.value]
  if (action === 'approve') service.approve.mockRejectedValueOnce(new Error('审核失败'))
  else if (action === 'reject') service.reject.mockRejectedValueOnce(new Error('审核失败'))
  else service.update.mockRejectedValueOnce(new Error('审核失败'))
  await state[method]()
  expect(state.reservations.value).toEqual(previous)
  expect(ElMessage.error).toHaveBeenCalledWith('审核失败')
  if (action === 'approve') ElMessageBox.confirm.mockRejectedValueOnce('close')
  else ElMessageBox.prompt.mockRejectedValueOnce('cancel')
  await state[method]()
  expect(state.reservations.value).toEqual(previous)
})

it.each(['reject', 'cancel'])('%s requires a reason, validates prompt text and stores the saved record', async action => {
  const state = await setup()
  ElMessageBox.prompt.mockResolvedValueOnce({ value: ' ' })
  await state[`${action}Reservation`]()
  expect(ElMessage.warning).toHaveBeenCalledWith(action === 'reject' ? '请填写驳回原因' : '请填写取消原因')
  const validator = ElMessageBox.prompt.mock.calls[0][2].inputValidator
  expect(validator('')).not.toBe(true)
  expect(validator('教学计划调整')).toBe(true)
  await state[`${action}Reservation`]()
  expect(state.selectedReservation.value.status).toBe(action === 'reject' ? '已驳回' : '已取消')
  await state[`${action}Reservation`]()
  expect(ElMessage.warning).toHaveBeenCalledWith(action === 'reject' ? '当前状态不能驳回' : '当前状态不能取消预约')
})

it('delete reloads server records and handles cancel and failure', async () => {
  const state = await setup()
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.deleteReservation(state.reservations.value[0])
  expect(service.remove).not.toHaveBeenCalled()
  service.remove.mockRejectedValueOnce(new Error('删除失败'))
  await state.deleteReservation(state.reservations.value[0])
  expect(ElMessage.error).toHaveBeenCalledWith('删除失败')
  service.list.mockResolvedValueOnce([])
  await state.deleteReservation(state.reservations.value[0])
  expect(state.reservations.value).toEqual([])
  await state.approveReservation(); await state.rejectReservation(); await state.cancelReservation(); await state.deleteReservation(null)
})

it('detect conflicts marks overlapping records and restores resolved conflicts without writing server state', async () => {
  service.list.mockResolvedValue([record(), record({ id: 2, timeRange: '09:00-11:00' }), record({ id: 3, status: '已取消' }), record({ id: 4, lab: 'other', status: '冲突', conflict: true })])
  const state = await setup()
  state.detectConflicts()
  expect(state.reservations.value.slice(0, 2).every(row => row.conflict && row.status === '冲突')).toBe(true)
  expect(state.reservations.value[2].conflict).toBe(false)
  expect(state.reservations.value[3]).toMatchObject({ conflict: false, status: '待审核' })
  expect(service.replace).not.toHaveBeenCalled()
  expect(state.conflictOnly.value).toBe(true)
  state.reservations.value = [record()]
  state.detectConflicts()
  expect(ElMessage.success).toHaveBeenCalledWith('未发现时间或资源冲突')
})

it('export uses filtered records and reset honors cancel before restoring initial state', async () => {
  const state = await setup()
  state.exportReservations()
  const config = exportTableToExcel.mock.calls[0][0]
  expect(config.rows).toHaveLength(1)
  const conflictColumn = config.columns.find(column => column.label === '冲突状态')
  expect(conflictColumn.formatter({ conflict: true })).toBe('存在冲突')
  expect(conflictColumn.formatter({ conflict: false })).toBe('无冲突')
  state.keyword.value = 'missing'
  state.exportReservations()
  expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的预约记录')
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.resetReservations()
  expect(service.reset).not.toHaveBeenCalled()
  await state.resetReservations(); await flushPromises()
  expect(state.keyword.value).toBe('')
  expect(state.reservations.value).toEqual([])
})
