// @vitest-environment happy-dom
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import ScanReservation from '../../src/views/reservations/ScanReservation.vue'
import * as api from '../../src/api/services/scanReservation'
import { getStoredUser, isAuthenticated } from '../../src/api/services/auth'
import { loadTeachingCalendar } from '../../src/modules/teaching-calendar/teachingCalendar'
import { ElMessage } from 'element-plus'
import { panelStubs as formStubs } from '../support/formStubs'
const route = vi.hoisted(() => ({ query: { labId: '7', code: 'test-code' } }))
vi.mock('vue-router', () => ({ useRoute: () => route }))
vi.mock('../../src/api/services/auth', () => ({ getStoredUser: vi.fn(), isAuthenticated: vi.fn() }))
vi.mock('../../src/api/services/scanReservation', () => Object.fromEntries(['getScanReservationProfile','getScanReservationStatus','submitScanReservation'].map(key => [key, vi.fn()])))
vi.mock('../../src/modules/teaching-calendar/teachingCalendar', () => ({ loadTeachingCalendar: vi.fn(), getAcademicWeek: () => 3 }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() } }))
const wrappers = []
it('rendered quick periods and query contact controls update the submitted request',async()=>{
  const vm=await setup(true),w=wrappers.at(-1)
  const period=w.findAll('.period-card')[1]
  await period.trigger('click');expect(vm.formModel.timeRange).toBe(vm.quickSlots[1].value)
  await w.get('input[placeholder="提交时的联系方式"]').setValue('student@example.com')
  await w.get('input[placeholder="预约编号"]').setValue('9')
  await vm.handleStatusLookup()
  expect(api.getScanReservationStatus).toHaveBeenCalledWith({id:'9',contact:'student@example.com'})
  vm.handleContinue();await flushPromises()
  const note=w.get('textarea[placeholder="如需软件环境、设备支持或特殊安排，可在此说明"]')
  await note.setValue('需要开发软件')
  expect(vm.formModel.note).toBe('需要开发软件')
})
it('missing ticket fields and lookup failures keep the QR page usable',async()=>{
  const vm=await setup()
  expect(vm.submittedSummary).toEqual([])
  vm.submittedReservation={};expect(vm.submittedSummary[0].value).toBe('-')
  vm.submittedReservation=null;vm.formModel.timeRange='';expect(vm.timeRangeMessage).toBe('')
  api.getScanReservationProfile.mockRejectedValueOnce({});await vm.loadLabProfile()
  expect(vm.loadError).toBe('实验室信息加载失败');expect(vm.loading).toBe(false)
})
const reservation = () => ({ id: 9, status: '待审核', lab: 'A101 机房', applicant: '张同学', date: '2026-10-19', timeRange: '08:30-10:00' })
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); vi.useFakeTimers({ toFake: ['setInterval','clearInterval','Date'] }); vi.setSystemTime(new Date('2026-10-18T09:00:00'))
  route.query = { labId: '7', code: 'test-code' }
  api.getScanReservationProfile.mockResolvedValue({ id: 7, code: 'A101', name: '机房', capacity: 40, status: '开放', openTime: '08:00-22:00', building: '信息楼', floor: '一层', room: '101' })
  api.submitScanReservation.mockResolvedValue(reservation()); api.getScanReservationStatus.mockResolvedValue(reservation())
  getStoredUser.mockReturnValue({ realName: '张同学', department: '信息学院', phone: '13800138000' }); isAuthenticated.mockReturnValue(true); loadTeachingCalendar.mockResolvedValue({})
  vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()
})
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()); vi.clearAllTimers(); vi.useRealTimers(); vi.restoreAllMocks() })
async function setup(render = false) {
  const names = ['button','card','icon','tag','alert','form','form-item','input','input-number','select','option','date-picker','time-picker','checkbox','steps','step','result','divider','descriptions','descriptions-item']
  const wrapper = shallowMount(ScanReservation, { global: { directives: { loading: {} }, stubs: render ? formStubs() : Object.fromEntries(names.map(name => [`el-${name}`, true])) } }); wrappers.push(wrapper); await flushPromises(); return wrapper.vm
}
it('rendered QR reservation form edits applicant and query identity through actual controls',async()=>{
  const vm=await setup(true),wrapper=wrappers.at(-1)
  expect(wrapper.text()).toContain('A101');expect(wrapper.text()).toContain('机房')
  await wrapper.find('input[placeholder="姓名、班级或团队名称"]').setValue('李同学')
  await wrapper.find('input[placeholder="手机号或邮箱"]').setValue('13912345678')
  await wrapper.find('textarea[placeholder="填写课程、自主实验、考试、竞赛或活动说明"]').setValue('课程上机实验')
  expect(vm.formModel).toMatchObject({applicant:'李同学',contact:'13912345678',reason:'课程上机实验'})
  await wrapper.find('input[placeholder="预约编号"]').setValue('9');expect(vm.queryModel.reservationId).toBe('9')
})
it('rendered reservation completes form bindings and exposes submitted ticket controls',async()=>{const vm=await setup(true),w=wrappers.at(-1);for(const input of w.findAll('input')){if(input.attributes('type')==='checkbox')await input.setValue(true);else if(input.attributes('placeholder')==='例如：软件学院 2 班')await input.setValue('软件二班')}const select=w.get('select');await select.setValue('课程');vm.formModel.date='2026-10-19';vm.timeRangeModel=['08:30','10:00'];vm.formModel.reason='教学';vm.formModel.confirmed=true;await vm.submitForm();await flushPromises();expect(w.text()).toContain('预约已提交');expect(w.text()).toContain('请保存预约编号');expect(w.text()).toContain('9');await w.findAll('button').find(b=>b.text()==='复制编号').trigger('click');await flushPromises();expect(navigator.clipboard.writeText).toHaveBeenCalledWith('9');api.getScanReservationStatus.mockResolvedValue({id:9,status:'已驳回',note:'时段冲突'});await w.findAll('button').find(b=>b.text()==='刷新状态').trigger('click');await flushPromises();expect(w.text()).toContain('已驳回');await w.findAll('button').find(b=>b.text()==='继续预约').trigger('click');await flushPromises();expect(vm.submittedReservation).toBeNull();expect(w.find('select').exists()).toBe(true)})
it('profile loads from QR query, pre-fills cached user and exposes lab/date facts', async () => {
  const vm = await setup()
  expect(api.getScanReservationProfile).toHaveBeenCalledWith({ labId: '7', code: 'test-code' })
  expect(vm.formModel).toMatchObject({ applicant: '张同学', contact: '13800138000', department: '信息学院' })
  expect(vm.attendeeMax).toBe(40); expect(vm.labLocation).toBe('信息楼 · 一层 · 101'); expect(vm.labStatusType).toBe('success'); expect(vm.labFacts).toHaveLength(6)
  vm.formModel.date = '2026-10-19'; expect(vm.selectedDateMeta).toMatchObject({ academicWeek: 3, weekday: '星期一' })
  vm.timeRangeModel = ['08:30','10:00']; expect(vm.formModel.timeRange).toBe('08:30-10:00'); expect(vm.selectedSlot.label).toBe('第1节'); expect(vm.durationLabel).toBe('1 小时 30 分钟')
  expect(vm.availabilityAlert.type).toBe('success')
  vm.timeRangeModel = []; expect(vm.formModel.timeRange).toBe('')
})
it.each([
  ['08:00-09:00','预约开始时间不能早于当前时间'],['12:00-11:00','结束时间需晚于开始时间'],['23:00-23:30','预约时段需在开放时段 08:00-22:00 内'],['10:00-10:15','预约时长至少 30 分钟'],
])('time range %s produces validation feedback', async (timeRange,message) => {
  const vm = await setup(); vm.formModel.date = vm.today; vm.formModel.timeRange = timeRange
  expect(vm.timeRangeMessage).toBe(message); expect(vm.availabilityAlert.type).toBe('warning')
  const callback = vi.fn(); vm.validateTimeRange(null,timeRange,callback); expect(callback.mock.calls[0][0].message).toBe(message)
})
it('custom validators enforce scenes, integer headcount, confirmation and required time', async () => {
  const vm = await setup()
  for (const [method,bad,good] of [['validateScene','other','课程'],['validateAttendees',0,1],['validateConfirmed',false,true]]) {
    const callback = vi.fn(); vm[method](null,bad,callback); expect(callback.mock.calls[0][0]).toBeInstanceOf(Error)
    callback.mockClear(); vm[method](null,good,callback); expect(callback).toHaveBeenCalledWith()
  }
  const callback = vi.fn(); vm.validateAttendees(null,41,callback); expect(callback.mock.calls[0][0].message).toContain('40')
  vm.validateTimeRange(null,'',callback); expect(callback.mock.calls.at(-1)[0].message).toBe('请选择预约时段')
})
it('submit validates form, posts normalized fields, remembers query and polls until approval', async () => {
  const vm = await setup(); vm.formRef = { validate: vi.fn().mockResolvedValue(), clearValidate: vi.fn(), validateField: vi.fn() }
  Object.assign(vm.formModel,{ date:'2026-10-19',reason:'程序设计课程',confirmed:true }); vm.setQuickSlot(vm.quickSlots[0])
  await vm.submitForm()
  expect(api.submitScanReservation).toHaveBeenCalledWith(expect.objectContaining({ labId:7,labCode:'A101',contact:'13800138000',reason:'程序设计课程' }))
  expect(JSON.parse(localStorage.getItem('smart-lab:scan-reservation:last-status'))).toMatchObject({ labId:'7',reservationId:9,contact:'13800138000' })
  expect(vm.submittedSummary).toHaveLength(5); expect(vm.statusStepActive).toBe(1)
  api.getScanReservationStatus.mockResolvedValue({id:9,status:'已通过',tagType:'success'})
  await vi.advanceTimersByTimeAsync(15001)
  expect(vm.submittedStatus).toBe('已通过'); expect(vm.statusStepActive).toBe(3); expect(vm.statusAlert.type).toBe('success')
  const calls = api.getScanReservationStatus.mock.calls.length; await vi.advanceTimersByTimeAsync(30000); expect(api.getScanReservationStatus).toHaveBeenCalledTimes(calls)
  await vm.copyReservationId(); expect(navigator.clipboard.writeText).toHaveBeenCalledWith('9')
  vm.handleContinue(); expect(vm.submittedReservation).toBeNull(); expect(vm.formModel.reason).toBe('')
})
it('submit validation/API errors preserve form and reset busy state', async () => {
  const vm = await setup(); vm.formRef = {validate:vi.fn().mockRejectedValue('invalid')}; await vm.submitForm(); expect(api.submitScanReservation).not.toHaveBeenCalled()
  vm.formRef.validate.mockResolvedValue(); api.submitScanReservation.mockRejectedValueOnce(new Error('时段冲突')); await vm.submitForm()
  expect(vm.submitting).toBe(false); expect(ElMessage.error).toHaveBeenCalledWith('时段冲突')
})
it('status lookup validates identity, displays status and handles errors and copy failure', async () => {
  const vm = await setup(); await vm.handleStatusLookup(); expect(ElMessage.warning).toHaveBeenCalledWith('请填写数字预约编号')
  vm.queryModel.reservationId='#9'; await vm.handleStatusLookup(); expect(ElMessage.warning).toHaveBeenCalledWith('请填写提交时的联系方式')
  vm.queryModel.contact='bad'; await vm.handleStatusLookup(); expect(api.getScanReservationStatus).not.toHaveBeenCalled()
  vm.queryModel.contact='13800138000'; await vm.handleStatusLookup(); expect(api.getScanReservationStatus).toHaveBeenCalledWith({id:'9',contact:'13800138000'})
  api.getScanReservationStatus.mockRejectedValueOnce(new Error('查询失败')); await vm.refreshSubmittedStatus(); expect(vm.statusLoading).toBe(false); expect(ElMessage.error).toHaveBeenCalledWith('查询失败')
  navigator.clipboard.writeText.mockRejectedValueOnce(new Error('denied')); await vm.copyReservationId(); expect(ElMessage.warning).toHaveBeenCalledWith('请手动保存预约编号：9')
  vm.submittedReservation={id:9,status:'已驳回'}; expect(vm.statusAlert.type).toBe('error'); expect(vm.statusStepActive).toBe(2)
  vm.submittedReservation={id:9,status:'已取消'}; expect(vm.statusAlert.type).toBe('info')
})
it('restores only matching lab queries and safely handles QR/profile/calendar failures', async () => {
  localStorage.setItem('smart-lab:scan-reservation:last-status',JSON.stringify({labId:'7',reservationId:9,contact:'13800138000'}))
  const vm=await setup(); expect(vm.submittedReservation.id).toBe(9)
  route.query={}; await vm.loadLabProfile(); expect(vm.loadError).toBe('预约二维码缺少实验室信息')
  route.query={labId:'7'}; api.getScanReservationProfile.mockRejectedValueOnce(new Error('二维码无效')); await vm.loadLabProfile(); expect(vm.loadError).toBe('二维码无效'); expect(vm.loading).toBe(false)
})


