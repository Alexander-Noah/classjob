import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/device-status/DeviceStatusPage.vue'
import { labResourceService } from '../../src/api/services/basicResources'
import { listDeviceStatusEvents,listLabRuntimeStatus } from '../../src/api/services/deviceStatus'
import { bindActivationCodeToLab,listLabActivationBindings,unbindActivationCodeFromLab } from '../../src/api/services/deviceActivation'
import { ElMessage,ElMessageBox } from 'element-plus'
vi.mock('../../src/api/services/basicResources',()=>({labResourceService:{list:vi.fn()}}))
vi.mock('../../src/api/services/deviceStatus',()=>({listDeviceStatusEvents:vi.fn(),listLabRuntimeStatus:vi.fn()}))
vi.mock('../../src/api/services/deviceActivation',()=>({bindActivationCodeToLab:vi.fn(),listLabActivationBindings:vi.fn(),unbindActivationCodeFromLab:vi.fn()}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),warning:vi.fn(),error:vi.fn()},ElMessageBox:{confirm:vi.fn()}}))
const wrappers=[]
it('rendered lab navigation and missing host telemetry use readable fallback identities',async()=>{
 listLabRuntimeStatus.mockResolvedValue({devices:[{labId:1,terminalId:'TERM-1'},{labId:1,macAddress:'cc:dd'},{labId:1},{labId:2,hostName:'二号主机'}]})
 listLabActivationBindings.mockResolvedValue([{labId:1,status:'inactive'},{status:'active'},binding()])
 const vm=await setup(true),w=wrappers.at(-1)
 expect(vm.currentLabDevices).toHaveLength(3)
 await w.findAll('.lab-item')[1].trigger('click');await flushPromises()
 expect(vm.selectedLab.id).toBe(2);expect(w.text()).toContain('二号主机')
 expect(vm.selectedLabActivation).toBeUndefined()
 listLabRuntimeStatus.mockResolvedValueOnce({devices:[{labId:2},{labId:2,name:'仅名称'},{labId:2,terminalId:'TERM-2'}]})
 await vm.loadRuntimeStatus(2);await flushPromises()
 expect(w.text()).toContain('未命名主机');expect(w.text()).toContain('TERM-2')
})
it('empty and unavailable lab sources never retain stale selection or loading flags',async()=>{
 labResourceService.list.mockResolvedValueOnce([])
 const vm=await setup();expect(vm.selectedLabId).toBe('');expect(vm.currentLabDevices).toEqual([])
 labResourceService.list.mockRejectedValueOnce({});await vm.loadData()
 expect(ElMessage.warning).toHaveBeenCalledWith('设备运行状态数据读取失败');expect(vm.loading).toBe(false)
})
it('rendered terminal view displays runtime health and binds lab activation controls',async()=>{listLabActivationBindings.mockResolvedValue([binding()]);const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('主机一');expect(w.text()).toContain('待连接主机');await w.get('input[placeholder="搜索实验室名称、编号、楼层"]').setValue('A101');expect(vm.visibleLabs).toHaveLength(1);await w.get('input[placeholder="搜索主机名、IP、MAC、终端"]').setValue('PC-1');expect(vm.selectedLabDevices.length).toBeGreaterThan(0);await w.findAll('button').find(b=>b.text()==='解除实验室接入授权').trigger('click');await flushPromises();vm.activationCodes=[];await flushPromises();await w.get('input[placeholder^="输入实验室接入码"]').setValue('NEW-CODE');expect(vm.labActivationCode).toBe('NEW-CODE');const button=w.findAll('button').find(b=>b.text()==='启用当前实验室接入授权');await button.trigger('click');await flushPromises();expect(bindActivationCodeToLab).toHaveBeenCalled();await w.findAll('button').find(b=>b.text()==='解除实验室接入授权').trigger('click');await flushPromises();expect(unbindActivationCodeFromLab).toHaveBeenCalled()})
const labs=[{id:1,labCode:'A101',labName:'机房',roomNo:'101',openStatus:'开放',capacity:'30'},{id:2,code:'B202',name:'实验室'}]
const devices=[{id:1,labId:1,terminalId:'PC-1',hostName:'主机一',deviceCode:'DEV1',macAddress:'aa:bb',ipAddress:'10.0.0.1',runtimeStatus:'online',onlineStatus:'online',health:'good',cpuUsage:15,memoryUsage:30,diskUsage:50},{id:2,labId:1,terminalId:'PC-2',runtimeStatus:'abnormal',onlineStatus:'offline',health:'abnormal'},{id:3,labId:2,terminalId:'other',onlineStatus:'online'}]
const binding=()=>({code:'CODE-123456789',labId:1,status:'active',deviceLimit:4,boundCount:2,boundDevices:[{terminalId:'PC-1',hostName:'主机一'},{terminalId:'unreported',hostName:'待连接主机'}]})
beforeEach(()=>{vi.clearAllMocks();labResourceService.list.mockResolvedValue(labs);listLabRuntimeStatus.mockResolvedValue({devices});listDeviceStatusEvents.mockResolvedValue({records:[{id:1,status:'online'}]});listLabActivationBindings.mockResolvedValue([]);bindActivationCodeToLab.mockResolvedValue(binding());unbindActivationCodeFromLab.mockResolvedValue({});ElMessageBox.confirm.mockResolvedValue(true)})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','icon','card','input','table','table-column','tag','empty','alert','descriptions','descriptions-item','progress','tooltip','divider','scrollbar'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads normalized lab terminal metrics and scopes devices by authoritative lab ID',async()=>{
  const vm=await setup();expect(vm.selectedLab).toMatchObject({code:'A101',name:'机房',capacity:30});expect(vm.currentLabDevices).toHaveLength(2);expect(vm.deviceStats.map(item=>item.value)).toEqual([2,1,1,1]);expect(vm.devices[0]).toMatchObject({name:'主机一',status:'正常',health:'良好',cpuUsage:'15%',memoryUsage:'30%',diskUsage:'50%'});expect(vm.statusEvents).toHaveLength(1)
  vm.labKeyword='b202';expect(vm.visibleLabs.map(lab=>lab.id)).toEqual([2]);vm.deviceKeyword='10.0.0.1';expect(vm.selectedLabDevices.map(row=>row.id)).toEqual([1]);expect(vm.maskActivationCode('')).toBe('-');expect(vm.maskActivationCode('ABCDEF')).toBe('AB****EF');expect(vm.maskActivationCode('123456789012')).toBe('1234****9012')
})
it('joins bound terminal keys to runtime snapshots and retains unreported terminals',async()=>{
  listLabActivationBindings.mockResolvedValue([binding()]);const vm=await setup();expect(vm.selectedLabActivationQuota).toEqual({bound:2,limit:4,remaining:2});expect(vm.activationDeviceStatusRows).toHaveLength(2);expect(vm.activationDeviceStatusRows[0]).toMatchObject({online:true,ipAddress:'10.0.0.1',status:'正常'});expect(vm.activationDeviceStatusRows[1]).toMatchObject({online:false,status:'待上报',source:'接入授权记录'});vm.deviceKeyword='待连接';expect(vm.displayedDeviceStatusRows).toHaveLength(1)
  vm.activationCodes=[{...binding(),boundDevices:[]}];vm.deviceKeyword='';expect(vm.activationDeviceStatusRows).toHaveLength(2);vm.activationCodes[0].boundCount=6;expect(vm.selectedLabActivationQuota.remaining).toBe(0)
})
it('lab selection clears keyword and handles runtime failures without stale terminals',async()=>{
  const vm=await setup();vm.deviceKeyword='old';vm.selectLab(vm.labs[1]);await flushPromises();expect(vm.deviceKeyword).toBe('');expect(listLabRuntimeStatus).toHaveBeenLastCalledWith(2);expect(vm.selectedLabDevices).toHaveLength(1)
  listLabRuntimeStatus.mockRejectedValueOnce(new Error('offline'));vm.selectLab(vm.labs[0]);await flushPromises();expect(vm.devices).toEqual([]);expect(vm.statusEvents).toEqual([]);expect(ElMessage.error).toHaveBeenCalledWith('offline');await vm.loadRuntimeStatus('');expect(vm.devices).toEqual([])
})
it('optional event and activation endpoint failures do not discard runtime data',async()=>{
  listLabActivationBindings.mockRejectedValue(new Error('offline'));listDeviceStatusEvents.mockRejectedValue(new Error('offline'));const vm=await setup();expect(vm.devices).toHaveLength(3);expect(vm.activationCodes).toEqual([]);expect(vm.statusEvents).toEqual([])
})
it('primary loading failures show fallback labs and always release loading state',async()=>{
  labResourceService.list.mockRejectedValue(new Error('offline'));const vm=await setup();expect(vm.labs).toHaveLength(3);expect(vm.devices).toEqual([]);expect(vm.loading).toBe(false);expect(ElMessage.warning).toHaveBeenCalledWith('offline')
})
it('binding validates lab, code and existing activation before posting',async()=>{
  const vm=await setup();await vm.bindActivationCode();expect(bindActivationCodeToLab).not.toHaveBeenCalled();vm.labActivationCode=' CODE ';vm.activationCodes=[binding()];await vm.bindActivationCode();expect(bindActivationCodeToLab).not.toHaveBeenCalled();vm.activationCodes=[];listLabActivationBindings.mockResolvedValueOnce([binding()]);await vm.bindActivationCode();expect(bindActivationCodeToLab).toHaveBeenCalledWith({code:'CODE',labId:1,labCode:'A101',labName:'机房'});expect(vm.labActivationCode).toBe('');expect(vm.bindingActivation).toBe(false)
  vm.labs=[];await vm.bindActivationCode();expect(ElMessage.warning).toHaveBeenCalledWith('请先选择实验室')
})
it('binding errors preserve input and list refresh failure retains returned binding',async()=>{
  const vm=await setup();vm.labActivationCode='CODE';bindActivationCodeToLab.mockRejectedValueOnce(new Error('invalid'));await vm.bindActivationCode();expect(vm.labActivationCode).toBe('CODE');expect(vm.bindingActivation).toBe(false);expect(ElMessage.error).toHaveBeenCalledWith('invalid');listLabActivationBindings.mockRejectedValueOnce(new Error('offline'));await vm.bindActivationCode();expect(vm.activationCodes).toEqual([binding()])
})
it('unbind honors cancellation, reports failures and refreshes successful results',async()=>{
  listLabActivationBindings.mockResolvedValue([binding()]);const vm=await setup();ElMessageBox.confirm.mockRejectedValueOnce('cancel');await vm.unbindActivationCode();expect(unbindActivationCodeFromLab).not.toHaveBeenCalled();unbindActivationCodeFromLab.mockRejectedValueOnce(new Error('offline'));await vm.unbindActivationCode();expect(ElMessage.error).toHaveBeenCalledWith('offline');expect(vm.unbindingActivation).toBe(false);listLabActivationBindings.mockResolvedValueOnce([]);await vm.unbindActivationCode();expect(unbindActivationCodeFromLab).toHaveBeenCalledWith(binding().code);expect(vm.selectedLabActivation).toBeUndefined();await vm.unbindActivationCode();expect(vm.unbindingActivation).toBe(false)
})
it('normalizes response envelopes and fallback telemetry fields',async()=>{
  const vm=await setup();for(const data of [devices,{devices},{records:devices},{data:{devices}},{data:{records:devices}}])expect(vm.normalizeRuntimeList(data)).toEqual(devices);for(const data of [devices,{records:devices},{data:{records:devices}}])expect(vm.normalizeEventList(data)).toEqual(devices);expect(vm.normalizeRuntimeList({})).toEqual([]);expect(vm.normalizeEventList({})).toEqual([])
  expect(vm.normalizeDevice({onlineStatus:'offline',terminalId:'T1',cpuUsage:'unknown'})).toMatchObject({code:'T1',status:'离线',online:false,health:'未知',cpuUsage:'unknown'});expect(vm.formatPercent(null)).toBe('');expect(['正常','维护中','离线','停用','自定义'].map(vm.getStatusType)).toEqual(['success','warning','danger','info','primary'])
})




