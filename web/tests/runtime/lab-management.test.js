import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useLabManagement } from '../../src/composables/useLabManagement'
import { labResourceService as service } from '../../src/api/services/basicResources'
import { listUserOptions } from '../../src/api/services/users'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage, ElMessageBox } from 'element-plus'

vi.mock('../../src/api/services/basicResources', () => ({ labResourceService: Object.fromEntries(['list', 'create', 'update', 'replace', 'remove', 'reset'].map(key => [key, vi.fn()])) }))
vi.mock('../../src/api/services/users', () => ({ listUserOptions: vi.fn() }))
vi.mock('../../src/utils/exportExcel', () => ({ exportTableToExcel: vi.fn(() => 2) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn(), error: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
const scopes = []
it('legacy timetable lesson ranges and lab names bind safely with sorted deduplicated schedules',async()=>{
  const state=await setup()
  const base={classroom:'机房',weekday:'周一',weekRaw:'第3周',teacher:'教师乙'}
  state.bindLabSchedules([
    {...base,sectionCode:'1,2',courseName:'甲课程'},
    {...base,sectionText:'1-4节',weekText:'2~4周',courseName:'长课程'},
    {...base,sectionText:'3-4节',weekExpanded:'3',courseName:'后续课程'},
    {...base,sectionText:'1,2节',courseName:'乙课程'},
    {...base,sectionText:'无节次',courseName:'损坏节次'},
    {...base,sectionCode:'21',courseName:'自定义节次'},
    {...base,classroom:'',sectionCode:'1'},
    {...base,classroom:'A1010',sectionCode:'1'},
    {...base,classroom:'A101',sectionCode:'1',weekText:'1周'},
  ],{week:3,weekday:1,section:1,currentMinute:505})
  expect(state.selectedLab.value.schedule).toHaveLength(3);expect(state.selectedLab.value.schedule[2]).toContain('长课程')
  expect(state.selectedLab.value.schedule[0]).toContain('第1节')
  expect(state.selectedLab.value.status).toBe('使用中')
  state.bindLabSchedules([{...base,sectionCode:'21',courseName:'自定义节次'},{...base,sectionText:'无节次',courseName:'损坏节次'}],{week:3,weekday:1,currentMinute:505})
  expect(state.selectedLab.value.schedule.find(item=>item.includes('自定义节次'))).toContain('第11节')
  expect(state.selectedLab.value.schedule.find(item=>item.includes('损坏节次'))).not.toContain('第0节')
  expect(state.selectedLab.value.status).toBe('开放')

})
it('approved legacy reservation identities bind by name and validated time while rejecting unrelated data',async()=>{
  const state=await setup(),date='2027-01-04'
  state.bindLabSchedules([], {date,currentMinute:535},[
    {labName:'机房',reservationDate:date,timeSlot:'09:00-10:00',status:'approved',type:'竞赛',userName:'姓名甲'},
    {labDisplayName:'A101',useDate:date,period:'08:00-09:00',status:'APPROVED',reservationType:'考试',contact:'电话乙'},
    {roomName:'A101',startDate:date,startTime:'10:00',finishTime:'11:00',status:'已通过'},
    {classroom:'A101',date,beginTime:'11:00',endTime:'12:00',status:'已通过'},
    {lab:'A101',date,status:'已通过',timeRange:'invalid'},
    {lab:'A101',date,status:'已通过',timeRange:'12:00-11:00'},
    {lab:'A101',date,status:'已通过'},
    {lab:'A1010',date,status:'已通过',timeRange:'09:00-10:00'},
    {date,status:'已通过',timeRange:'09:00-10:00'},
    {lab:'A101',date,status:'待审核',timeRange:'09:00-10:00'},
  ])
  expect(state.selectedLab.value.schedule).toHaveLength(6)
  expect(state.selectedLab.value.schedule.some(item=>item.includes('姓名甲'))).toBe(true)
  expect(state.selectedLab.value.schedule.some(item=>item.includes('电话乙'))).toBe(true)
  expect(state.selectedLab.value.status).toBe('使用中')
  state.bindLabSchedules([], {date:'',currentMinute:535})
  expect(state.selectedLab.value.schedule).toEqual(['今日暂无排课'])
})
const labs = () => [
  { id: 7, labCode: 'A101', labName: '机房', building: '信息楼', roomNo: 'A101', capacity: '40', managerUserId: 1, features: '课程，实训', deviceCount: 5 },
  { id: 8, code: 'B102', name: '科研室', building: '科研楼', status: '维护中', manager: '教师乙', type: '科研创新', features: ['科研'] },
]
beforeEach(() => {
  vi.resetAllMocks()
  listUserOptions.mockResolvedValue([{ id: 1, realName: '教师甲' }])
  service.list.mockResolvedValue(labs())
  service.create.mockImplementation(async data => ({ ...data, id: 9 }))
  service.update.mockImplementation(async (id, data) => ({ ...data, id }))
  service.replace.mockImplementation(async data => data)
  service.remove.mockResolvedValue([])
  service.reset.mockResolvedValue(labs())
  ElMessageBox.confirm.mockResolvedValue(true)
})
afterEach(() => { for (const scope of scopes.splice(0)) scope.stop() })
async function setup() { const scope = effectScope(); scopes.push(scope); const state = scope.run(() => useLabManagement()); await flushPromises(); return state }

it('normalizes backend lab fields and derives options, filters, summaries and selection', async () => {
  const state = await setup()
  expect(state.selectedLab.value).toMatchObject({ id: 7, code: 'A101', name: '机房', room: 'A101', capacity: 40, manager: '教师甲', devices: 5, features: ['课程', '实训'] })
  expect(state.buildingOptions.value).toEqual(['信息楼', '科研楼'])
  expect(state.statusSummary.value.find(option => option.label === '开放').count).toBe(1)
  state.keyword.value = '科研'
  await nextTick()
  expect(state.filteredLabs.value.map(lab => lab.id)).toEqual([8])
  expect(state.selectedLabId.value).toBe(8)
  state.keyword.value = ''; state.buildingFilter.value = '信息楼'
  expect(state.filteredLabs.value.map(lab => lab.id)).toEqual([7])
  state.statusFilter.value = '维护中'
  expect(state.filteredLabs.value).toEqual([])
  state.buildingFilter.value = 'all'; state.statusFilter.value = 'all'; state.typeFilter.value = '科研创新'
  expect(state.filteredLabs.value).toHaveLength(1)
  state.availableOnly.value = true
  expect(state.filteredLabs.value).toEqual([])
  state.selectLab({ id: 7 })
  expect(state.selectedLab.value.id).toBe(7)
  expect(state.typeOptions).toContain('基础机房')
  expect(state.safetyOptions).toContain('告警')
})

it('create validates required fields, resolves manager and posts server contract', async () => {
  const state = await setup(); state.openCreateDialog()
  expect(state.dialogTitle.value).toBe('新增实验室')
  await state.submitLab()
  expect(ElMessage.warning).toHaveBeenCalledWith('请填写实验室编号')
  Object.assign(state.formModel, { code: 'C103', name: '新机房', building: '新楼', managerUserId: 1, featuresText: '计算，教学' })
  await state.submitLab()
  expect(service.create).toHaveBeenCalledWith(expect.objectContaining({ labCode: 'C103', labName: '新机房', managerUserId: 1, managerName: '教师甲', capacity: 48 }))
  expect(service.create.mock.calls[0][0]).not.toHaveProperty('id')
  expect(state.selectedLab.value).toMatchObject({ id: 9, name: '新机房', features: ['计算', '教学'] })
  expect(state.dialogVisible.value).toBe(false)
  expect(state.formSubmitting.value).toBe(false)
})

it('edit preserves device metadata and failure restores the prior lab list', async () => {
  const state = await setup(); state.openEditDialog()
  expect(state.dialogTitle.value).toBe('编辑实验室')
  state.formModel.name = '更新机房'
  await state.submitLab()
  expect(service.update).toHaveBeenCalledWith(7, expect.objectContaining({ id: 7, labName: '更新机房' }))
  expect(state.selectedLab.value).toMatchObject({ name: '更新机房', devices: 5 })
  const previous = [...state.filteredLabs.value]
  state.openEditDialog(); state.formModel.name = '失败修改'
  service.update.mockRejectedValueOnce(new Error('保存失败'))
  await state.submitLab()
  expect(state.filteredLabs.value).toEqual(previous)
  expect(state.dialogVisible.value).toBe(true)
  expect(ElMessage.error).toHaveBeenCalledWith('保存失败')
  service.update.mockRejectedValueOnce({ __handled: true })
  ElMessage.error.mockClear()
  await state.submitLab()
  expect(ElMessage.error).not.toHaveBeenCalled()
})

it('delete handles cancellation, server failure and successful refreshed list', async () => {
  const state = await setup()
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.deleteLab()
  expect(service.remove).not.toHaveBeenCalled()
  service.remove.mockRejectedValueOnce(new Error('删除失败'))
  await state.deleteLab()
  expect(state.filteredLabs.value).toHaveLength(2)
  expect(state.selectedLab.value.id).toBe(7)
  expect(ElMessage.error).toHaveBeenCalledWith('删除失败')
  service.remove.mockResolvedValueOnce([labs()[1]])
  await state.deleteLab()
  expect(state.filteredLabs.value.map(lab => lab.id)).toEqual([8])
  expect(state.selectedLabId.value).toBe(8)
  await state.deleteLab()
  expect(state.selectedLab.value).toBeUndefined()
  await state.deleteLab(); state.openEditDialog(); await state.changeLabStatus('开放')
})

it('status changes persist server field names and export uses current filtered records', async () => {
  const state = await setup()
  await state.changeLabStatus('停用')
  expect(service.replace).toHaveBeenCalledWith(expect.arrayContaining([expect.objectContaining({ id: 7, openStatus: '停用', managerUserId: 1 })]))
  expect(state.selectedLab.value.status).toBe('停用')
  state.exportLabs()
  expect(exportTableToExcel.mock.calls[0][0]).toMatchObject({ filename: '实验室清单', rows: expect.any(Array) })
  state.keyword.value = 'missing'; state.exportLabs()
  expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的实验室数据')
})

it('reset preserves filters on cancellation then restores them with server records', async () => {
  const state = await setup()
  state.keyword.value = 'missing'; state.availableOnly.value = true
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await state.resetLabs()
  expect(state.keyword.value).toBe('missing')
  await state.resetLabs()
  expect(state.keyword.value).toBe('')
  expect(state.availableOnly.value).toBe(false)
  expect(state.selectedLabId.value).toBe(7)
})

it('schedule binding deduplicates, sorts, applies weeks and weekdays and locks maintenance status', async () => {
  const state = await setup()
  const course = { classroom: 'A101', weekday: '周一', weekExpanded: '1-5', sectionText: '1-2节', startSection: 1, endSection: 2, courseName: '程序设计', teacher: '教师甲' }
  state.bindLabSchedules([
    course, { ...course },
    { ...course, courseName: '第二节', startSection: 3, endSection: 4, sectionText: '3-4节' },
    { ...course, courseName: '其他周', weekExpanded: '6,7' },
    { ...course, courseName: '其他日', weekday: '周二' },
    { ...course, classroom: 'XA1010', courseName: '其他实验室' },
  ], { week: 3, weekday: 1, currentMinute: 505, date: '2027-01-04' })
  expect(state.selectedLab.value.status).toBe('使用中')
  expect(state.selectedLab.value.scheduleCount).toBe(2)
  expect(state.selectedLab.value.schedule[0]).toContain('程序设计')
  expect(state.selectedLab.value.schedule[1]).toContain('第二节')
  state.selectLab({ id: 8 })
  expect(state.selectedLab.value.status).toBe('维护中')
  state.selectLab({ id: 7 })
  state.bindLabSchedules([course], { week: 3, weekday: 1, section: 3, currentMinute: 700, isToday: false })
  expect(state.selectedLab.value.schedule).toEqual(['今日暂无排课'])
  expect(state.selectedLab.value.status).toBe('使用中')
  state.bindLabSchedules([], { week: 3, weekday: 1, currentMinute: 700 })
  expect(state.selectedLab.value.status).toBe('开放')
})

it('approved reservations are included only for the selected lab and date and affect busy status', async () => {
  const state = await setup()
  const reservation = { lab: 'A101', date: '2027-01-04', timeRange: '09:00-10:00', status: '已通过', scene: '考试', applicant: '张同学' }
  state.bindLabSchedules([], { date: '2027-01-04', currentMinute: 535 }, [
    reservation, { ...reservation, timeRange: '08:00-09:00', applicant: '李同学' },
    { ...reservation, status: '待审核' }, { ...reservation, date: '2027-01-05' }, { ...reservation, lab: 'B999' },
  ])
  expect(state.selectedLab.value.status).toBe('使用中')
  expect(state.selectedLab.value.schedule).toHaveLength(2)
  expect(state.selectedLab.value.schedule[0]).toContain('李同学')
  state.bindLabSchedules(null, {}, null)
  expect(state.selectedLab.value.schedule).toEqual(['今日暂无排课'])
})

it('manager lookup failure does not block labs and an empty list remains safe', async () => {
  listUserOptions.mockRejectedValue(new Error('offline'))
  service.list.mockResolvedValue([])
  const state = await setup()
  expect(state.managerOptions.value).toEqual([])
  expect(state.selectedLab.value).toBeUndefined()
  state.exportLabs()
  expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的实验室数据')
})

it('username-only managers and unstructured transport failures keep lab edits recoverable',async()=>{
 listUserOptions.mockResolvedValue([{id:1,username:'teacher1'},null,{id:2}])
 const state=await setup();state.openEditDialog();service.update.mockRejectedValueOnce({})
 await state.submitLab();expect(ElMessage.error).toHaveBeenLastCalledWith('保存实验室失败')
 expect(state.dialogVisible.value).toBe(true);expect(state.formSubmitting.value).toBe(false)
 service.remove.mockRejectedValueOnce({__handled:true});ElMessage.error.mockClear()
 await state.deleteLab();expect(ElMessage.error).not.toHaveBeenCalled();expect(state.selectedLab.value.id).toBe(7)
 service.remove.mockRejectedValueOnce({});await state.deleteLab()
 expect(ElMessage.error).toHaveBeenLastCalledWith('删除实验室失败')
})
it('unknown status and sparse room identity still permit browsing and empty timetable binding',async()=>{
 service.list.mockResolvedValue([{id:'legacy-room',labName:'无编码房间',status:'待巡检'}])
 const state=await setup();expect(state.selectedLab.value.status).toBe('开放')
 state.keyword.value='无编码';expect(state.filteredLabs.value).toHaveLength(1)
 state.bindLabSchedules([{classroom:'其他房间',weekday:'周一',weekRaw:'1',sectionCode:'1',courseName:'课程'}],{weekday:1})
 expect(state.selectedLab.value.schedule).toEqual(['今日暂无排课'])
 service.reset.mockResolvedValue([]);await state.resetLabs();expect(state.selectedLabId.value).toBe('')
 expect(state.selectedLab.value).toBeUndefined()
 state.bindLabSchedules([],{},[])
})