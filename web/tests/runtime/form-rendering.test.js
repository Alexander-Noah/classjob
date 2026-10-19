// @vitest-environment happy-dom
import { afterEach,expect,it,vi } from 'vitest'
import { reactive } from 'vue'
import { shallowMount,flushPromises } from '@vue/test-utils'
import { formStubs } from '../support/formStubs'
import BusinessDialog from '../../src/components/business-management/BusinessRecordDialog.vue'
import Reservation from '../../src/views/reservations/components/ReservationEditorDialog.vue'
import Adjustment from '../../src/views/teacher/components/TeacherAdjustmentDialog.vue'
import Inventory from '../../src/views/devices/components/DeviceInventoryDialog.vue'
import User from '../../src/views/users/components/PermissionUserDialog.vue'
import Lab from '../../src/views/labs/components/LabEditorDialog.vue'
import Transfer from '../../src/views/devices/components/DeviceTransferDialog.vue'
import EnvironmentRequest from '../../src/views/teacher/components/TeacherEnvironmentRequestDialog.vue'
import DeviceEditor from '../../src/views/devices/components/DeviceEditorDialog.vue'
import ReservationDetail from '../../src/views/reservations/components/ReservationDetailPanel.vue'
const wrappers=[]
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function mount(component,props){const wrapper=shallowMount(component,{props,global:{stubs:formStubs()}});wrappers.push(wrapper);await flushPromises();return wrapper}
async function click(wrapper,text){const button=wrapper.findAll('button').find(button=>button.text()===text);expect(button,`button ${text}`).toBeTruthy();await button.trigger('click');await flushPromises()}
function field(wrapper,label){const found=wrapper.findAll('[data-element="form-item"]').find(item=>item.find('label').text()===label);expect(found,`field ${label}`).toBeTruthy();return found}
it('business dialog renders inferred field types and saves the values edited through controls',async()=>{
  const model=reactive({name:'旧名称',course:'程序设计',count:1,status:'启用',remark:'说明',date:'2027-01-04',timestamp:'2027-01-04 10:00'})
  const wrapper=await mount(BusinessDialog,{modelValue:true,dialogTitle:'编辑',formModel:model,statusOptions:['启用','停用'],editableColumns:[{prop:'name',label:'名称'},{prop:'course',label:'课程',options:['程序设计','数据库']},{prop:'count',label:'数量'},{prop:'status',label:'状态'},{prop:'remark',label:'备注'},{prop:'date',label:'日期'},{prop:'timestamp',label:'时间'}]})
  expect(wrapper.text()).toContain('名称');expect(wrapper.text()).toContain('课程');expect(wrapper.findAll('select')).toHaveLength(2);await wrapper.find('input[placeholder="请输入名称"]').setValue('新名称');await wrapper.find('textarea').setValue('新说明');await wrapper.findAll('select')[0].setValue('数据库');expect(model).toMatchObject({name:'新名称',course:'数据库',remark:'新说明'});await click(wrapper,'保存');expect(wrapper.emitted('submit')).toHaveLength(1);await click(wrapper,'取消');expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
})
it('reservation fields expose server availability and saving emits from the rendered footer',async()=>{
  const model=reactive({applicant:'教师甲',department:'学院',contact:'13812345678',lab:'A101',date:'2027-01-04',timeRange:'08:30-10:00',attendees:20,scene:'教学',reason:'实验课程',note:''})
  const wrapper=await mount(Reservation,{modelValue:true,title:'新预约',formModel:model,labOptions:['A101'],labMetaMap:{A101:{code:'A101',name:'机房'}},sceneOptions:['教学'],availability:{capacity:30,isReady:true,isFree:true,messages:['已检查课表'],reservationConflicts:[],timetableConflicts:[]}})
  expect(wrapper.text()).toContain('容量 30 人');expect(wrapper.text()).toContain('已检查课表');expect(wrapper.findAll('select')).toHaveLength(2);const textareas=wrapper.findAll('textarea');await textareas[0].setValue('课程上机实践');expect(model.reason).toBe('课程上机实践');await click(wrapper,'保存预约');expect(wrapper.emitted('submit')).toHaveLength(1);await click(wrapper,'取消');expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
})
it('teacher adjustment renders structured time options and submits selected target period',async()=>{
  const wrapper=await mount(Adjustment,{modelValue:true,courseOptions:['数据库'],prefill:{course:'数据库',originalTime:'周一上午',reason:'教学安排冲突'}})
  expect(wrapper.text()).toContain('数据库');expect(wrapper.findAll('select').length).toBeGreaterThan(3)
  const vm=wrapper.vm;Object.assign(vm.timeModel.target,{week:'下一周',day:'周二',period:'第1节 08:30-10:00'});await flushPromises();await click(wrapper,'提交申请');expect(wrapper.emitted('save')[0][0]).toMatchObject({course:'数据库',targetTime:'下一周 周二 第1节 08:30-10:00'})
})
it('inventory checkboxes update exclusive conditions and submit from its rendered footer',async()=>{
  const model=reactive({inspectorName:'管理员',inspectedAt:'2026-10-18 10:00',normal:true,missing:false,damaged:false,resultStatus:'已盘点',remark:''})
  const wrapper=await mount(Inventory,{modelValue:true,formModel:model,devices:[{assetNo:'PC1',deviceName:'电脑',labName:'A101',status:'正常'},{assetNo:'PC2',deviceName:'电脑二'}],resultOptions:[{label:'已盘点'},{label:'设备缺失'},{label:'设备损坏'}]});expect(wrapper.text()).toContain('PC1、PC2');await wrapper.findAll('input[type="checkbox"]')[1].setValue(true);expect(model).toMatchObject({normal:false,missing:true,damaged:false,resultStatus:'设备缺失'});await click(wrapper,'保存盘点');expect(wrapper.emitted('submit')).toHaveLength(1)
})
it('user dialog displays assignable roles, editable contact inputs and sends the saved form',async()=>{
  const wrapper=await mount(User,{modelValue:true,departmentOptions:['计算机'],roleOptions:['实验室管理员']});expect(wrapper.text()).toContain('实验室管理员');expect(wrapper.text()).toContain('计算机');expect(wrapper.findAll('input').length).toBeGreaterThanOrEqual(5);wrapper.vm.formModel.name='新管理员';wrapper.vm.formModel.account='newadmin';await click(wrapper,'保存用户');expect(wrapper.emitted('save')[0][0]).toMatchObject({name:'新管理员',account:'newadmin',role:'实验室管理员'})
})
it('lab editor binds each resource field to its corresponding submission property',async()=>{
  const model=reactive({});const wrapper=await mount(Lab,{modelValue:true,title:'新增实验室',formModel:model,managerOptions:[{id:7,realName:'管理员',username:'admin'}],typeOptions:['开发实验室'],statusOptions:[{label:'开放'}],safetyOptions:['正常']})
  for(const [label,value,key] of [['实验室编号','A101','code'],['实验室名称','开发机房','name'],['所在楼宇','教学楼','building'],['楼层','1F','floor'],['房间','101','room'],['总容量','30','capacity'],['开放时段','08:00-22:00','openTime'],['实验室标签','开发,教学','featuresText']]){await field(wrapper,label).find('input').setValue(value);expect(model[key]).toBe(key==='capacity'?Number(value):value)}
  for(const [label,value,key] of [['负责人','7','managerUserId'],['实验室类型','开发实验室','type'],['开放状态','开放','status'],['安全状态','正常','safety']]){await field(wrapper,label).find('select').setValue(value);expect(String(model[key])).toBe(value)}
  await click(wrapper,'保存');expect(wrapper.emitted('submit')).toHaveLength(1);await click(wrapper,'取消');expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
})
it('device transfer renders destination identities and edits every transfer detail',async()=>{
  const model=reactive({});const wrapper=await mount(Transfer,{modelValue:true,formModel:model,devices:[{assetNo:'PC1',deviceName:'电脑',labName:'A101',ownerName:'原负责人'}],labOptions:[{id:2,labCode:'B202',labName:'目标机房'}],ownerOptions:[{id:7,realName:'新负责人',username:'manager'}]})
  expect(wrapper.text()).toContain('PC1');expect(wrapper.text()).toContain('新负责人');const selects=wrapper.findAll('select');await selects[0].setValue('2');await selects[1].setValue('7');expect(model).toMatchObject({toLabId:'2',toOwnerUserId:'7'});
  for(const [label,value,key] of [['安装位置','B202-1','toLocation'],['调拨时间','2026-10-18 10:00','transferAt'],['经办人','管理员','operatorName']]){await field(wrapper,label).find('input').setValue(value);expect(model[key]).toBe(value)}
  await wrapper.find('textarea').setValue('教学设备调整');expect(model.reason).toBe('教学设备调整');await click(wrapper,'保存调拨');expect(wrapper.emitted('submit')).toHaveLength(1);await wrapper.setProps({devices:[{assetNo:'PC1'},{assetNo:'PC2'}]});expect(wrapper.text()).toContain('批量调拨 2 台设备')
})
it('environment request validates required fields and submits the rendered input values',async()=>{
  const wrapper=await mount(EnvironmentRequest,{modelValue:true,prefill:{labType:'特别机房'}});expect(wrapper.find('select').text()).toContain('特别机房');await click(wrapper,'提交需求');expect(wrapper.emitted('save')).toBeUndefined();
  for(const [label,value] of [['课程名称','程序设计'],['授课班级','软件一班'],['使用时间','周一上午']])await field(wrapper,label).find('input').setValue(value)
  const texts=wrapper.findAll('textarea');await texts[0].setValue('IDEA JDK21');await field(wrapper,'系统/镜像要求').find('input').setValue('Windows 11');await field(wrapper,'特殊说明').find('input').setValue('需要数据库');await click(wrapper,'提交需求');expect(wrapper.emitted('save')[0][0]).toMatchObject({courseName:'程序设计',className:'软件一班',useTime:'周一上午',labType:'特别机房',softwareRequirements:'IDEA JDK21',systemRequirements:'Windows 11',specialRequirements:'需要数据库'});expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
})
it('device editor binds laboratory metadata and persists each asset field through its controls',async()=>{
  const model=reactive({});const wrapper=await mount(DeviceEditor,{modelValue:true,formModel:model,labOptions:[{id:7,labCode:'A101',labName:'机房',roomNo:'101'}],categoryOptions:['计算终端'],statusOptions:['正常']})
  for(const [label,value,key] of [['资产编号','PC1','assetNo'],['设备名称','教学电脑','deviceName'],['安装位置','101-1','roomNo'],['数量','2','quantity'],['单位','台','unit']]){await field(wrapper,label).find('input').setValue(value);expect(model[key]).toBe(key==='quantity'?2:value)}
  await field(wrapper,'设备分类').find('select').setValue('计算终端');await field(wrapper,'所属实验室').find('select').setValue('7');expect(model).toMatchObject({labName:'机房',roomNo:'101'});await field(wrapper,'状态').find('select').setValue('正常')
  for(const [label,value,key] of [['规格参数','16GB','specification'],['执行标准','教学用机','standardRequirement'],['备注','新购','remark']]){await field(wrapper,label).find('textarea').setValue(value);expect(model[key]).toBe(value)}
  const save=wrapper.findAll('button').find(button=>button.text().includes('保存'));expect(save).toBeTruthy();await save.trigger('click');expect(wrapper.emitted('submit')).toHaveLength(1);wrapper.vm.handleLabChange('missing');expect(model.labId).toBe('');expect(model.location).toBe('')
})
it('reservation detail displays approval context and enforces lifecycle button availability',async()=>{
  const reservation={id:9,applicant:'教师甲',lab:'A101 机房',date:'2027-01-04',timeRange:'08:30-10:00',status:'待审核',attendees:20,reason:'课程实验',contact:'13812345678',conflict:false,usageCount:2}
  const wrapper=await mount(ReservationDetail,{reservation});expect(wrapper.text()).toContain('A101 机房');expect(wrapper.text()).toContain('课程实验');const approve=wrapper.findAll('button').find(button=>button.text()==='通过');expect(approve.attributes('disabled')).toBeUndefined();await approve.trigger('click');expect(wrapper.emitted('approve')).toHaveLength(1);await wrapper.setProps({reservation:{...reservation,status:'已取消'}});expect(wrapper.findAll('button').find(button=>button.text()==='通过').attributes('disabled')).toBeDefined();await wrapper.setProps({reservation:null});expect(wrapper.findAll('button')).toHaveLength(0)
})
