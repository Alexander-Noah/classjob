// @vitest-environment happy-dom
import { afterEach,expect,it } from 'vitest'
import { defineComponent,h,reactive } from 'vue'
import { shallowMount } from '@vue/test-utils'
import Transfer from '../../src/views/devices/components/DeviceTransferDialog.vue'
import Inventory from '../../src/views/devices/components/DeviceInventoryDialog.vue'
import { panelStubs } from '../support/formStubs'
const wrappers=[]
afterEach(()=>wrappers.splice(0).forEach(w=>w.unmount()))
function mount(component,props){const stubs=panelStubs();stubs['el-option']=defineComponent({props:['value','label'],setup(p,{slots}){return()=>h('option',{value:p.value},slots.default?slots.default():p.label)}});const w=shallowMount(component,{props,global:{stubs}});wrappers.push(w);return w}
it('transfer dialog displays legacy identities and owner role metadata, then emits cancellation',async()=>{
 const w=mount(Transfer,{modelValue:true,formModel:{},devices:[],ownerOptions:[{id:1,name:'姓名甲',roleName:'管理员'},{id:2,username:'teacher'}]})
 expect(w.vm.firstDevice).toBeNull();expect(w.text()).toContain('管理员')
 for(const [device,text] of [[{name:'旧设备',code:'OLD',lab:'机房',owner:'所有者'},'OLD · 机房 · 所有者'],[{deviceCode:'D-1',roomNo:'A101',ownerUsername:'user'},'D-1 · A101 · user'],[{},'- · 未关联实验室 · 未设置责任人']]){
  await w.setProps({devices:[device]});expect(w.text()).toContain(text)
 }
 const cancel=w.findAll('button').find(b=>b.text()==='取消');await cancel.trigger('click');expect(w.emitted('update:modelValue')).toEqual([[false]])
})
it('inventory renders batch and sparse devices and never clears an existing exception when normal is unchecked',async()=>{
 const model=reactive({normal:true,resultStatus:'已盘点'}),w=mount(Inventory,{modelValue:true,formModel:model,devices:[]})
 expect(w.vm.summaryDevice).toBeNull()
 await w.setProps({devices:[{name:'旧设备',code:'OLD',lab:'旧机房',status:'维修中'}]});expect(w.text()).toContain('OLD · 旧机房')
 const boxes=w.findAll('input[type="checkbox"]');await boxes[0].setValue(false);expect(model.resultStatus).toBe('盘点异常')
 model.resultStatus='设备缺失';model.normal=true;await boxes[0].setValue(false);expect(model.resultStatus).toBe('设备缺失')
 await w.setProps({devices:[{deviceCode:'D-1',roomNo:'A101',status:'已报废'}]});expect(w.text()).toContain('D-1 · A101')
 await w.setProps({devices:[{status:'自定义'}]});expect(w.text()).toContain('未命名设备');expect(w.text()).toContain('- · 未关联实验室')
 await w.setProps({devices:[{assetNo:'PC1'},{code:'PC2'}]});expect(w.text()).toContain('已选择 2 台设备');expect(w.text()).toContain('PC1、PC2')
})
