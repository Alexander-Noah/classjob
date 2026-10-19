// @vitest-environment happy-dom
import { afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import { formStubs } from '../support/formStubs'
import Dialog from '../../src/views/users/components/PermissionUserDialog.vue'
const wrappers=[]
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
function setup(props={}){const wrapper=shallowMount(Dialog,{props:{modelValue:true,roleOptions:['实验室管理员'],...props},global:{stubs:formStubs()}});wrappers.push(wrapper);return {vm:wrapper.vm,wrapper}}
it('new accounts have password rules and edit resets never retain plaintext initial passwords',async()=>{const {vm,wrapper}=setup();expect(vm.isCreateMode).toBe(true);expect(vm.formModel.role).toBe('实验室管理员');expect(vm.rules.initialPassword).toHaveLength(2);vm.formModel.initialPassword='secret';await wrapper.setProps({user:{id:7,username:'labadmin',realName:'管理员',department:'计算机',enabled:false,roleName:'实验室管理员'}});await flushPromises();expect(vm.isCreateMode).toBe(false);expect(vm.rules.initialPassword).toEqual([]);expect(vm.formModel).toMatchObject({account:'labadmin',name:'管理员',enabled:false,initialPassword:''})})
it('phone and role validators reject malformed or retired role choices',()=>{const {vm}=setup();const callback=vi.fn();for(const value of ['', '13812345678']){vm.validatePhone({},value,callback);expect(callback.mock.calls.at(-1)[0]).toBeUndefined()}vm.validatePhone({},'123',callback);expect(callback.mock.calls.at(-1)[0]).toBeInstanceOf(Error);vm.validateAssignableRole({},'教师',callback);expect(callback.mock.calls.at(-1)[0].message).toContain('已停用');vm.validateAssignableRole({},'实验室管理员',callback);expect(callback.mock.calls.at(-1)[0]).toBeUndefined()})
it('save emits a detached payload only after form validation passes',async()=>{const {vm,wrapper}=setup();vm.formModel.name='管理员';vm.formRef={clearValidate:vi.fn(),validate:vi.fn().mockRejectedValue('invalid')};await vm.handleSave();expect(wrapper.emitted('save')).toBeUndefined();vm.formRef.validate.mockResolvedValue(true);await vm.handleSave();expect(wrapper.emitted('save')[0][0].name).toBe('管理员');vm.formModel.name='changed';expect(wrapper.emitted('save')[0][0].name).toBe('管理员');vm.visible=false;expect(wrapper.emitted('update:modelValue')).toEqual([[false]])})