it('handles incomplete profiles, malformed slots and all lab availability states', async () => {
  getStoredUser.mockReturnValue(null); isAuthenticated.mockReturnValue(false)
  api.getScanReservationProfile.mockResolvedValue({})
  const vm = await setup(true)
  expect(vm.attendeeMax).toBe(120); expect(vm.labLocation).toBe('位置待完善'); expect(vm.labStatusType).toBe('info')
  expect(vm.formModel.applicant).toBe(''); expect(vm.labFacts.map(f=>f.value)).toContain('管理员')
  for (const [status,type] of [['使用中','warning'],['预约中','warning'],['维护','danger'],['停用','danger'],['未知','info']]) { vm.lab={status}; expect(vm.labStatusType).toBe(type) }
  vm.lab={openTime:'invalid',type:'竞赛机房',manager:'王老师',code:'B2'}; expect(vm.openRangeLabel).toBe('08:00-22:00'); expect(vm.labFacts[2].value).toBe('竞赛机房')
  for(const range of ['', 'bad', '10:00-', '-11:00']){vm.formModel.timeRange=range;expect(vm.durationLabel).toBe('')}
  vm.formModel.date='invalid'; expect(vm.selectedDateMeta).toBeNull()
  vm.formModel.date='2026-10-19'
  for(const [range,label] of [['10:00-11:00','1 小时'],['10:00-10:30','30 分钟'],['10:00-11:15','1 小时 15 分钟']]){vm.formModel.timeRange=range;expect(vm.durationLabel).toBe(label)}
  const callback=vi.fn(); vm.validateTimeRange(null,'10:00-11:15',callback);expect(callback).toHaveBeenCalledWith()
  expect(loadTeachingCalendar).not.toHaveBeenCalled()
})

