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

