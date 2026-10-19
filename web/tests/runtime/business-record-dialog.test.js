// @vitest-environment happy-dom
import { afterEach,expect,it,vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import Dialog from '../../src/components/business-management/BusinessRecordDialog.vue'
const wrappers=[]
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.restoreAllMocks()})
function setup(props={}){const wrapper=shallowMount(Dialog,{props:{dialogTitle:'编辑业务记录',formModel:{},editableColumns:[],modelValue:true,...props},global:{stubs:Object.fromEntries(['dialog','form','form-item','input','input-number','select','option','date-picker','button','row','col'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);return {vm:wrapper.vm,wrapper}}
it('filters invalid columns and generates required validation rules using submission keys',()=>{
  const {vm}=setup({editableColumns:[null,{}, {prop:'name',label:'名称'},{prop:'labName',formProp:'roomId',label:'实验室',type:'select'},{prop:'remark',label:'备注',optional:true},{prop:'date',label:'日期',required:false}]});expect(vm.normalizedColumns).toHaveLength(4);expect(vm.formRules).toEqual({name:[{required:true,message:'请填写名称',trigger:'blur'}],roomId:[{required:true,message:'请选择实验室',trigger:'change'}]});expect(vm.getFormProp({prop:'name',modelProp:'courseName'})).toBe('courseName')
})
it.each([['remark','备注','textarea'],['purchaseDate','购置日期','date'],['timestamp','时间','datetime'],['count','数量','number'],['name','名称','input']])('infers field kind for %s', (prop,label,type)=>{const {vm}=setup();expect(vm.getFieldKind({prop,label})).toBe(type);expect(vm.shouldFullRow({prop,label})).toBe(type==='textarea');expect(vm.getPlaceholder({prop,label})).toBe(`${['date','datetime'].includes(type)?'请选择':'请输入'}${label}`)})
it('respects explicit types and prevents free creation for linked resource identities or statuses',()=>{
  const {vm}=setup();expect(vm.getFieldKind({prop:'name',formType:'textarea'})).toBe('textarea');expect(vm.getFieldKind({prop:'name',fieldType:'number'})).toBe('number');expect(vm.getFieldKind({prop:'name',type:'date'})).toBe('date');expect(vm.getFieldKind({prop:'status'})).toBe('select');expect(vm.getFieldKind({prop:'other',type:'tag'})).toBe('select');for(const prop of ['labId','lab','labName','labs','teacher','teacherName','className','status'])expect(vm.canCreateOption({prop,allowCreate:true})).toBe(false);expect(vm.canCreateOption({prop:'version'})).toBe(true);expect(vm.canCreateOption({prop:'version',allowCreate:false})).toBe(false);expect(vm.canCreateOption({prop:'custom',allowCreate:true})).toBe(true)
})
it('normalizes custom option keys and excludes empty identities while preserving numeric zero',()=>{
  const {vm}=setup({fieldOptions:{courseId:[{courseId:0,courseName:'零号课程'},{courseId:7,courseName:'程序设计'},'',null]},statusOptions:['启用',{label:'停用',value:0}]});expect(vm.getColumnOptions({prop:'courseId'})).toEqual([{label:'零号课程',value:0,displayValue:'零号课程',type:''},{label:'程序设计',value:7,displayValue:'程序设计',type:''}]);expect(vm.getColumnOptions({prop:'status'}).map(item=>item.value)).toEqual(['启用',0]);expect(vm.normalizeOption({key:7,text:'课程',tagType:'success'},{optionLabel:'text',optionValue:'key'})).toEqual({label:'课程',value:7,displayValue:'课程',type:'success'});expect(vm.hasOptions({prop:'name',options:['IDEA']})).toBe(true)
})
it('selecting linked identities updates the display field without changing unrelated input',()=>{
  vi.spyOn(console,'log').mockImplementation(()=>{});const model={labId:7,name:'项目'};const {vm}=setup({formModel:model,fieldOptions:{labId:[{id:7,labName:'A101 机房'}]}});vm.handleSelectChange({prop:'labName',formProp:'labId'},7);expect(model).toEqual({labId:7,labName:'A101 机房',name:'项目'});vm.handleSelectChange({prop:'labName',formProp:'labId'},99);expect(model.labName).toBe('A101 机房');expect(vm.getFieldKind({prop:'lab',options:['A101']})).toBe('select')
})
it('field option lists override static options, with static fallback when dynamic options are empty',()=>{
  const {vm}=setup({fieldOptions:{course:['动态'],name:[]}});expect(vm.getColumnOptions({prop:'course',options:['静态']})[0].value).toBe('动态');expect(vm.getColumnOptions({prop:'name',options:['静态']})[0].value).toBe('静态');expect(vm.normalizeOption({username:'教师',id:8})).toMatchObject({label:'教师',value:8});expect(vm.normalizeOption({title:'标题',code:'T1'})).toMatchObject({label:'标题',value:'T1'})
})
it('cancel emits visibility updates and submit only emits after successful validation',async()=>{
  const {vm,wrapper}=setup();expect(vm.visible).toBe(true);vm.handleCancel();expect(wrapper.emitted('update:modelValue')).toEqual([[false]]);await vm.handleSubmit();expect(wrapper.emitted('submit')).toHaveLength(1);vm.formRef={validate:vi.fn().mockRejectedValue('invalid')};await vm.handleSubmit();expect(wrapper.emitted('submit')).toHaveLength(1);vm.formRef.validate.mockResolvedValue(true);await vm.handleSubmit();expect(wrapper.emitted('submit')).toHaveLength(2)
})
