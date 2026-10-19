// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { ref } from 'vue'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/login/Login.vue'
import * as auth from '../../src/api/services/auth'
import { ElMessage } from 'element-plus'
import http from '../../src/api/http'
const router=vi.hoisted(()=>({replace:vi.fn()}))
vi.mock('vue-router',()=>({useRouter:()=>router}))
vi.mock('../../src/composables/useLiquidGlass',()=>({useLiquidGlass:()=>({glassElement:ref(null)})}))
vi.mock('../../src/api/services/auth',()=>({getRememberedLogin:vi.fn(),loginByPassword:vi.fn(),persistRememberedLogin:vi.fn()}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),error:vi.fn(),info:vi.fn()}}))
vi.mock('../../src/api/http',()=>({default:{get:vi.fn().mockResolvedValue({data:{data:{captchaId:'challenge-id',image:'data:image/png;base64,test'}}})}}))
const wrappers=[]
it('malformed captcha responses clear the challenge and allow a safe retry',async()=>{
  http.get.mockResolvedValueOnce({data:{data:{captchaId:'bad-image',image:'javascript:alert(1)'}}})
  const wrapper=setup();await flushPromises()
  expect(wrapper.text()).toContain('验证码加载失败')
  expect(wrapper.vm.captchaId).toBe('')
  expect(wrapper.find('.captcha-refresh img').exists()).toBe(false)
  await wrapper.find('[aria-label="刷新图片验证码"]').trigger('click');await flushPromises()
  expect(wrapper.vm.captchaId).toBe('challenge-id')
  expect(wrapper.find('.captcha-refresh img').attributes('src')).toMatch(/^data:image\/png/)
})
beforeEach(()=>{vi.clearAllMocks();auth.getRememberedLogin.mockReturnValue({username:'remembered',remember:true});auth.loginByPassword.mockResolvedValue({role:'labAdmin'})})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
function setup(){const wrapper=shallowMount(Page);wrappers.push(wrapper);return wrapper}
it('remembered username never restores a password and validation blocks missing fields',async()=>{const wrapper=setup(),vm=wrapper.vm;expect(vm.form).toMatchObject({username:'remembered',password:'',remember:true});vm.form.username=' ';await wrapper.find('form').trigger('submit');expect(auth.loginByPassword).not.toHaveBeenCalled();expect(wrapper.text()).toContain('请输入用户名');expect(wrapper.text()).toContain('请输入密码');expect(vm.isSubmitting).toBe(false)})
it('login submits trimmed username with chosen remember preference and redirects by saved role',async()=>{const wrapper=setup();const inputs=wrapper.findAll('input');await inputs[0].setValue(' labadmin ');await inputs[1].setValue('test-password');await wrapper.find('input[type="checkbox"]').setValue(false);await flushPromises();wrapper.vm.captchaCode='AB23';await wrapper.find('form').trigger('submit');await flushPromises();expect(auth.loginByPassword).toHaveBeenCalledWith({username:'labadmin',password:'test-password',remember:false,captchaId:'challenge-id',captchaCode:'AB23'});expect(auth.persistRememberedLogin).toHaveBeenCalledWith({username:'labadmin',remember:false});expect(router.replace).toHaveBeenCalledWith('/lab-admin');expect(wrapper.vm.isSubmitting).toBe(false)})
it('password visibility, password recovery and registration tabs use actual controls',async()=>{const wrapper=setup();expect(wrapper.find('input[type="password"]').exists()).toBe(true);await wrapper.find('[aria-label="显示密码"]').trigger('click');expect(wrapper.vm.passwordInputType).toBe('text');await wrapper.find('.forgot-link').trigger('click');expect(ElMessage.info).toHaveBeenCalledWith('请联系系统管理员重置密码');await wrapper.find('#register-tab').trigger('click');expect(wrapper.text()).toContain('平台账号由系统管理员统一创建');expect(wrapper.find('form').exists()).toBe(false);await wrapper.find('.registration button').trigger('click');expect(wrapper.find('form').exists()).toBe(true)})
it('failed login preserves edits, releases busy state and suppresses already handled errors',async()=>{const wrapper=setup(),vm=wrapper.vm;vm.form.password='test-password';await flushPromises();vm.captchaCode='AB23';auth.loginByPassword.mockRejectedValueOnce(new Error('凭据错误'));await vm.handleLogin();expect(ElMessage.error).toHaveBeenCalledWith('凭据错误');expect(vm.form.password).toBe('test-password');expect(vm.isSubmitting).toBe(false);expect(router.replace).not.toHaveBeenCalled();ElMessage.error.mockClear();auth.loginByPassword.mockRejectedValueOnce({__handled:true});vm.captchaCode='AB23';await vm.handleLogin();expect(ElMessage.error).not.toHaveBeenCalled();expect(vm.getRedirectPath('unknown')).toBe('/lab-admin')})
it('captcha is required and refreshing clears the previous answer',async()=>{const wrapper=setup();await flushPromises();wrapper.vm.form.password='pw';await wrapper.vm.handleLogin();expect(auth.loginByPassword).not.toHaveBeenCalled();expect(wrapper.text()).toContain('请输入四位图片验证码');wrapper.vm.captchaCode='AB23';await wrapper.find('[aria-label="刷新图片验证码"]').trigger('click');await flushPromises();expect(wrapper.vm.captchaCode).toBe('');expect(wrapper.vm.captchaId).toBe('challenge-id')})
