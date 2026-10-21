// @vitest-environment happy-dom
import { beforeEach,expect,it,vi } from 'vitest'
import router from '../../src/router'
import * as auth from '../../src/api/services/auth'
import { ElMessage } from 'element-plus'
import { refreshPageAccess } from '../../src/api/services/rolePageAccess'
import catalog from '../../../Backend/src/main/resources/security/role-pages.json'
const capture=vi.hoisted(()=>({guard:null,options:null}))
vi.mock('vue-router',async original=>({...await original(),createWebHistory:()=>({}),createRouter:options=>{capture.options=options;return {beforeEach:guard=>capture.guard=guard}}}))
vi.mock('../../src/api/services/auth',()=>({assertActiveSessionRole:vi.fn(),clearAuthSession:vi.fn(),getStoredRole:vi.fn(),getStoredUser:vi.fn(),isAuthenticated:vi.fn(),refreshCurrentUserSession:vi.fn()}))
vi.mock('element-plus',()=>({ElMessage:{error:vi.fn()}}))
vi.mock('@wxperia/liquid-glass-vue',()=>({}))
vi.mock('../../src/api/services/rolePageAccess',()=>({refreshPageAccess:vi.fn()}))
const route=(extra={})=>({path:'/lab-admin',fullPath:'/lab-admin',name:'LabAdminPortal',matched:[{meta:{requiresAuth:true}}],meta:{title:'工作台',roleScope:'labAdmin'},...extra})
beforeEach(()=>{vi.resetAllMocks();auth.getStoredRole.mockReturnValue('labAdmin');auth.getStoredUser.mockReturnValue({roleCode:'labAdmin',permissions:[]});auth.isAuthenticated.mockReturnValue(true);auth.refreshCurrentUserSession.mockResolvedValue({});refreshPageAccess.mockImplementation(async role=>({role,accessiblePaths:catalog.filter(p=>p.role===role).map(p=>p.path)}))})
it('every registered lazy route resolves to a Vue component module',async()=>{const visit=async routes=>{for(const entry of routes){if(typeof entry.component==='function'){const module=await entry.component();expect(module.default,entry.path).toBeTruthy()}if(entry.children)await visit(entry.children)}};await visit(capture.options.routes)},30000)
it('registered routes have role metadata and preserve legacy redirect targets',()=>{expect(router).toBeTruthy();const routes=capture.options.routes;expect(routes.find(route=>route.path==='/repairs').redirect).toBe('/lab-admin/maintenance-records');expect(routes.find(route=>route.path==='/users').redirect).toBe('/system-admin/users');const children=routes.find(route=>route.path==='/').children;const home=children.find(route=>route.name==='LabAdminPortal');expect(home.meta.breadcrumb).toHaveLength(1);expect(children.find(route=>route.name==='LabAdminLabManagement').meta.breadcrumb).toHaveLength(2);expect(capture.options.scrollBehavior()).toEqual({top:0})})
it('anonymous protected navigation redirects to login with full destination while public QR stays open',async()=>{auth.isAuthenticated.mockReturnValue(false);expect(await capture.guard(route({fullPath:'/lab-admin?tab=devices'}))).toEqual({path:'/login',query:{redirect:'/lab-admin?tab=devices'}});expect(await capture.guard(route({path:'/scan/reserve',name:'ScanReservation',matched:[{meta:{public:true}}],meta:{title:'扫码预约'}}))).toBe(true);expect(auth.refreshCurrentUserSession).not.toHaveBeenCalled();expect(document.title).toContain('扫码预约')})
it('authenticated protected routes force profile refresh but login redirects to role home',async()=>{expect(await capture.guard(route())).toBe(true);expect(auth.refreshCurrentUserSession).toHaveBeenCalledWith({force:true});auth.refreshCurrentUserSession.mockClear();expect(await capture.guard(route({path:'/login',name:'Login',matched:[{meta:{public:true}}],meta:{}}))).toBe('/lab-admin');expect(auth.refreshCurrentUserSession).not.toHaveBeenCalled()})
it('expired and suspended sessions are cleared and carry a useful login error',async()=>{auth.assertActiveSessionRole.mockImplementationOnce(()=>{throw {userMessage:'角色停用'}});expect(await capture.guard(route())).toEqual({path:'/login',query:{error:'角色停用'}});expect(auth.clearAuthSession).toHaveBeenCalled();expect(ElMessage.error).toHaveBeenCalledWith('角色停用');ElMessage.error.mockClear();auth.refreshCurrentUserSession.mockRejectedValueOnce({message:'过期',__handled:true});expect(await capture.guard(route())).toEqual({path:'/login',query:{error:'过期'}});expect(ElMessage.error).not.toHaveBeenCalled()})
it('role scopes cannot be crossed and forbidden page remains accessible for valid sessions',async()=>{expect(await capture.guard(route({meta:{roleScope:'systemAdmin'}}))).toBe('/forbidden');expect(await capture.guard(route({path:'/forbidden',name:'Forbidden',meta:{}}))).toBe(true);expect(await capture.guard(route({path:'/unknown',name:'Unknown',meta:{}}))).toBe('/lab-admin')})
it('custom menu permissions restrict subpages but never lock the role portal',async()=>{auth.getStoredUser.mockReturnValue({roleCode:'labAdmin',permissions:['实验室管理']});expect(await capture.guard(route({path:'/lab-admin/devices',name:'LabAdminDeviceManagement'}))).toBe('/forbidden');expect(await capture.guard(route({path:'/lab-admin/labs',name:'LabAdminLabManagement'}))).toBe(true);expect(await capture.guard(route())).toBe(true)})
it('server role grants deny direct page URLs even when personal permissions allow them',async()=>{
  auth.getStoredUser.mockReturnValue({roleCode:'labAdmin',permissions:['设备资产']})
  refreshPageAccess.mockResolvedValue({role:'labAdmin',accessiblePaths:['/lab-admin']})
  expect(await capture.guard(route({path:'/lab-admin/devices',name:'LabAdminDeviceManagement'}))).toBe('/forbidden')
})
it('unavailable server policy fails closed without clearing a valid login',async()=>{
  refreshPageAccess.mockRejectedValue(new Error('offline'))
  expect(await capture.guard(route({path:'/lab-admin/devices',fullPath:'/lab-admin/devices',name:'LabAdminDeviceManagement'})))
    .toEqual({path:'/permissions-unavailable',query:{redirect:'/lab-admin/devices'}})
  expect(auth.clearAuthSession).not.toHaveBeenCalled()
})
it('policy service failure never locks the role home or super-admin recovery entry',async()=>{
  refreshPageAccess.mockRejectedValue(new Error('service not yet started'))
  expect(await capture.guard(route())).toBe(true)
  auth.getStoredRole.mockReturnValue('systemAdmin')
  expect(await capture.guard(route({path:'/system-admin/role-access',name:'SystemAdminRolePageAccess',meta:{roleScope:'systemAdmin'}}))).toBe(true)
  expect(await capture.guard(route({path:'/permissions-unavailable',name:'PermissionsUnavailable',meta:{}}))).toBe(true)
})

