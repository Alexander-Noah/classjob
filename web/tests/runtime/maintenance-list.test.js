import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { reactive } from 'vue'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/maintenance-records/MaintenanceRecordList.vue'
import * as api from '../../src/api/services/maintenanceRecords'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage,ElMessageBox } from 'element-plus'
const session=vi.hoisted(()=>({user:{roleCode:'labAdmin'}}));const navigation=vi.hoisted(()=>({replace:vi.fn()}));let route
vi.mock('vue-router',()=>({useRoute:()=>route,useRouter:()=>navigation}))
vi.mock('../../src/api/services/auth',()=>({getStoredUser:()=>session.user}))
vi.mock('../../src/api/services/maintenanceRecords',()=>({listMaintenanceRecords:vi.fn(),getMaintenanceRecord:vi.fn(),deleteMaintenanceRecord:vi.fn()}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn(options=>options.rows.length)}))
vi.mock('element-plus',()=>({ElMessage:{warning:vi.fn(),success:vi.fn(),error:vi.fn()},ElMessageBox:{confirm:vi.fn()}}))
const records=[{id:1,recordNo:'MR1',lab:'A101',device:'电脑',maintenanceType:'巡检',maintainer:'教师甲',maintenanceDate:'2026-10-18',content:'检查运行',result:'正常'},{id:2,recordNo:'MR2',lab:'B202',device:'投影仪',maintenanceType:'维修',maintainer:'教师乙',maintenanceDate:'2026-10-17',content:'更换灯泡',result:'完成'}]
const wrappers=[]
it('rendered maintenance list opens details and forwards editor lifecycle events',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('MR1');const click=async label=>{await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()};await click('详情');expect(vm.detailVisible).toBe(true);w.findComponent({name:'MaintenanceRecordDetail'}).vm.$emit('update:modelValue',false);await click('编辑');expect(vm.editorVisible).toBe(true);w.findComponent({name:'MaintenanceRecordForm'}).vm.$emit('saving-change',true);expect(vm.formSaving).toBe(true);w.findComponent({name:'MaintenanceRecordForm'}).vm.$emit('saving-change',false);w.findComponent({name:'MaintenanceRecordForm'}).vm.$emit('saved',{...records[0],content:'已更新'});await flushPromises();expect(vm.editorVisible).toBe(false);w.findComponent({name:'MaintenanceRecordFilters'}).vm.$emit('reset');expect(vm.filters.keyword).toBe('')})
beforeEach(()=>{vi.clearAllMocks();session.user={roleCode:'labAdmin'};route=reactive({query:{}});api.listMaintenanceRecords.mockResolvedValue(records.map(record=>({...record})));api.getMaintenanceRecord.mockImplementation(async id=>records.find(row=>String(row.id)===String(id)));api.deleteMaintenanceRecord.mockResolvedValue({});ElMessageBox.confirm.mockResolvedValue(true)})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','icon','card','tag','empty','table','table-column','dialog','alert','drawer'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads records, unique filter options and semantic filtered export rows',async()=>{
  const vm=await setup();expect(vm.canWrite).toBe(true);expect(vm.filterOptions).toEqual({labs:['A101','B202'],devices:['电脑','投影仪'],types:['巡检','维修'],maintainers:['教师甲','教师乙']});vm.filters.lab='A101';expect(vm.filteredRecords.map(row=>row.id)).toEqual([1]);vm.exportRecords();expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({filename:'维护记录',rows:[expect.objectContaining({recordNo:'MR1',lab:'A101'})]}));vm.resetFilters();expect(vm.filteredRecords).toHaveLength(2);vm.filters.keyword='missing';vm.exportRecords();expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的维护记录')
})
it('out-of-order loads cannot replace newer records or clear current loading state',async()=>{
  const vm=await setup();let resolveOld,resolveNew;api.listMaintenanceRecords.mockImplementationOnce(()=>new Promise(resolve=>resolveOld=resolve)).mockImplementationOnce(()=>new Promise(resolve=>resolveNew=resolve));const old=vm.loadRecords(),latest=vm.loadRecords();resolveOld([{id:9}]);await old;expect(vm.loading).toBe(true);resolveNew([records[1]]);await latest;expect(vm.records.map(row=>row.id)).toEqual([2]);expect(vm.loading).toBe(false)
})
it('detail and edit retrieve canonical records and prevent simultaneous actions',async()=>{
  const vm=await setup();await vm.openDetail(records[0]);expect(vm.detailVisible).toBe(true);expect(vm.selectedRecord.id).toBe(1);await vm.editRecord(records[0]);expect(vm.editorVisible).toBe(true);expect(vm.detailVisible).toBe(false);expect(vm.editorRecord.id).toBe(1);vm.actionId='busy';api.getMaintenanceRecord.mockClear();await vm.openDetail(records[1]);await vm.editRecord(records[1]);expect(api.getMaintenanceRecord).not.toHaveBeenCalled();vm.actionId='';vm.openCreate();expect(vm.editorRecord).toEqual({})
})
it('dirty editor respects cancellation, saving guard and explicit close callbacks',async()=>{
  const vm=await setup();vm.openCreate();vm.editorRef={isDirty:()=>true};ElMessageBox.confirm.mockRejectedValueOnce('cancel');await vm.closeEditor();expect(vm.editorVisible).toBe(true);vm.formSaving=true;await vm.closeEditor();expect(vm.editorVisible).toBe(true);vm.formSaving=false;const done=vi.fn();await vm.closeEditor(done);expect(done).toHaveBeenCalled();await vm.closeEditor();expect(vm.editorVisible).toBe(false)
})
it('record save refreshes authoritative data and closes editor even if refresh fails',async()=>{
  const vm=await setup();vm.editorVisible=true;vm.formSaving=true;await vm.recordSaved({...records[0],content:'updated'});expect(vm.formSaving).toBe(false);expect(vm.editorVisible).toBe(false);api.listMaintenanceRecords.mockRejectedValueOnce(new Error('offline'));await vm.recordSaved({...records[0],id:3});expect(vm.records[0].id).toBe(3);expect(vm.loadError).toContain('加载失败')
})
it('deletion cancellation has no write and successful removal closes matching detail',async()=>{
  const vm=await setup();await vm.openDetail(records[0]);ElMessageBox.confirm.mockRejectedValueOnce('cancel');await vm.removeRecord(records[0]);expect(api.deleteMaintenanceRecord).not.toHaveBeenCalled();api.listMaintenanceRecords.mockResolvedValueOnce([records[1]]);await vm.removeRecord(records[0]);expect(api.deleteMaintenanceRecord).toHaveBeenCalledWith('1');expect(vm.detailVisible).toBe(false);expect(vm.actionId).toBe('');expect(vm.records).toEqual([records[1]])
})
it('read-only sessions cannot create, edit or delete but legacy query is cleaned',async()=>{
  session.user={roleCode:'academic'};route.query={create:'1',keep:'yes'};const vm=await setup();expect(vm.canWrite).toBe(false);expect(vm.editorVisible).toBe(false);expect(navigation.replace).toHaveBeenCalledWith({path:'/lab-admin/maintenance-records',query:{keep:'yes'}});await vm.editRecord(records[0]);await vm.removeRecord(records[0]);vm.openCreate();expect(api.deleteMaintenanceRecord).not.toHaveBeenCalled();expect(api.getMaintenanceRecord).not.toHaveBeenCalled()
})
it('legacy edit entry opens selected record and server errors release action lock',async()=>{
  route.query={edit:'1'};const vm=await setup();expect(vm.editorRecord.id).toBe(1);expect(vm.editorVisible).toBe(true);api.getMaintenanceRecord.mockRejectedValueOnce({userMessage:'无法读取'});await vm.openDetail(records[1]);expect(ElMessage.error).toHaveBeenCalledWith('无法读取');expect(vm.actionId).toBe('');ElMessage.error.mockClear();vm.showError({__handled:true},'fallback');expect(ElMessage.error).not.toHaveBeenCalled();api.deleteMaintenanceRecord.mockRejectedValueOnce(new Error('offline'));await vm.removeRecord(records[0]);expect(vm.actionId).toBe('')
})


