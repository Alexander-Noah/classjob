// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/labs/LabAdminClassTimetable.vue'
import * as api from '../../src/api/services/classTimetable'
import { ElMessage,ElMessageBox } from 'element-plus'
import { panelStubs as tableStubs } from '../support/formStubs'
vi.mock('../../src/api/services/classTimetable',()=>Object.fromEntries(['clearClassTimetableCrawlerTasks','deleteClassTimetableCrawlerCredential','getClassTimetableCrawlerCredential','getClassTimetableCrawlerConfig','getClassTimetableSummary','listClassTimetableCrawlerTasks','saveClassTimetableCrawlerConfig','saveClassTimetableCrawlerCredential','triggerClassTimetableCrawler'].map(name=>[name,vi.fn()])))
vi.mock('element-plus',()=>({ElMessage:{warning:vi.fn(),error:vi.fn(),success:vi.fn()},ElMessageBox:{confirm:vi.fn()}}))
const wrappers=[]
it('rendered crawler settings bind schedule modes and account filters',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('保存配置');await w.get('input[type="checkbox"]').setValue(false);expect(vm.configForm.scheduleEnabled).toBe(false);await w.get('input[type="checkbox"]').setValue(true);await w.get('input[placeholder="按账号或备注搜索"]').setValue('te');expect(vm.accountFilter.keyword).toBe('te');vm.configForm.frequency='daily';await flushPromises();const picker=w.findComponent({name:'Testtime-picker'});picker.vm.$emit('update:modelValue','08:30');expect(vm.configForm.dailyTime).toBe('08:30');vm.configForm.frequency='weekly';await flushPromises();w.findComponent({name:'Testtime-picker'}).vm.$emit('update:modelValue','09:30');const selects=w.findAll('select');for(const select of selects){const option=select.findAll('option')[0];if(option)await select.setValue(option.element.value)}expect(vm.configForm.weeklyTime).toBe('09:30');const click=async label=>{await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()};await click('保存配置');expect(api.saveClassTimetableCrawlerConfig).toHaveBeenCalled();await click('详情');await click('关闭');expect(vm.taskDetailVisible).toBe(false);await click('清空记录');expect(api.clearClassTimetableCrawlerTasks).toHaveBeenCalled()})
const credential={configured:true,usable:true,usernameMasked:'te***er',updatedAt:'2026-10-18'}
beforeEach(()=>{vi.resetAllMocks();localStorage.clear();api.getClassTimetableCrawlerCredential.mockResolvedValue({...credential});api.getClassTimetableCrawlerConfig.mockResolvedValue({cron:'0 0 */6 * * *',semester:'2026秋',semesterStartDate:'2026-09-01',nextRunAt:'2026-10-19 02:00'});api.getClassTimetableSummary.mockResolvedValue({latestSyncAt:'2026-10-18 02:00'});api.listClassTimetableCrawlerTasks.mockResolvedValue([{id:1,status:'success'},{id:2,status:'failed',error:'登录失败'}]);api.saveClassTimetableCrawlerConfig.mockImplementation(async config=>config);api.saveClassTimetableCrawlerCredential.mockResolvedValue({...credential});ElMessageBox.confirm.mockResolvedValue(true)})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{stubs:render?tableStubs():Object.fromEntries(['button','icon','card','switch','select','option','input','input-number','time-picker','date-picker','table','table-column','tag','pagination','dialog','form','form-item','radio-group','radio-button','descriptions','descriptions-item','empty','alert'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('rendered crawler account edit saves entered credentials and task details target their row',async()=>{
  const vm=await setup(true),wrapper=wrappers.at(-1);expect(wrapper.text()).toContain('te***er')
  await wrapper.findAll('button').find(button=>button.text()==='编辑').trigger('click');await flushPromises()
  await wrapper.find('input[placeholder="请输入新账号；原账号只显示脱敏值"]').setValue('teacher-new')
  await wrapper.find('input[placeholder="保存时会加密写入数据库，不会明文存储"]').setValue('test-password')
  await wrapper.findAll('button').find(button=>button.text()==='保存').trigger('click');await flushPromises()
  expect(api.saveClassTimetableCrawlerCredential).toHaveBeenCalledWith({username:'teacher-new',password:'test-password'})
  await wrapper.findAll('button').find(button=>button.text()==='详情').trigger('click');await flushPromises();expect(vm.activeTask.id).toBe(1)
})
it('loads account summaries, configuration and paged task records',async()=>{
  const vm=await setup();expect(vm.accountRows[0]).toMatchObject({username:'te***er',status:'启用',latestSuccessAt:'2026-10-18 02:00',latestError:'登录失败'});expect(vm.pagedAccountRows).toHaveLength(1);expect(vm.pagedTaskRecords).toHaveLength(2);expect(vm.latestTask.id).toBe(1);expect(vm.scheduleSummary).toBe('每 6 小时自动采集');expect(vm.nextRunText).toBe('2026-10-19 02:00')
  vm.accountFilter.keyword='missing';expect(vm.accountRows).toEqual([]);vm.resetAccountFilter();vm.accountFilter.status='待更新';expect(vm.accountRows).toEqual([]);vm.credential.usable=false;expect(vm.accountRows[0].status).toBe('待更新');expect(vm.nextRunText).toContain('账号可用')
  vm.accountPagination.page=3;vm.queryAccountRows();expect(vm.accountPagination.page).toBe(1)
})
it.each([['0 0 */4 * * *','interval',4],['0 0 */3 * * *','interval',6],['0 30 8 * * *','daily',6],['0 15 9 * * FRI','weekly',6],['bad','interval',6],['0 0 0 1 1 *','interval',6]])('parses supported cron %s',async(cron,frequency,interval)=>{const vm=await setup();vm.parseCronToForm(cron);expect(vm.configForm.frequency).toBe(frequency);expect(vm.configForm.intervalHours).toBe(interval)})
it('builds daily and weekly cron and formats disabled schedule state',async()=>{
  const vm=await setup();vm.configForm.frequency='daily';vm.configForm.dailyTime='8:30';expect(vm.buildCronFromConfig()).toBe('0 30 8 * * *');expect(vm.scheduleSummary).toContain('每天')
  Object.assign(vm.configForm,{frequency:'weekly',weeklyDay:'FRI',weeklyTime:'09:15'});expect(vm.buildCronFromConfig()).toBe('0 15 9 * * FRI');expect(vm.scheduleSummary).toBe('每周五 09:15 自动采集');expect(vm.parseTime('99:99')).toBe('23:59');expect(vm.parseTime('bad')).toBe('02:00')
  vm.configForm.scheduleEnabled=false;expect(vm.scheduleSummary).toBe('已关闭自动采集');expect(vm.nextRunText).toBe('自动采集已关闭');vm.applyCrawlerConfig({scheduleEnabled:'1'});expect(vm.configForm.scheduleEnabled).toBe(true)
})
it('validates and persists schedule configuration while releasing busy state after failure',async()=>{
  const vm=await setup();vm.configForm.semester='';await vm.saveConfig();expect(api.saveClassTimetableCrawlerConfig).not.toHaveBeenCalled();vm.configForm.semester='2026秋';vm.configForm.semesterStartDate='';await vm.saveConfig();expect(api.saveClassTimetableCrawlerConfig).not.toHaveBeenCalled();vm.configForm.semesterStartDate='2026-09-01';await vm.saveConfig();expect(api.saveClassTimetableCrawlerConfig).toHaveBeenCalledWith(expect.objectContaining({cron:'0 0 */6 * * *',semester:'2026秋'}));expect(vm.isSavingConfig).toBe(false)
  api.saveClassTimetableCrawlerConfig.mockRejectedValue(new Error('保存失败'));await vm.saveConfig();expect(ElMessage.error).toHaveBeenCalledWith('保存失败');expect(vm.isSavingConfig).toBe(false)
})
it('keeps password out of local storage and clears it only after successful credential saving',async()=>{
  const vm=await setup();vm.openAccountDialog();await vm.saveCredential();expect(api.saveClassTimetableCrawlerCredential).not.toHaveBeenCalled();Object.assign(vm.credentialForm,{username:'teacher',password:'secret',remark:'教学账号'});await vm.saveCredential();expect(api.saveClassTimetableCrawlerCredential).toHaveBeenCalledWith({username:'teacher',password:'secret'});expect(vm.credentialForm.password).toBe('');expect(vm.accountDialogVisible).toBe(false);expect(Object.values(localStorage)).not.toContain('secret');expect(localStorage.getItem('lab-admin-class-timetable:account-remark')).toBe('教学账号')
  vm.openAccountDialog();Object.assign(vm.credentialForm,{username:'teacher',password:'retry'});api.saveClassTimetableCrawlerCredential.mockRejectedValue(new Error('offline'));await vm.saveCredential();expect(vm.accountDialogVisible).toBe(true);expect(vm.credentialForm.password).toBe('retry');expect(vm.isSavingCredential).toBe(false)
})
it('credential deletion handles cancellation and successful removal',async()=>{
  const vm=await setup();ElMessageBox.confirm.mockRejectedValueOnce('cancel');await vm.deleteCredential();expect(api.deleteClassTimetableCrawlerCredential).not.toHaveBeenCalled();await vm.deleteCredential();expect(vm.credential.configured).toBe(false);expect(vm.accountRows).toEqual([])
  api.deleteClassTimetableCrawlerCredential.mockRejectedValue(new Error('offline'));await vm.deleteCredential();expect(ElMessage.error).toHaveBeenCalledWith('offline')
})
it('blocks unusable accounts, triggers collection and handles timed-out tasks with refreshed history',async()=>{
  const vm=await setup();vm.credential={configured:false};await vm.handleCrawler();expect(api.triggerClassTimetableCrawler).not.toHaveBeenCalled();vm.credential={...credential};api.triggerClassTimetableCrawler.mockResolvedValue({status:'success',message:'完成'});await vm.handleCrawler();expect(ElMessage.success).toHaveBeenCalledWith('完成');expect(vm.isCrawling).toBe(false)
  api.triggerClassTimetableCrawler.mockResolvedValue({status:'skipped'});await vm.handleCrawler();expect(ElMessage.warning).toHaveBeenCalledWith('采集任务已结束，请查看任务记录')
  api.triggerClassTimetableCrawler.mockRejectedValue({code:'ECONNABORTED'});await vm.handleCrawler();expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('等待超时'));expect(vm.isCrawling).toBe(false)
})
it('task detail and clearing honor cancellation and backend errors',async()=>{
  const vm=await setup();vm.openTaskDetail(vm.latestTask);expect(vm.activeTask.id).toBe(1);expect(vm.taskDetailVisible).toBe(true);ElMessageBox.confirm.mockRejectedValueOnce('close');await vm.clearTaskRecords();expect(api.clearClassTimetableCrawlerTasks).not.toHaveBeenCalled();api.clearClassTimetableCrawlerTasks.mockRejectedValueOnce(new Error('offline'));await vm.clearTaskRecords();expect(vm.taskRecords).toHaveLength(2);await vm.clearTaskRecords();expect(vm.taskRecords).toEqual([]);await vm.clearTaskRecords();expect(api.clearClassTimetableCrawlerTasks).toHaveBeenCalledTimes(2)
  expect(['success','running','skipped','failed'].map(vm.getTaskStatusTagType)).toEqual(['success','primary','warning','danger']);expect(['scheduled','manual','custom',''].map(vm.formatTriggerType)).toEqual(['定时','手动','custom','-'])
})
it('credential load failures disable collection while preserving configuration facts',async()=>{
  api.getClassTimetableCrawlerCredential.mockRejectedValue(new Error('offline'));const vm=await setup();expect(vm.credential.status).toBe('load_failed');expect(vm.credentialState.canCollect).toBe(false);expect(vm.configForm.semester).toBe('2026秋')
})

