import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/teacher/TeacherAdjustments.vue'
import * as api from '../../src/api/services/scheduleAdjustments'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage } from 'element-plus'
const route=vi.hoisted(()=>({query:{}}));const router=vi.hoisted(()=>({push:vi.fn()}))
const identity=vi.hoisted(()=>({role:'teacher'}))
vi.mock('vue-router',()=>({useRoute:()=>route,useRouter:()=>router}))
vi.mock('../../src/api/services/auth',()=>({getStoredRole:()=>identity.role}))
vi.mock('../../src/api/services/scheduleAdjustments',()=>({createScheduleAdjustment:vi.fn(),listScheduleAdjustments:vi.fn(),updateScheduleAdjustment:vi.fn()}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn(options=>options.rows.length)}))
vi.mock('element-plus',()=>({ElMessage:{warning:vi.fn(),success:vi.fn(),error:vi.fn()}}))
const wrappers=[];const records=[{id:1,course:'数据库',originalTime:'周一',targetTime:'周二',status:'审核中'},{id:2,course:'程序设计',status:'已通过'},{id:3,course:'人工智能',status:'已调整'}]
it('rendered adjustment table updates selection and dispatches withdrawal',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('数据库');expect(w.text()).toContain('程序设计');const table=w.findComponent({name:'TestTable'});table.vm.$emit('row-click',vm.requestList[1]);await flushPromises();expect(vm.selectedRequest.course).toBe('程序设计');table.vm.$emit('row-click',vm.requestList[0]);await flushPromises();await w.findAll('button').find(b=>b.text()==='撤回申请').trigger('click');await flushPromises();expect(api.updateScheduleAdjustment).toHaveBeenCalledWith(1,expect.objectContaining({status:'已取消'}));w.findComponent({name:'TeacherAdjustmentDialog'}).vm.$emit('update:modelValue',true);expect(vm.dialogVisible).toBe(true)})
beforeEach(()=>{identity.role='teacher';vi.clearAllMocks();route.query={};api.listScheduleAdjustments.mockResolvedValue(records);api.updateScheduleAdjustment.mockImplementation(async(id,payload)=>payload);api.createScheduleAdjustment.mockImplementation(async payload=>({...payload,id:4}))})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','icon','card','tag','empty','table','table-column','radio-group','radio-button','steps','step'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads and normalizes workflow metadata with filter metrics and pagination',async()=>{
  const vm=await setup();expect(vm.metrics.map(item=>item.value)).toEqual(['3','1','1','1']);expect(vm.selectedRequest.id).toBe(1);expect(vm.requestList[1]).toMatchObject({flowStep:3,tagType:'success'});expect(vm.requestList[2]).toMatchObject({flowStep:4,tagType:'primary'});vm.activeStatus='审核中';expect(vm.filteredRequests.map(row=>row.id)).toEqual([1]);expect(vm.pagedRequests).toHaveLength(1);expect(vm.normalizeAdjustment({status:'已取消'})).toMatchObject({flowStep:1,tagType:'info',reviewer:'教师撤回'});expect(vm.normalizeAdjustment({}).course).toBe('未命名课程')
})
it('route context pre-fills create requests and offers its course',async()=>{
  route.query={action:'create',course:' 高等数学 ',time:'周一上午',lab:'A101'};const vm=await setup();expect(vm.dialogVisible).toBe(true);expect(vm.adjustmentPrefill).toEqual({course:'高等数学',originalTime:'周一上午',targetTime:'',reason:'原实验室 A101 授课安排需协调'});expect(vm.courseOptions).toContain(' 高等数学 ')
})
it('withdraw only pending requests and persists server IDs before replacing selection',async()=>{
  const vm=await setup();await vm.withdrawAdjustment(vm.requestList[1]);expect(api.updateScheduleAdjustment).not.toHaveBeenCalled();await vm.withdrawAdjustment();expect(api.updateScheduleAdjustment).toHaveBeenCalledWith(1,expect.objectContaining({status:'已取消',flowStep:1,reviewer:'教师撤回'}));expect(vm.selectedRequest.status).toBe('已取消');vm.selectedRequest=null;await vm.withdrawAdjustment()
})
it('local draft withdrawal is immediate and failed server withdrawal retains pending state',async()=>{
  api.listScheduleAdjustments.mockResolvedValue([]);const vm=await setup();await vm.withdrawAdjustment();expect(api.updateScheduleAdjustment).not.toHaveBeenCalled();expect(vm.selectedRequest.status).toBe('已取消');vm.requestList=records.map(vm.normalizeAdjustment);vm.selectedRequest=vm.requestList[0];api.updateScheduleAdjustment.mockRejectedValueOnce(new Error('offline'));await vm.withdrawAdjustment();expect(vm.selectedRequest.status).toBe('审核中');expect(ElMessage.error).toHaveBeenCalledWith('offline')
})
it('submission adds saved requests and reports errors without appending failed input',async()=>{
  const vm=await setup();await vm.handleSave({course:'操作系统',targetTime:'周三'});expect(api.createScheduleAdjustment).toHaveBeenCalledWith(expect.objectContaining({course:'操作系统',status:'审核中',flowStep:2,teacherName:'任课教师'}));expect(vm.requestList[0].id).toBe(4);expect(vm.selectedRequest.id).toBe(4);api.createScheduleAdjustment.mockRejectedValueOnce(new Error('offline'));await vm.handleSave({course:'失败'});expect(vm.requestList).toHaveLength(4)
})
it('sync failures retain current records and release loading state',async()=>{
  const vm=await setup();api.listScheduleAdjustments.mockRejectedValueOnce(new Error('offline'));expect(await vm.loadAdjustments()).toBe(false);expect(vm.requestList).toHaveLength(3);expect(vm.isLoading).toBe(false);await vm.syncAdjustments();expect(ElMessage.success).toHaveBeenCalledWith('已同步调课申请记录')
})
it('hero commands open blank dialog, sync, export filtered rows and navigate home',async()=>{
  const vm=await setup();await vm.handleHeroAction({command:'create'});expect(vm.dialogVisible).toBe(true);expect(vm.adjustmentPrefill).toBeNull();await vm.handleHeroAction({command:'sync'});expect(api.listScheduleAdjustments).toHaveBeenCalledTimes(2);vm.activeStatus='审核中';await vm.handleHeroAction({command:'export'});expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({rows:[vm.requestList[0]],filename:'调课申请'}));vm.activeStatus='missing';vm.exportAdjustmentRequests();expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的调课申请');vm.handleBack();expect(router.push).toHaveBeenCalledWith('/teacher')
})
it('empty route drafts remain closed and missing error messages use actionable fallbacks', async () => {
  route.query={action:'create',course:' '}
  const vm=await setup();expect(vm.dialogVisible).toBe(false)
  api.listScheduleAdjustments.mockRejectedValueOnce(null)
  await vm.syncAdjustments()
  expect(ElMessage.warning).toHaveBeenCalledWith('调课申请加载失败，已保留当前列表')
  expect(ElMessage.success).not.toHaveBeenCalled()
  api.updateScheduleAdjustment.mockRejectedValueOnce({})
  await vm.withdrawAdjustment()
  expect(ElMessage.error).toHaveBeenCalledWith('调课申请撤回失败')
  api.createScheduleAdjustment.mockRejectedValueOnce({})
  await vm.handleSave({course:'失败'})
  expect(ElMessage.error).toHaveBeenCalledWith('调课申请提交失败')
  await vm.handleHeroAction({command:'unknown'})
  expect(vm.requestList).toHaveLength(3)
})
it('route drafts need no lab or time and withdrawing a nonselected row keeps the selected request', async () => {
  route.query={action:'create',course:'数据库'}
  const vm=await setup(true),w=wrappers.at(-1)
  expect(vm.adjustmentPrefill).toMatchObject({originalTime:'',reason:''})
  vm.requestList.push(vm.normalizeAdjustment({id:8,course:'新课程',status:'审核中'}))
  vm.selectedRequest=vm.requestList[1]
  await flushPromises()
  const buttons=w.findAll('button').filter(button=>button.text()==='撤回')
  expect(buttons.length).toBeGreaterThan(0)
  await buttons.at(-1).trigger('click');await flushPromises()
  expect(api.updateScheduleAdjustment).toHaveBeenCalledWith(8,expect.objectContaining({status:'已取消'}))
  expect(vm.selectedRequest.id).toBe(2)
})
it('missing route course is ignored and unknown stored roles retain a safe home destination',async()=>{
  route.query={action:'create'};identity.role='unknown'
  const vm=await setup();expect(vm.dialogVisible).toBe(false)
  vm.handleBack();expect(router.push).toHaveBeenCalledWith('/teacher')
  vm.selectedRequest=null
  vm.replaceAdjustment({...vm.requestList[0],status:'已取消'})
  expect(vm.selectedRequest).toBeNull();expect(vm.requestList[0].status).toBe('已取消')
})

