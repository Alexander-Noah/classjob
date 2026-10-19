// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { reactive,ref,defineComponent,h } from 'vue'
import { formStubs } from '../support/formStubs'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Layout from '../../src/views/layout/AdminLayout.vue'
import * as auth from '../../src/api/services/auth'
import { uploadUserAvatar } from '../../src/api/services/users'
import { ElMessage } from 'element-plus'
const navigation=vi.hoisted(()=>({push:vi.fn(),replace:vi.fn()}));let route
vi.mock('vue-router',()=>({useRoute:()=>route,useRouter:()=>navigation}))
vi.mock('../../src/api/services/auth',()=>({getStoredRole:()=> 'teacher',getStoredUser:()=>({id:7,username:'teacher',realName:'教师甲',roleName:'教师',department:'计算机',avatarUrl:''}),logout:vi.fn(),updateCurrentUserProfile:vi.fn(),updateStoredUser:vi.fn(user=>user)}))
vi.mock('../../src/api/services/users',()=>({uploadUserAvatar:vi.fn()}))
vi.mock('../../src/composables/useAdminGlass',()=>({useAdminGlass:()=>({glassRoot:ref(null)})}))
vi.mock('../../src/composables/useCurrentTime',()=>({useCurrentTime:()=>({currentTime:ref('2026-10-18')})}))
vi.mock('element-plus',()=>({ElImageViewer:{template:'<div />'},ElMessage:{success:vi.fn(),error:vi.fn(),warning:vi.fn()}}))
const wrappers=[]
function layoutStubs(){const stubs=formStubs();for(const name of ['container','aside','header','main','menu','sub-menu','menu-item','breadcrumb','breadcrumb-item','avatar','upload'])stubs[`el-${name}`]=defineComponent({setup(_,{slots}){return()=>h('section',[slots.default?.(),slots.title?.(),slots.dropdown?.(),slots.trigger?.()])}});return stubs}
it('rendered navigation and profile editor bind changes and sidebar controls',async()=>{route.meta.roleScope='labAdmin';const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('教师甲');expect(w.get('.brand-lockup').attributes('src')).toBe('/login-brand-transparent.png');await w.get('[aria-label="切换菜单"]').trigger('click');expect(vm.isSidebarCollapsed).toBe(true);window.innerWidth=600;await w.get('[aria-label="切换菜单"]').trigger('click');expect(w.find('.sidebar-mask').exists()).toBe(true);await w.get('.sidebar-mask').trigger('click');expect(vm.isMobileMenuOpen).toBe(false);vm.openProfileDialog();await flushPromises();for(const input of w.findAll('input').filter(x=>!x.element.disabled))await input.setValue('修改值');expect(vm.profileForm.realName).toBe('修改值');expect(vm.profileForm.department).toBe('修改值');vm.profileForm.avatarUrl='data:image/png;base64,a';await flushPromises();expect(w.find('.profile-panel').exists()).toBe(true)})
beforeEach(()=>{vi.clearAllMocks();route=reactive({path:'/teacher',meta:{roleScope:'teacher'},matched:[{meta:{title:'教师工作台'}}]});auth.updateCurrentUserProfile.mockImplementation(async user=>user);uploadUserAvatar.mockResolvedValue({objectKey:'avatars/7.png'});vi.spyOn(URL,'createObjectURL').mockReturnValue('blob:preview');vi.spyOn(URL,'revokeObjectURL').mockImplementation(()=>{});Object.defineProperty(window,'innerWidth',{value:1200,writable:true,configurable:true})})
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.restoreAllMocks()})
async function setup(render=false){const wrapper=shallowMount(Layout,{global:{stubs:render?layoutStubs():Object.fromEntries(['container','aside','header','main','menu','sub-menu','menu-item','menu-item-group','icon','button','breadcrumb','breadcrumb-item','dropdown','dropdown-menu','dropdown-item','avatar','dialog','form','form-item','input','upload','scrollbar','tooltip','router-view','router-link'].map(name=>[name.startsWith('router')?name:`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('uses session identity, role menus and route breadcrumbs',async()=>{
  const vm=await setup();expect(vm.currentUserName).toBe('教师甲');expect(vm.currentUserRole).toBe('教师');route.meta.roleScope='labAdmin';expect(vm.navMenus.length).toBeGreaterThan(0);expect(vm.sidebarWidth).toBe('236px');expect(vm.activeMenu).toBe('/teacher');expect(vm.breadcrumbItems).toEqual([{title:'教师工作台',to:undefined}]);route.meta.breadcrumb=[{title:'首页',path:'/teacher'},{title:'课程'}];expect(vm.breadcrumbItems).toEqual([{title:'首页',to:{path:'/teacher'}},{title:'课程',to:undefined}]);route.meta.activeMenu='/teacher/schedule';expect(vm.activeMenu).toBe('/teacher/schedule')
})
it('toggles desktop and mobile navigation and skips redundant navigation',async()=>{
  const vm=await setup();vm.handleToggleSidebar();expect(vm.sidebarWidth).toBe('72px');window.innerWidth=600;vm.handleToggleSidebar();expect(vm.isMobileMenuOpen).toBe(true);vm.handleMenuSelect('/teacher');expect(navigation.push).not.toHaveBeenCalled();expect(vm.isMobileMenuOpen).toBe(false);vm.handleMenuSelect('/teacher/schedule');expect(navigation.push).toHaveBeenCalledWith('/teacher/schedule')
})
it('validates profile name and saves normalized identity into the shared session',async()=>{
  const vm=await setup();vm.openProfileDialog();expect(vm.profileForm.username).toBe('teacher');vm.profileForm.realName=' ';await vm.saveProfile();expect(auth.updateCurrentUserProfile).not.toHaveBeenCalled();Object.assign(vm.profileForm,{realName:' 新姓名 ',department:' 计算机系 ',phone:' 123 ',email:' a@b.test '});await vm.saveProfile();expect(auth.updateCurrentUserProfile).toHaveBeenCalledWith(expect.objectContaining({realName:'新姓名',name:'新姓名',department:'计算机系',phone:'123',email:'a@b.test'}));expect(vm.currentUserName).toBe('新姓名');expect(vm.profileDialogVisible).toBe(false);expect(vm.profileSubmitting).toBe(false);expect(auth.updateStoredUser).toHaveBeenCalledWith(expect.any(Object),'teacher')
})
it('uploads a selected avatar before saving and revokes preview URLs after save',async()=>{
  const vm=await setup();vm.openProfileDialog();const file=new File(['png'],'avatar.png',{type:'image/png'});vm.handleAvatarSelection({raw:file});expect(vm.pendingAvatarFile.name).toBe(file.name);expect(vm.currentAvatarUrl).toBe('blob:preview');vm.openAvatarViewer(vm.currentAvatarUrl);expect(vm.avatarViewerUrl).toBe('blob:preview');await vm.saveProfile();expect(uploadUserAvatar).toHaveBeenCalledWith(file);expect(auth.updateCurrentUserProfile.mock.calls[0][0].avatarUrl).toContain('objectKey=avatars%2F7.png');expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview');expect(vm.avatarViewerUrl).toBe('');expect(vm.pendingAvatarFile).toBeNull()
})
it('rejects unsupported and oversized images without replacing the current preview',async()=>{
  const vm=await setup();expect(vm.validateAvatarFile({type:'text/plain',size:1})).toBe(false);expect(vm.validateAvatarFile({type:'image/png',size:2*1024*1024+1})).toBe(false);expect(vm.validateAvatarFile({type:'image/jpeg',size:2*1024*1024})).toBe(true);vm.handleAvatarSelection({type:'text/plain',size:1});expect(vm.pendingAvatarFile).toBeNull();expect(vm.resolveUploadedAvatarUrl({url:'data:image/png;base64,a'})).toBe('data:image/png;base64,a');expect(vm.buildAvatarProxyUrl('')).toBe('');expect(vm.resolveUploadedAvatarUrl({})).toBe('')
})
it('profile failures keep edits and suppress messages already handled by HTTP',async()=>{
  const vm=await setup();vm.openProfileDialog();auth.updateCurrentUserProfile.mockRejectedValueOnce({userMessage:'无法保存'});await vm.saveProfile();expect(vm.profileDialogVisible).toBe(true);expect(vm.profileSubmitting).toBe(false);expect(ElMessage.error).toHaveBeenCalledWith('无法保存');ElMessage.error.mockClear();auth.updateCurrentUserProfile.mockRejectedValueOnce({__handled:true});await vm.saveProfile();expect(ElMessage.error).not.toHaveBeenCalled();auth.updateCurrentUserProfile.mockImplementation(async user=>user);await vm.saveProfile({closeDialog:false,successMessage:'自动保存'});expect(vm.profileDialogVisible).toBe(true);expect(ElMessage.success).toHaveBeenCalledWith('自动保存')
})
it('avatar viewer avoids upload clicks and cleanup releases previews on unmount',async()=>{
  const vm=await setup();vm.profileForm.avatarUrl='data:image/png;base64,a';vm.handleProfileAvatarPreview({target:{closest:()=>({})}});expect(vm.avatarViewerUrl).toBe('');vm.handleProfileAvatarPreview({target:{closest:()=>null}});expect(vm.avatarViewerUrl).toBe('data:image/png;base64,a');vm.closeAvatarViewer();vm.handleAvatarSelection(new File(['x'],'a.webp',{type:'image/webp'}));wrappers[0].unmount();expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview')
})
it('user commands open profile or clear authentication and replace the login route',async()=>{
  const vm=await setup();await vm.handleUserCommand('profile');expect(vm.profileDialogVisible).toBe(true);await vm.handleUserCommand('unknown');expect(auth.logout).not.toHaveBeenCalled();await vm.handleUserCommand('logout');expect(auth.logout).toHaveBeenCalled();expect(navigation.replace).toHaveBeenCalledWith('/login')
})

