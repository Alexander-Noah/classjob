// @vitest-environment happy-dom
import { afterEach,it,expect,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Portal from '../../src/views/roles/SystemAdminPortal.vue'
import { fetchPlatformSnapshot } from '../../src/api/services/platformOverview'
import { fetchDashboardOverview } from '../../src/api/dashboard'
import http from '../../src/api/http'
import { panelStubs } from '../support/formStubs'
vi.mock('../../src/api/services/platformOverview',()=>({fetchPlatformSnapshot:vi.fn()}))
vi.mock('../../src/api/config',()=>({API_USE_MOCK:false}))
vi.mock('../../src/api/http',()=>({default:{get:vi.fn()}}))
const wrappers=[]
afterEach(()=>{wrappers.splice(0).forEach(w=>w.unmount());vi.clearAllMocks()})
it('system portal shows unavailable-source errors and reloads authoritative account totals',async()=>{fetchPlatformSnapshot.mockResolvedValueOnce({users:[],notices:[],errors:['用户接口不可用']}).mockResolvedValueOnce({users:[{id:1,username:'admin',realName:'管理员',roleCode:'systemAdmin',status:1}],notices:[],errors:[]});const w=shallowMount(Portal,{global:{stubs:panelStubs(),directives:{loading:()=>{}}}});wrappers.push(w);await flushPromises();expect(w.text()).toContain('用户接口不可用');await w.get('button').trigger('click');await flushPromises();expect(w.find('button').exists()).toBe(false);const config=w.findComponent({name:'RolePortalPage'}).props('config');expect(config.records[0]).toContain('admin');expect(w.vm.loading).toBe(false)})
it('dashboard adapter normalizes absent collections and removes duplicate lab shortcuts',async()=>{http.get.mockResolvedValueOnce({data:{data:{stats:[{value:2}],shortcuts:[{title:'实验室资源'},{title:'实验室管理'},{title:'设备资产',path:'/devices'}],labUsage:'bad'}}});const result=await fetchDashboardOverview();expect(result.stats).toEqual([{value:2}]);expect(result.shortcuts).toEqual([{title:'设备资产',path:'/devices'}]);expect(result.labUsage).toEqual([]);expect(result.notices).toEqual([]);http.get.mockResolvedValueOnce({data:{}});expect((await fetchDashboardOverview()).todaySchedules).toEqual([])})