it.each([
  [{name:'备用姓名',roleName:'学生',email:'a@example.com'},'备用姓名','学生','a@example.com'],
  [{username:'student'},'student','',''],
])('uses available account identity fields without requiring a phone', async(user,name,department,contact)=>{
  getStoredUser.mockReturnValue(user);const vm=await setup();expect(vm.formModel).toMatchObject({applicant:name,department,contact})
  vm.formModel.applicant='已填写';vm.formModel.department='班级';vm.formModel.contact='manual@example.com';await vm.loadLabProfile();expect(vm.formModel.applicant).toBe('已填写')
})

it('tolerates corrupt status cache and missing calendar, reports only unhandled lookup errors', async()=>{
  localStorage.setItem('smart-lab:scan-reservation:last-status','{')
  loadTeachingCalendar.mockRejectedValueOnce({})
  const vm=await setup();expect(vm.submittedReservation).toBeNull();expect(ElMessage.warning).toHaveBeenCalledWith('教学日历加载失败')
  await vm.copyReservationId();expect(ElMessage.warning).toHaveBeenCalledWith('暂无预约编号')
  vm.queryModel.reservationId='9';vm.queryModel.contact='a@example.com'
  api.getScanReservationStatus.mockRejectedValueOnce({});await vm.handleStatusLookup();expect(ElMessage.error).toHaveBeenCalledWith('预约状态查询失败')
  ElMessage.error.mockClear();api.getScanReservationStatus.mockRejectedValueOnce({__handled:true});await vm.handleStatusLookup();expect(ElMessage.error).not.toHaveBeenCalled()
  vm.submittedReservation={id:9,contact:'a@example.com',status:'冲突'};vm.statusContact='';vm.formModel.contact=''
  api.getScanReservationStatus.mockRejectedValueOnce({});await vm.refreshSubmittedStatus();expect(ElMessage.error).toHaveBeenCalledWith('审核状态查询失败')
  ElMessage.error.mockClear();api.getScanReservationStatus.mockRejectedValueOnce(new Error('silent'));await vm.refreshSubmittedStatus({silent:true});expect(ElMessage.error).not.toHaveBeenCalled()
  vm.submittedReservation=null;await vm.refreshSubmittedStatus();expect(vm.statusLoading).toBe(false)
})

