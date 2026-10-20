// @vitest-environment happy-dom
import { afterEach,expect,it } from 'vitest'
import { defineComponent,h,reactive } from 'vue'
import { shallowMount } from '@vue/test-utils'
import Dialog from '../../src/components/business-management/BusinessRecordDialog.vue'
import { panelStubs } from '../support/formStubs'
const wrappers=[]
afterEach(()=>wrappers.splice(0).forEach(w=>w.unmount()))
it('business editor exposes tagged options, empty choices and edits all date and numeric controls',async()=>{
 const model=reactive({date:'',timestamp:'',count:0,status:'on'})
 const stubs=panelStubs()
 for(const key of ['el-dialog','el-option','el-select'])stubs[key]=defineComponent({props:['label','modelValue'],setup(p,{slots}){return()=>h('section',{'data-kind':key},[p.label,...Object.values(slots).map(slot=>slot())])}})
 const w=shallowMount(Dialog,{props:{modelValue:true,dialogTitle:'环境配置',formModel:model,editableColumns:[{prop:'date',label:'日期'},{prop:'timestamp',label:'时间'},{prop:'count',label:'数量'},{prop:'status',label:'状态'}],statusOptions:[{value:'on',label:'启用',type:'success'},{value:'off',label:'停用'}]},global:{stubs}});wrappers.push(w)
 expect(w.text()).toContain('请完善基础信息');expect(w.text()).toContain('暂无可选数据');expect(w.find('[data-element="tag"]').text()).toBe('启用')
 await w.get('input[placeholder="请选择日期"]').setValue('2027-01-04')
 await w.get('input[placeholder="请选择时间"]').setValue('2027-01-04 10:00:00')
 await w.get('input[placeholder="请输入数量"]').setValue(7)
 expect(model).toMatchObject({date:'2027-01-04',timestamp:'2027-01-04 10:00:00',count:7})
 const options=[{teacherName:'教师甲'},{courseName:'程序设计'},{name:'软件'},{roomName:'机房'},{classroomName:'教室'},{value:0},{displayName:'显示名',label:'资源'}]
 for(const option of options)expect(w.vm.normalizeOption(option).label).not.toBe('')
 expect(w.vm.normalizeOption({})).toMatchObject({label:'',value:''})
 const labelOnly={prop:'',label:''};expect(w.vm.getPlaceholder(labelOnly)).toBe('请输入')
 expect(w.vm.canCreateOption({prop:''})).toBe(false)
})
