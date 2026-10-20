// @vitest-environment happy-dom
import {afterEach,expect,it} from 'vitest'
import {shallowMount} from '@vue/test-utils'
import {defineComponent,h} from 'vue'
import {panelStubs} from '../support/formStubs'
import PageHero from '../../src/components/common/PageHero.vue'
import MetricGrid from '../../src/components/common/MetricGrid.vue'
import LabHeader from '../../src/views/labs/components/LabManagementHeader.vue'
import DevicePanel from '../../src/views/room-control/components/DeviceControlPanel.vue'
import LabEditor from '../../src/views/labs/components/LabEditorDialog.vue'
const wrappers=[]
const mount=(page,props={})=>{const w=shallowMount(page,{props,global:{stubs:panelStubs()}});wrappers.push(w);return w}
afterEach(()=>wrappers.splice(0).forEach(w=>w.unmount()))
it('page actions emit their complete configuration and back remains available without actions',async()=>{
  const icon=defineComponent({render:()=>h('i','icon')})
  const action={label:'导出',type:'primary',command:'export'}
  const w=mount(PageHero,{pageIcon:icon,pageTitle:'设备管理',actions:[action]})
  expect(w.text()).toContain('业务管理');expect(w.get('h1').text()).toBe('设备管理')
  await w.findAll('button')[0].trigger('click');expect(w.emitted('action')).toEqual([[action]])
  await w.findAll('button')[1].trigger('click');expect(w.emitted('back')).toHaveLength(1)
  await w.setProps({actions:[],badge:'资产'});expect(w.findAll('button')).toHaveLength(1);expect(w.text()).toContain('资产')
})
it('metric columns stay bounded as asynchronous results change and values remain plain text',async()=>{
  const w=mount(MetricGrid);expect(w.findAll('article')).toHaveLength(0)
  const metrics=Array.from({length:5},(_,i)=>({label:`指标${i}`,value:i,trend:'<b>变化</b>'}))
  await w.setProps({metrics});expect(w.findAll('article')).toHaveLength(5)
  expect(w.attributes('style')).toContain('--metric-column-count: 4');expect(w.find('b').exists()).toBe(false)
  await w.setProps({metrics:metrics.slice(0,2)});expect(w.attributes('style')).toContain('--metric-column-count: 2')
})
it('lab header renders counts and exposes creation and export events',async()=>{
  const w=mount(LabHeader,{statusSummary:[{label:'开放',count:0},{label:'维护',count:2}]})
  expect(w.text()).toContain('开放 0');expect(w.text()).toContain('维护 2')
  await w.findAll('button')[0].trigger('click');await w.findAll('button')[1].trigger('click')
  expect(w.emitted('create')).toHaveLength(1);expect(w.emitted('export')).toHaveLength(1)
  await w.setProps({statusSummary:[]});expect(w.findAll('.status-strip span')).toHaveLength(0)
})
it('hardware capability changes carry the originating device and preserve distinct state keys',async()=>{
  const state={};const capabilities=[{key:'power',type:'switch',label:'开关',enabled:true}]
  const device={id:1,name:'灯光',type:'light',sourceType:'mock',online:true,capabilities}
  const w=mount(DevicePanel,{capabilityState:state,devices:[device],labName:'L1'})
  expect(w.text()).toContain('模拟网关演示');expect(w.text()).toContain('灯光')
  await w.get('input[type="checkbox"]').setValue(true)
  expect(state['1::power']).toEqual({enabled:true,value:''})
  expect(w.emitted('capability')[0]).toEqual([device,capabilities[0]])
  await w.setProps({devices:[],loading:true});expect(w.findAll('.device-control-card')).toHaveLength(0)
})
it('lab manager choices display legacy usernames and missing role labels without losing selection',async()=>{
  const stubs=panelStubs()
  stubs['el-option']=defineComponent({props:['value','label'],setup(props,{slots}){return()=>h('option',{value:props.value},slots.default?slots.default():props.label)}})
  const form={managerUserId:1};const w=shallowMount(LabEditor,{props:{title:'新增实验室',modelValue:true,formModel:form,
    managerOptions:[{id:1,username:'legacy'},{id:2,username:'admin',realName:'管理员',roleName:'实验室管理员'}]},global:{stubs}})
  wrappers.push(w);expect(w.text()).toContain('legacy · 未分配角色');expect(w.text()).toContain('admin · 实验室管理员')
  await w.get('select[aria-label="请选择负责人"]').setValue('2');expect(String(form.managerUserId)).toBe('2')
})
