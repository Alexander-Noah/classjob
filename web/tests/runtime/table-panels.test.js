// @vitest-environment happy-dom
import { describe,it,expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { defineComponent,h } from 'vue'
import { tableStubs } from '../support/formStubs'
import BusinessRecordTable from '../../src/components/business-management/BusinessRecordTable.vue'
import ReservationListTable from '../../src/views/reservations/components/ReservationListTable.vue'
import LabSelectorPanel from '../../src/views/room-control/components/LabSelectorPanel.vue'
import DeviceControlPanel from '../../src/views/room-control/components/DeviceControlPanel.vue'

const mount=(component,props={})=>{const stubs=tableStubs();stubs['el-slider']=defineComponent({name:'TestSlider',props:['modelValue'],emits:['change'],setup(props,{emit}){return()=>h('input',{type:'range',value:props.modelValue,onChange:e=>emit('change',Number(e.target.value))})}});stubs['el-card']=defineComponent({setup(_,{slots}){return()=>h('section',[slots.header?.(),slots.default?.()])}});return shallowMount(component,{props,global:{stubs}})}
const click=async(w,label)=>{const button=w.findAll('button').find(b=>b.text()===label);expect(button,label).toBeTruthy();await button.trigger('click')}

describe('rendered tables and room panels',()=>{
  it('dispatches record actions, filters, pagination and row selection with original records',async()=>{
    const row={_recordId:'a',name:'设备借用',status:'待处理'}
    const w=mount(BusinessRecordTable,{filteredRecords:[row],activeRecordId:'a',selectedCount:1,getRowTagType:()=> 'warning',tableColumns:[{prop:'name',label:'名称'},{prop:'status',label:'状态',type:'tag'}],statusOptions:[{label:'待处理'}]})
    expect(w.text()).toContain('设备借用');expect(w.text()).toContain('待处理')
    await w.get('input').setValue('设备');await w.get('select').setValue('待处理')
    expect(w.emitted('update:keyword')[0]).toEqual(['设备']);expect(w.emitted('update:statusFilter')[0]).toEqual(['待处理'])
    for(const [label,event] of [['新增','create'],['标记为已处理','batch-status'],['删除所选','delete-selected'],['导出','export'],['编辑','edit'],['删除','delete']]){await click(w,label);expect(w.emitted(event)).toHaveLength(1)}
    expect(w.emitted('edit')[0]).toEqual([row]);expect(w.emitted('delete')[0]).toEqual([row])
    const table=w.findComponent({name:'TestTable'});table.vm.$emit('row-click',row);table.vm.$emit('selection-change',[row]);expect(w.emitted('row-click')[0]).toEqual([row]);expect(w.emitted('selection-change')[0]).toEqual([[row]])
    expect(table.attributes('row-class-name')).toBeDefined()
    const pagination=w.findComponent({name:'Testpagination'});pagination.vm.$emit('current-change',2);pagination.vm.$emit('size-change',20);expect(w.emitted('page-change')[0]).toEqual([2]);expect(w.emitted('page-size-change')[0]).toEqual([20])
    await w.setProps({selectedCount:0});expect(w.text()).toContain('勾选记录后可批量标记或删除');w.unmount()
  })
  it('shows reservation details and emits each enabled audit action',async()=>{
    const row={id:1,applicant:'申请甲',lab:'实验室甲',date:'2099-01-01',timeRange:'08:00-10:00',status:'待审核',conflict:false,reason:'上课'}
    const w=mount(ReservationListTable,{reservations:[row],rowOffset:10});expect(w.text()).toContain('申请甲');expect(w.text()).toContain('上课');expect(w.get('[data-column="编号"]').text()).toBe('11')
    for(const [label,event] of [['通过','approve'],['驳回','reject'],['编辑','edit'],['取消预约','cancel'],['删除','delete']]){await click(w,label);expect(w.emitted(event)?.[0]).toEqual([row])}
    await w.setProps({reservations:[{...row,status:'已取消',conflict:true}]});expect(w.get('[data-column="冲突"]').text()).toContain('冲突');expect(w.findAll('button').find(b=>b.text()==='通过').element.disabled).toBe(true);w.unmount()
  })
  it('filters labs by nested device readings and status, then selects the visible lab',async()=>{
    const labs=[{id:'a',name:'甲机房',location:'一楼',capacity:30,status:'空闲',statusType:'success',door:{doorState:'关闭'},sensor:{temperature:25,smoke:0}},{id:'b',name:'乙机房',location:'二楼',capacity:50,status:'使用中',door:{doorState:'开启'},sensor:{temperature:28,smoke:1}}]
    const w=mount(LabSelectorPanel,{labs,selectedLabId:'a'});expect(w.findAll('.lab-select-card')).toHaveLength(2);expect(w.get('.lab-select-card').classes()).toContain('active')
    await w.get('input').setValue('28');expect(w.findAll('.lab-select-card')).toHaveLength(1);await w.get('.lab-select-card').trigger('click');expect(w.emitted('select')[0]).toEqual([labs[1]])
    await w.get('select').setValue('空闲');expect(w.findAll('.lab-select-card')).toHaveLength(0);await w.get('input').setValue('');expect(w.findAll('.lab-select-card')).toHaveLength(1);w.unmount()
  })
  it('updates capability state before emitting hardware commands',async()=>{
    const capabilities=[{key:'power',type:'switch',enabled:true,label:'电源'},{key:'temperature',type:'slider',enabled:true,label:'温度',unit:'℃'},{key:'mode',type:'select',enabled:true,label:'模式',options:['制冷','制热']},{key:'status',type:'button',enabled:true,label:'刷新'},{key:'open',type:'button',enabled:true,label:'开门'}]
    const device={code:'D1',type:'air-conditioner',name:'空调',sourceType:'mock',online:true,capabilities};const state={}
    const w=mount(DeviceControlPanel,{devices:[device,{id:2,type:'unknown',name:'未知设备',configured:true,capabilities:[]}],capabilityState:state})
    expect(w.text()).toContain('模拟网关演示');expect(w.text()).toContain('待配置真实网关')
    await w.get('input[type="checkbox"]').setValue(true);expect(state['D1::power']).toEqual({enabled:true,value:''})
    await w.get('input[type="range"]').setValue('23');expect(state['D1::temperature']).toEqual({enabled:false,value:23})
    await w.get('select').setValue('制热');expect(state['D1::mode'].value).toBe('制热');await click(w,'刷新');await click(w,'开门');expect(w.emitted('capability')).toHaveLength(5);expect(w.emitted('capability')[0]).toEqual([device,capabilities[0]])
    await w.setProps({loading:true});expect(w.findAll('.device-control-card')).toHaveLength(0);w.unmount()
  })
})
