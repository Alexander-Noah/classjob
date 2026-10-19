// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import { panelStubs } from '../support/formStubs'
import Page from '../../src/views/roles/components/RolePortalPage.vue'
import { labResourceService,reservationResourceService } from '../../src/api/services/basicResources'
import { deviceAssetService } from '../../src/api/services/deviceAssets'
import { listMaintenanceRecords } from '../../src/api/services/maintenanceRecords'
import { listClassTimetables } from '../../src/api/services/classTimetable'
import { ElMessage } from 'element-plus'
vi.mock('../../src/api/services/basicResources',()=>({labResourceService:{list:vi.fn(),update:vi.fn()},reservationResourceService:{list:vi.fn()}}))
vi.mock('../../src/api/services/deviceAssets',()=>({deviceAssetService:{list:vi.fn()}}))
vi.mock('../../src/api/services/maintenanceRecords',()=>({listMaintenanceRecords:vi.fn()}))
vi.mock('../../src/api/services/classTimetable',()=>({listClassTimetables:vi.fn()}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),error:vi.fn()}}))
const labs=[{id:1,labCode:'A101',labName:'开发机房',floor:'1F',capacity:20,status:'使用中'},{id:2,code:'B202',name:'网络机房',capacity:10,status:'开放'}]
const devices=[{id:1,labName:'A101 开发机房',status:'online'},{id:2,lab:'A101',status:'offline'},{id:3,lab:'B202',status:'正常'}]
const wrappers=[]
it('standard role portal displays configured statistics, actions and tabular status',async()=>{const config={digitalTwinOnly:false,badge:'管理中心',title:'资源工作台',stats:[{label:'实验室',value:2,trend:'运行中'}],actions:[{title:'设备管理',tag:'资产'}],recordsTitle:'最新记录',columns:['名称','状态'],records:[['甲机房','正常']]};const w=shallowMount(Page,{props:{config},global:{stubs:panelStubs()}});wrappers.push(w);await flushPromises();expect(w.text()).toContain('资源工作台');expect(w.text()).toContain('运行中');expect(w.text()).toContain('设备管理');expect(w.text()).toContain('甲机房');expect(w.text()).toContain('正常')})
beforeEach(()=>{vi.clearAllMocks();vi.useFakeTimers({toFake:['Date']});vi.setSystemTime(new Date(2026,9,18,10));labResourceService.list.mockResolvedValue(labs);deviceAssetService.list.mockResolvedValue(devices);reservationResourceService.list.mockResolvedValue([{id:1,lab:'A101',date:'2026-10-18',status:'已通过'},{id:2,lab:'A101',date:'2026-10-18',status:'待审核'}]);listMaintenanceRecords.mockResolvedValue([{id:1,lab:'A101',deviceName:'教学主机',result:'已完成'}]);listClassTimetables.mockResolvedValue([{id:1}]);labResourceService.update.mockResolvedValue({})})
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.useRealTimers()})
async function setup(overrides={}){const config={digitalTwinOnly:true,digitalTwin:{labs:[],summary:[],events:[],sceneTags:['系统总览','设备'],...overrides.digitalTwin},...overrides};const wrapper=shallowMount(Page,{props:{config},global:{stubs:{'el-icon':true,'el-button':true,'el-tag':true}}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('aggregates real assets, reservations, maintenance and schedule summaries',async()=>{
  const vm=await setup();expect(vm.liveSummary.map(item=>item.value)).toEqual(['2','3','1','1']);expect(vm.liveLabNodes[0]).toMatchObject({code:'A101',usage:88,color:'#f59e0b'});expect(vm.selectedLabDevices).toHaveLength(2);expect(vm.selectedLabReservations).toHaveLength(2);expect(vm.selectedLabMaintenanceRecords).toHaveLength(1);expect(vm.selectedLabPanel.rows.map(row=>row.value)).toEqual(['2 台','1 台','2 条','1 条']);expect(vm.liveEvents.map(event=>event.title)).toEqual(['教学主机','A101 正在使用','设备在线数据已同步']);expect(vm.twinLoading).toBe(false)
})
it('lab matching requires token boundaries while supporting normalized names',async()=>{
  const vm=await setup();expect(vm.isLabMatched('楼宇 A101 教室',labs[0])).toBe(true);expect(vm.isLabMatched('A1010',labs[0])).toBe(false);expect(vm.isLabMatched('开发 机房',labs[0])).toBe(true);expect(vm.isLabMatched('',labs[0])).toBe(false);expect(vm.hasLabToken('A[1]','A[1]')).toBe(true);expect(vm.hasLabToken('A101','')).toBe(false)
  expect(['offline','报废','停用','离线','online'].map(status=>vm.isDeviceOnline({status}))).toEqual([false,false,false,false,true])
})
it('focuses labs, modules and hotspots and bounds the operation history',async()=>{
  const vm=await setup();vm.selectLab(vm.liveLabNodes[1]);expect(vm.selectedLab.code).toBe('B202');expect(vm.selectedLabPanel.title).toContain('B202');vm.selectHotspot('设备机房');vm.selectModule('设备');for(let index=0;index<5;index++)vm.selectModule(`模块${index}`);expect(vm.operationLog).toHaveLength(4);expect(vm.operationLog[0]).toBe('切换至 模块4')
})
it('updates lab state optimistically and rolls back rejected persistence',async()=>{
  const vm=await setup();await vm.updateSelectedLabStatus('维护中');expect(labResourceService.update).toHaveBeenCalledWith(1,expect.objectContaining({status:'维护中',openStatus:'维护中'}));expect(vm.selectedLab.status).toBe('维护中');expect(vm.selectedLab.usage).toBe(12);labResourceService.update.mockRejectedValueOnce(new Error('offline'));await vm.updateSelectedLabStatus('开放');expect(vm.selectedLab.status).toBe('维护中');expect(ElMessage.error).toHaveBeenCalledWith('offline')
})
it('optional history and timetable failures yield empty collections without losing primary data',async()=>{
  listMaintenanceRecords.mockRejectedValue(new Error('offline'));listClassTimetables.mockRejectedValue(new Error('offline'));const vm=await setup();expect(vm.liveLabs).toHaveLength(2);expect(vm.liveMaintenanceRecords).toEqual([]);expect(vm.liveTimetables).toEqual([])
})
it('empty API results retain configured digital twin defaults and allow no-lab operations',async()=>{
  labResourceService.list.mockResolvedValue([]);deviceAssetService.list.mockResolvedValue([]);reservationResourceService.list.mockResolvedValue(null);listMaintenanceRecords.mockResolvedValue(null);listClassTimetables.mockResolvedValue(null);const summary=[{label:'默认',value:0}],events=[{title:'默认事件'}];const vm=await setup({digitalTwin:{labs:[],summary,events,sceneTags:[]}});expect(vm.liveSummary).toEqual(summary);expect(vm.liveEvents).toEqual(events);expect(vm.selectedLabPanel.rows).toEqual([]);expect(vm.selectedLabDevices).toEqual([]);await vm.updateSelectedLabStatus('开放');expect(labResourceService.update).not.toHaveBeenCalled()
})
it('status appearance and capacity usage have stable bounded semantics',async()=>{
  const vm=await setup();expect(['开放','使用中','维护中','停用','未知'].map(vm.getStatusColor)).toEqual(['#22c55e','#f59e0b','#ef4444','#ef4444','#3b82f6']);expect(vm.getUsageByStatus('预约中',20,1)).toBe(64);expect(vm.getUsageByStatus('开放',100,1)).toBe(18);expect(vm.getUsageByStatus('开放',10,100)).toBe(58);expect(vm.getUsageByStatus('开放',0,0)).toBe(30)
})
