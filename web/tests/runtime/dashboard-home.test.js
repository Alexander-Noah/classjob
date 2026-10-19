import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/dashboard/DashboardHome.vue'
import { fetchDashboardOverview } from '../../src/api/dashboard'
import http from '../../src/api/http'
import { ElMessage } from 'element-plus'
const router=vi.hoisted(()=>({push:vi.fn()}))
vi.mock('vue-router',()=>({useRouter:()=>router}))
vi.mock('../../src/api/dashboard',()=>({fetchDashboardOverview:vi.fn()}))
vi.mock('../../src/api/http',()=>({default:{get:vi.fn()}}))
vi.mock('element-plus',()=>({ElMessage:{error:vi.fn()}}))
const wrappers=[]
it('rendered dashboard shows schedules, stock categories and routes detail shortcuts',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('程序设计');expect(w.text()).toContain('教师甲');expect(w.text()).toContain('网络');await w.findAll('button').find(b=>b.text()==='刷新工作台').trigger('click');await flushPromises();expect(fetchDashboardOverview).toHaveBeenCalledTimes(2);for(const button of w.findAll('button').filter(b=>b.text().includes('查看')))await button.trigger('click');expect(router.push).toHaveBeenCalled();vm.errorMessage='数据加载失败';await flushPromises();expect(w.text()).toContain('数据加载失败');await w.findAll('button').find(b=>b.text()==='重新加载').trigger('click');await flushPromises();expect(vm.errorMessage).toBe('')})
const overview=()=>({stats:[{label:'实验室',value:2,iconKey:'lab'},{value:1,iconKey:'unknown'}],labUsage:[{name:'A101',value:80}],deviceStatus:[],notices:[],doorAlerts:[{id:2,lab:'B202',time:'10:10-11:00',reason:'实验',applicant:'教师乙'}],todaySchedules:[{id:1,lab:'A101',time:'09:30-10:30',course:'程序设计',teacher:'教师甲',className:'软件一班'}],recentActivities:[]})
beforeEach(()=>{vi.clearAllMocks();vi.useFakeTimers({toFake:['Date','setInterval','clearInterval']});vi.setSystemTime(new Date(2026,9,18,10));fetchDashboardOverview.mockResolvedValue(overview());http.get.mockResolvedValue({data:[{id:1,name:'网线',category:'网络',stock:10,warnThreshold:5},{id:2,name:'电池',category:'电子',stock:1,warnThreshold:5}]})})
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.useRealTimers()})
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','icon','card','tag','empty','table','table-column','progress','timeline','timeline-item','alert','skeleton'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads overview and derives schedule, reminder and stock summaries from real data',async()=>{
  const vm=await setup();expect(vm.stats.map(stat=>stat.title)).toEqual(['实验室','实验室信息']);expect(vm.scheduleRows[0].course).toBe('程序设计');expect(vm.doorReminderView.totalActive).toBe(2);expect(vm.consumableSummary).toEqual({totalKinds:2,totalStock:11,warningCount:1,categoryCount:2});expect(vm.consumableCategoryRows.map(row=>row.category)).toEqual(['网络','电子']);expect(vm.maxConsumableStock).toBe(10);expect(vm.hiddenConsumableCategoryCount).toBe(0);expect(vm.generatedActivities.some(item=>item.id==='consumable-warning')).toBe(true);expect(vm.recentActivities.length).toBeGreaterThan(0);expect(vm.isLoading).toBe(false)
})
it('prefers backend activity timeline and normalizes descriptive status labels',async()=>{
  fetchDashboardOverview.mockResolvedValue({...overview(),recentActivities:[{name:'巡检',description:'检查设备',status:'正常',statusType:'success',category:'维护'}]});const vm=await setup();expect(vm.recentActivities).toHaveLength(1);expect(vm.recentActivities[0]).toMatchObject({title:'巡检',desc:'检查设备',type:'success',typeLabel:'维护'});expect(['warning','故障','已完成','预约','未知'].map(vm.normalizeActivityType)).toEqual(['warning','warning','success','primary','info']);expect(vm.normalizeActivity({})).toMatchObject({time:'刚刚',title:'实验室动态',desc:'暂无详情',type:'info'})
})
it('normalizes menu shortcuts and prevents empty navigation',async()=>{
  const vm=await setup();for(const [input,expected] of [['/repairs','/lab-admin/maintenance-records'],['/devices','/lab-admin/devices'],['/device-status','/lab-admin/device-status'],['/reservations','/lab-admin/reservations'],['/records','/lab-admin/records'],['/environments','/lab-admin/environments'],['/room-control','/lab-admin/room-control'],['/lab-admin/records','/lab-admin/records'],['/custom','/custom']]){vm.openPath(input);expect(router.push).toHaveBeenLastCalledWith(expected)}router.push.mockClear();vm.openPath('');expect(router.push).not.toHaveBeenCalled()
})
it('optional consumable failures do not break overview and primary failures retain prior data',async()=>{
  http.get.mockRejectedValueOnce(new Error('stock offline'));const vm=await setup();expect(vm.consumableRecords).toEqual([]);expect(vm.errorMessage).toBe('');fetchDashboardOverview.mockRejectedValueOnce(new Error('offline'));await vm.loadDashboardOverview();expect(vm.errorMessage).toContain('加载失败');expect(vm.dashboardData.stats).toHaveLength(2);expect(vm.isLoading).toBe(false);expect(ElMessage.error).toHaveBeenCalledTimes(1);fetchDashboardOverview.mockRejectedValueOnce({__handled:true});await vm.loadDashboardOverview();expect(ElMessage.error).toHaveBeenCalledTimes(1)
})
it('clock refreshes statuses and is released on unmount',async()=>{
  const vm=await setup();expect(vm.scheduleRows).toHaveLength(1);vi.setSystemTime(new Date(2026,9,18,12));vi.advanceTimersByTime(60000);expect(vm.scheduleRows).toEqual([]);expect(vm.doorReminderView.totalActive).toBe(0);expect(vi.getTimerCount()).toBe(1);wrappers[0].unmount();expect(vi.getTimerCount()).toBe(0)
})
it('stock response envelopes and safety bounds preserve category aggregates',async()=>{
  const vm=await setup();for(const payload of [[1],{data:[1]},{records:[1]},{data:{records:[1]}}])expect(vm.getResponseData(payload)).toEqual([1]);expect(vm.getResponseData({})).toEqual([]);expect(vm.normalizeConsumable({stock:-10})).toMatchObject({stock:0,status:'待补货'});vm.consumableRecords=Array.from({length:6},(_,index)=>vm.normalizeConsumable({id:index,category:`分类${index}`,stock:index}));expect(vm.visibleConsumableCategoryRows).toHaveLength(4);expect(vm.hiddenConsumableCategoryCount).toBe(2);expect(vm.consumableSummary.totalStock).toBe(15)
})