it('matches legacy bound-device identities and safely renders missing terminal telemetry',async()=>{
 listLabActivationBindings.mockResolvedValue([{labId:1,status:'active',boundDevices:[{macAddress:'aa:bb'},{deviceCode:'DEV1'},{code:'DEV1'},{hostName:'主机一'},{}]}]);const vm=await setup(true)
 expect(vm.activationDeviceStatusRows.slice(0,4).every(d=>d.online)).toBe(true);expect(vm.activationDeviceStatusRows[4].status).toBe('待上报');vm.deviceKeyword='no-match';expect(vm.displayedDeviceStatusRows).toEqual([])
 vm.activationCodes=[{status:'active',labId:1,boundDevices:{}}];expect(vm.activationBoundDevices).toEqual([]);vm.activationCodes=[];expect(vm.activationDeviceStatusRows).toEqual([]);vm.labs=[];expect(vm.currentLabDevices).toEqual([]);expect(vm.selectedLab).toBeNull();expect(vm.selectedLabActivationQuota).toEqual({bound:0,limit:0,remaining:0})
 expect(vm.normalizeLab({room:'R1'})).toMatchObject({code:'R1',name:'R1'});expect(vm.normalizeLab({roomNo:'R2'}).code).toBe('R2');expect(vm.normalizeLab({}).code).toBe('')
 expect(vm.normalizeDevice({assetNo:'AS1',deviceName:'设备',online:false,runtimeStatus:'custom',health:'custom',createdAt:'2026',sourceType:'agent'})).toMatchObject({code:'AS1',name:'设备',status:'custom',health:'custom',online:false,activatedAt:'2026',source:'agent'});expect(vm.normalizeDevice({macAddress:'MAC'})).toMatchObject({code:'MAC',name:'未命名主机'});expect(vm.formatPercent(undefined)).toBe('');expect(vm.formatPercent('')).toBe('')
})
it('falls back on malformed optional bindings and supplies default error messages',async()=>{
 listLabActivationBindings.mockResolvedValue({});const vm=await setup();expect(vm.activationCodes).toEqual([]);listLabRuntimeStatus.mockRejectedValueOnce({});vm.selectLab(vm.labs[0]);await flushPromises();expect(ElMessage.error).toHaveBeenCalledWith('当前实验室设备运行状态读取失败')
 vm.labActivationCode='CODE';bindActivationCodeToLab.mockRejectedValueOnce({});await vm.bindActivationCode();expect(ElMessage.error).toHaveBeenCalledWith('实验室接入授权启用失败');vm.activationCodes=[binding()];unbindActivationCodeFromLab.mockRejectedValueOnce({});await vm.unbindActivationCode();expect(ElMessage.error).toHaveBeenCalledWith('实验室接入授权解除失败')
})
