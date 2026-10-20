// @vitest-environment happy-dom
import { afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Request from '../../src/views/teacher/components/TeacherEnvironmentRequestDialog.vue'
import { formStubs } from '../support/formStubs'
import { ElMessage } from 'element-plus'
vi.mock('element-plus',()=>({ElMessage:{warning:vi.fn()}}))
const wrappers=[]
afterEach(()=>{wrappers.splice(0).forEach(w=>w.unmount());vi.clearAllMocks()})
const valid={courseName:'程序设计',className:'软件一班',useTime:'周一上午',labType:'开发实验室',softwareRequirements:'IDEA'}
it.each([
 ['className','请填写授课班级'],['useTime','请填写使用时间'],['labType','请选择实验室类型'],['softwareRequirements','请填写软件环境要求']
])('the environment request rejects a blank %s before emitting a save',async(key,message)=>{
 const w=shallowMount(Request,{props:{modelValue:true,prefill:valid},global:{stubs:formStubs()}});wrappers.push(w)
 w.vm.formModel[key]='  ';await w.findAll('button').find(b=>b.text()==='提交需求').trigger('click')
 expect(ElMessage.warning).toHaveBeenLastCalledWith(message);expect(w.emitted('save')).toBeUndefined()
})
it('reopening resets drafts, preserves predefined room choices and handles an empty choice list',async()=>{
 const w=shallowMount(Request,{props:{modelValue:false,labTypeOptions:[],prefill:null},global:{stubs:formStubs()}});wrappers.push(w)
 expect(w.vm.formModel.labType).toBe('')
 await w.setProps({modelValue:true});expect(w.vm.formModel.labType).toBe('')
 await w.setProps({labTypeOptions:['开发实验室'],prefill:valid});expect(w.vm.resolvedLabTypeOptions).toEqual(['开发实验室'])
 w.vm.formModel.courseName='未提交编辑';await w.setProps({modelValue:false});expect(w.vm.formModel.courseName).toBe('未提交编辑')
 await w.setProps({modelValue:true});expect(w.vm.formModel.courseName).toBe('程序设计')
})