it('restores only complete matching tickets and supports legacy clipboard copy',async()=>{
  localStorage.setItem('smart-lab:scan-reservation:last-status',JSON.stringify({labId:'other',reservationId:9,contact:'a@example.com'}))
  const vm=await setup();expect(vm.submittedReservation).toBeNull()
  vm.submittedReservation={id:9};vm.formModel.applicant='申请人';expect(vm.submittedSummary[1].value).toBe('申请人');expect(vm.submittedStatus).toBe('待审核');expect(vm.submittedStatusType).toBe('success')
  const saved=navigator.clipboard.writeText;navigator.clipboard.writeText=undefined;document.execCommand=vi.fn().mockReturnValue(true)
  try{await vm.copyReservationId();expect(document.execCommand).toHaveBeenCalledWith('copy');expect(document.querySelector('textarea')).toBeNull()}finally{navigator.clipboard.writeText=saved}
  for(const status of ['已驳回','已取消']){vm.submittedReservation={id:9,status};expect(vm.statusAlert.description).toBeTruthy()}
  vm.formRef={validate:vi.fn().mockResolvedValue()};vm.formModel.note='特别说明';vm.formModel.attendees='';vm.lab={id:7,displayName:'完整机房名称'}
  api.submitScanReservation.mockRejectedValueOnce({});await vm.submitForm();expect(ElMessage.error).toHaveBeenCalledWith('预约提交失败')
  expect(api.submitScanReservation).toHaveBeenLastCalledWith(expect.objectContaining({lab:'完整机房名称',attendees:1,note:'特别说明'}))
  ElMessage.error.mockClear();api.submitScanReservation.mockRejectedValueOnce({__handled:true});await vm.submitForm();expect(ElMessage.error).not.toHaveBeenCalled()
  vm.lab=null;await vm.submitForm();expect(vm.submitting).toBe(false)
})
