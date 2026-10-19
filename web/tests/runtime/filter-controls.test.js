// @vitest-environment happy-dom
import { afterEach,expect,it } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { formStubs } from '../support/formStubs'
import Lab from '../../src/views/labs/components/LabFilterBar.vue'
import Usage from '../../src/views/records/components/UsageRecordFilterBar.vue'
import Device from '../../src/views/devices/components/DeviceFilterBar.vue'
import Reservation from '../../src/views/reservations/components/ReservationFilterBar.vue'
const wrappers=[]
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
const cases=[
  ['laboratory',Lab,{buildingOptions:['教学楼'],statusOptions:[{label:'开放'}],typeOptions:['开发']},['buildingFilter','statusFilter','typeFilter'],['教学楼','开放','开发'],'availableOnly'],
  ['usage',Usage,{statusOptions:[{label:'异常'}],typeOptions:['课程'],labOptions:['A101']},['statusFilter','typeFilter','labFilter'],['异常','课程','A101'],'abnormalOnly'],
  ['device',Device,{categoryOptions:['计算终端'],statusOptions:['正常'],labOptions:[{id:7,labCode:'A101',labName:'机房'}]},['labFilter','categoryFilter','statusFilter'],['7','计算终端','正常'],null],
  ['reservation',Reservation,{statusOptions:[{label:'待审核'}],sceneOptions:['教学'],labOptions:['A101']},['statusFilter','sceneFilter','labFilter'],['待审核','教学','A101'],'conflictOnly'],
]
it.each(cases)('%s filter controls emit their named parent models without mutating props',async(_name,component,options,keys,values,toggle)=>{
  const wrapper=shallowMount(component,{props:{keyword:'原搜索',resultCount:2,...options},global:{stubs:formStubs()}});wrappers.push(wrapper)
  const text=wrapper.find('input:not([type="checkbox"])');expect(text.element.value).toBe('原搜索');await text.setValue('新搜索');expect(wrapper.emitted('update:keyword')).toEqual([['新搜索']]);expect(wrapper.props('keyword')).toBe('原搜索')
  const selects=wrapper.findAll('select');expect(selects).toHaveLength(3)
  for(let index=0;index<3;index++){await selects[index].setValue(values[index]);expect(wrapper.emitted(`update:${keys[index]}`)).toEqual([[values[index]]])}
  if(toggle){await wrapper.find('input[type="checkbox"]').setValue(true);expect(wrapper.emitted(`update:${toggle}`)).toEqual([[true]])}
  for(const button of wrapper.findAll('button')){await button.trigger('click')}
  if(_name==='device'){expect(wrapper.emitted('search')).toBeTruthy();expect(wrapper.emitted('reset')).toBeTruthy()}
  if(_name==='usage'){expect(wrapper.emitted('reset')).toBeTruthy();wrapper.vm.dateRangeModel=null;expect(wrapper.emitted('update:dateRange')).toEqual([[[]]])}
})
