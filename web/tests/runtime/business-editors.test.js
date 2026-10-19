// @vitest-environment happy-dom
import { afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Reservation from '../../src/views/reservations/components/ReservationEditorDialog.vue'
import Adjustment from '../../src/views/teacher/components/TeacherAdjustmentDialog.vue'
import Inventory from '../../src/views/devices/components/DeviceInventoryDialog.vue'
import { ElMessage } from 'element-plus'
vi.mock('element-plus',()=>({ElMessage:{warning:vi.fn()}}))
const wrappers=[]
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.clearAllMocks()})
function mount(component,props){const wrapper=shallowMount(component,{props,global:{stubs:Object.fromEntries(['dialog','form','form-item','row','col','input','input-number','select','option','date-picker','time-picker','button','alert','tag','icon','checkbox','radio-group','radio-button','descriptions','descriptions-item'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);return {vm:wrapper.vm,wrapper}}
const form=()=>({applicant:'教师甲',lab:'A101',date:'2027-01-04',timeRange:'08:30-10:00',attendees:20})
function validate(fn,value){const done=vi.fn();fn({},value,done);return done.mock.calls[0][0]}
it('reservation time picker writes the submitted range and validates ordering and conflicts',async()=>{
  const model=form();const {vm,wrapper}=mount(Reservation,{title:'预约',formModel:model});expect(vm.timeRangeModel).toEqual(['08:30','10:00']);vm.timeRangeModel=['10:00','11:00'];expect(model.timeRange).toBe('10:00-11:00');vm.timeRangeModel=[];expect(model.timeRange).toBe('');expect(validate(vm.validateTimeRange,'')).toBeInstanceOf(Error);expect(validate(vm.validateTimeRange,'11:00-10:00')).toBeInstanceOf(Error);expect(validate(vm.validateTimeRange,'08:30-10:00')).toBeUndefined();await wrapper.setProps({availability:{timetableConflicts:[{}]}});expect(validate(vm.validateTimeRange,'08:30-10:00').message).toContain('课表');await wrapper.setProps({availability:{reservationConflicts:[{}]}});expect(validate(vm.validateTimeRange,'08:30-10:00').message).toContain('人工预约')
})
it('reservation attendance uses server capacity and rejects fractional or nonpositive counts',async()=>{
  const {vm,wrapper}=mount(Reservation,{title:'预约',formModel:form(),availability:{capacity:30}});expect(vm.attendeeMax).toBe(30);expect(validate(vm.validateAttendees,30)).toBeUndefined();for(const number of [0,-1,1.5,31])expect(validate(vm.validateAttendees,number)).toBeInstanceOf(Error);await wrapper.setProps({availability:{capacity:0}});expect(vm.attendeeMax).toBe(120)
})
it('reservation availability distinguishes unsynced data from authoritative busy/free states',async()=>{
  const model=form();const {vm,wrapper}=mount(Reservation,{title:'预约',formModel:model,availability:{isReady:false,isFree:true}});expect(vm.availabilityAlert).toMatchObject({type:'warning',title:'课表数据暂未完成同步'});await wrapper.setProps({availability:{isReady:false,isFree:false}});expect(vm.availabilityAlert.type).toBe('error');await wrapper.setProps({availability:{isReady:true,isFree:true,capacity:30}});expect(vm.availabilityAlert.title).toContain('容量 30 人');await wrapper.setProps({availability:{isReady:true,isFree:false}});expect(vm.availabilityAlert.title).toBe('当前时段不可预约');await wrapper.setProps({formModel:{...model,lab:''}});expect(vm.availabilityAlert).toBeNull()
})
it('reservation submit honors form validation and closed clears errors',async()=>{
  const {vm,wrapper}=mount(Reservation,{title:'预约',formModel:form()});await vm.handleSubmit();expect(wrapper.emitted('submit')).toHaveLength(1);vm.formRef={validate:vi.fn().mockRejectedValue('invalid'),clearValidate:vi.fn()};await vm.handleSubmit();expect(wrapper.emitted('submit')).toHaveLength(1);vm.formRef.validate.mockResolvedValue(true);await vm.handleSubmit();expect(wrapper.emitted('submit')).toHaveLength(2);vm.handleClosed();expect(vm.formRef.clearValidate).toHaveBeenCalled()
})
it('adjustment resets from prefill and maps structured period selectors to submitted text',async()=>{
  const {vm,wrapper}=mount(Adjustment,{modelValue:true,prefill:{course:'数据库',originalTime:'5月 第10周 周一 第1节 08:30-10:00',reason:'会议冲突'}});expect(vm.formModel.course).toBe('数据库');expect(vm.timeModel.original).toEqual({week:'5月 第10周',day:'周一',period:'第1节 08:30-10:00'});Object.assign(vm.timeModel.target,{week:'下一周',day:'周二',period:'第2节 10:20-11:50'});await flushPromises();expect(vm.formModel.targetTime).toBe('下一周 周二 第2节 10:20-11:50');vm.handleSave();expect(wrapper.emitted('save')[0][0]).toMatchObject({course:'数据库',reason:'会议冲突'});expect(wrapper.emitted('update:modelValue')).toEqual([[false]]);await wrapper.setProps({prefill:{course:'新课程'}});expect(vm.formModel.reason).toBe('');expect(vm.buildTimeText({week:'下一周'})).toBe('')
})
it('adjustment rejects each missing required field before emitting save',()=>{
  const {vm,wrapper}=mount(Adjustment,{modelValue:true});for(const [field,value] of [['course','数据库'],['originalTime','周一'],['targetTime','周二'],['reason','教学冲突']]){vm.handleSave();expect(wrapper.emitted('save')).toBeUndefined();vm.formModel[field]=value}vm.handleSave();expect(wrapper.emitted('save')).toHaveLength(1);expect(ElMessage.warning).toHaveBeenCalledTimes(4)
})
it('inventory single/batch summaries and device alias formatting are consistent',async()=>{
  const {vm,wrapper}=mount(Inventory,{modelValue:true,formModel:{},device:{id:1,name:'主机',code:'PC1',lab:'A101'}});expect(vm.isBatch).toBe(false);expect(vm.title).toBe('设备盘点登记');expect(vm.summaryDevice.id).toBe(1);expect(vm.getDeviceName(vm.summaryDevice)).toBe('主机');expect(vm.getDeviceCode(vm.summaryDevice)).toBe('PC1');expect(vm.getDeviceLab(vm.summaryDevice)).toBe('A101');await wrapper.setProps({devices:[{id:1},{id:2}]});expect(vm.isBatch).toBe(true);expect(vm.title).toBe('批量盘点 2 台设备');vm.visible=false;expect(wrapper.emitted('update:modelValue')).toEqual([[false]]);expect(['正常','维修中','已报废','其他'].map(vm.getStatusType)).toEqual(['success','warning','info','info']);expect(vm.getDeviceName({})).toBe('未命名设备')
})
it('inventory condition toggles remain mutually exclusive and match result status',()=>{
  const model={normal:false,missing:true,damaged:true,resultStatus:'设备缺失'};const {vm}=mount(Inventory,{formModel:model});vm.handleNormalChange(true);expect(model).toMatchObject({missing:false,damaged:false,resultStatus:'已盘点'});vm.handleNormalChange(false);expect(model.resultStatus).toBe('盘点异常');vm.handleMissingChange(true);expect(model).toMatchObject({normal:false,damaged:false,resultStatus:'设备缺失'});vm.handleDamagedChange(true);expect(model).toMatchObject({normal:false,missing:false,resultStatus:'设备损坏'});for(const result of ['已盘点','设备缺失','设备损坏']){vm.handleResultChange(result);expect([model.normal,model.missing,model.damaged].filter(Boolean)).toHaveLength(1)}vm.handleMissingChange(false);vm.handleDamagedChange(false)
})
