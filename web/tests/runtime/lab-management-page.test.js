// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { ref } from 'vue'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/labs/LabManagement.vue'
import { getLabDevices } from '../../src/api/services/lab'
import { reservationResourceService } from '../../src/api/services/basicResources'
import * as timetable from '../../src/api/services/classTimetable'
let state
const router=vi.hoisted(()=>({resolve:vi.fn(()=>({href:'/scan/reserve?labId=1&code=A101'}))}))
vi.mock('vue-router',()=>({useRouter:()=>router}))
vi.mock('../../src/api/services/lab',()=>({getLabDevices:vi.fn()}))
vi.mock('../../src/api/services/basicResources',()=>({reservationResourceService:{list:vi.fn()}}))
vi.mock('../../src/api/services/classTimetable',()=>({getClassTimetableCrawlerConfig:vi.fn(),getClassTimetableSemesters:vi.fn(),listClassTimetables:vi.fn()}))
vi.mock('../../src/composables/useLabManagement',()=>({useLabManagement:()=>state}))
const wrappers=[]
beforeEach(()=>{vi.clearAllMocks();vi.useFakeTimers({toFake:['Date','setInterval','clearInterval']});vi.setSystemTime(new Date(2026,9,19,9));state=Object.fromEntries(['availableOnly','buildingFilter','dialogTitle','dialogVisible','formSubmitting','keyword','selectedLabId','statusFilter','typeFilter'].map(key=>[key,ref('')]));Object.assign(state,Object.fromEntries(['buildingOptions','filteredLabs','managerOptions','safetyOptions','statusOptions','statusSummary','typeOptions'].map(key=>[key,ref([])])),{formModel:{},selectedLab:ref(null)},Object.fromEntries(['bindLabSchedules','changeLabStatus','deleteLab','exportLabs','openCreateDialog','openEditDialog','selectLab','submitLab'].map(key=>[key,vi.fn()])));timetable.getClassTimetableCrawlerConfig.mockResolvedValue({semesterStartDate:'2026-09-07'});timetable.getClassTimetableSemesters.mockResolvedValue(['2026秋']);timetable.listClassTimetables.mockResolvedValue([{id:1}]);reservationResourceService.list.mockResolvedValue([{id:2}]);getLabDevices.mockResolvedValue([{id:3,deviceCode:'PC1',deviceName:'电脑',ownerName:'教师甲',inventory_date:'2026-10-18',online:1}])})
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.useRealTimers()})
async function setup(){const wrapper=shallowMount(Page,{global:{stubs:{'el-dialog':true,'el-button':true}}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads semester context and binds authoritative timetable and reservation rows',async()=>{const vm=await setup();expect(vm.scheduleFilters).toMatchObject({semester:'2026秋',week:7,weekday:1});expect(vm.activeTimeUnit).toBe(1);expect(vm.todayText).toBe('2026-10-19');expect(vm.isCurrentScheduleDay).toBe(true);expect(state.bindLabSchedules).toHaveBeenCalledWith([{id:1}],expect.objectContaining({date:'2026-10-19',week:7,isToday:true}),[{id:2}]);expect(vm.scheduleLoading).toBe(false)})
it('lab selection loads normalized asset details and handles empty or failed results',async()=>{const vm=await setup();const lab={id:1,code:'A101'};await vm.handleSelectLab(lab);expect(state.selectLab).toHaveBeenCalledWith(lab);expect(vm.labAssets[0]).toMatchObject({code:'PC1',name:'电脑',owner:'教师甲',inventoryDate:'2026-10-18',online:true});await vm.handleViewLabAssets(lab);expect(vm.assetDialogVisible).toBe(true);expect(vm.assetDialogLab).toEqual(lab);await vm.loadLabAssets(null);expect(vm.labAssets).toEqual([]);const error=vi.spyOn(console,'error').mockImplementation(()=>{});getLabDevices.mockRejectedValueOnce(new Error('offline'));await vm.loadLabAssets(lab);expect(vm.labAssets).toEqual([]);error.mockRestore()})
it('QR links resolve through router and opening an empty lab does nothing',async()=>{const vm=await setup();expect(vm.reservationQrLink).toBe('');vm.handleOpenReservationQr(null);expect(vm.qrDialogVisible).toBe(false);vm.handleOpenReservationQr({id:1,code:'A101'});expect(vm.qrDialogVisible).toBe(true);expect(vm.reservationQrLink).toContain('/scan/reserve?labId=1&code=A101');expect(router.resolve).toHaveBeenCalledWith({path:'/scan/reserve',query:{labId:1,code:'A101'}})})
it('filter and clock changes rebind availability and unmount releases clock',async()=>{const vm=await setup();state.bindLabSchedules.mockClear();vm.scheduleFilters.section=2;await flushPromises();expect(state.bindLabSchedules).toHaveBeenCalled();vi.setSystemTime(new Date(2026,9,19,12));vi.advanceTimersByTime(60000);await flushPromises();expect(vm.activeTimeUnit).toBe(0);expect(vi.getTimerCount()).toBe(1);wrappers[0].unmount();expect(vi.getTimerCount()).toBe(0)})
it('failed schedule refresh releases loading and optional semester failure clears options',async()=>{const vm=await setup();timetable.listClassTimetables.mockRejectedValueOnce(new Error('offline'));await expect(vm.loadScheduleRows()).rejects.toThrow('offline');expect(vm.scheduleLoading).toBe(false);timetable.getClassTimetableSemesters.mockRejectedValueOnce(new Error('offline'));await vm.loadSemesters();expect(vm.semesters).toEqual([]);reservationResourceService.list.mockResolvedValueOnce({});await vm.loadScheduleRows();expect(vm.reservationRows).toEqual([])})
it('sparse imported assets retain valid aliases and malformed collection responses clear previous rows',async()=>{
 const vm=await setup();getLabDevices.mockResolvedValueOnce([{assetNo:'ASSET',ownerUsername:'legacy'},{}])
 await vm.loadLabAssets({id:1});expect(vm.labAssets).toEqual([
  {assetNo:'ASSET',ownerUsername:'legacy',code:'ASSET',name:'',owner:'legacy',inventoryDate:'',online:false},
  {code:'',name:'',owner:'',inventoryDate:'',online:false},
 ])
 getLabDevices.mockResolvedValueOnce({data:[]});await vm.loadLabAssets({id:1});expect(vm.labAssets).toEqual([])
 await vm.handleViewLabAssets(null);expect(vm.assetDialogVisible).toBe(false)
})
it('missing calendar and semester data keep Sunday and optional filters well defined',async()=>{
 vi.setSystemTime(new Date(2026,9,18,9));timetable.getClassTimetableCrawlerConfig.mockResolvedValueOnce(null)
 timetable.getClassTimetableSemesters.mockResolvedValueOnce([])
 const vm=await setup();expect(vm.currentWeekday()).toBe(7);expect(vm.semesterStartDate).toBe('')
 vm.scheduleFilters.week=0;await vm.loadScheduleRows()
 expect(timetable.listClassTimetables).toHaveBeenLastCalledWith(expect.objectContaining({semester:undefined,week:undefined}))
 expect(vm.scheduleLoading).toBe(false)
})
