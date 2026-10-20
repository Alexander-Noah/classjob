import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/teacher/TeacherCourseEnvironments.vue'
import http from '../../src/api/http'
import { API_ENDPOINTS } from '../../src/api/endpoints'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage } from 'element-plus'
const auth=vi.hoisted(()=>({role:'teacher',user:{id:7,realName:'教师甲'}}))
const route=vi.hoisted(()=>({query:{}}));const router=vi.hoisted(()=>({push:vi.fn()}))
vi.mock('vue-router',()=>({useRoute:()=>route,useRouter:()=>router}))
vi.mock('../../src/api/services/auth',()=>({getStoredRole:()=>auth.role,getStoredUser:()=>auth.user}))
vi.mock('../../src/api/http',()=>({default:{get:vi.fn(),post:vi.fn(),put:vi.fn()}}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn(options=>options.rows.length)}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),warning:vi.fn(),error:vi.fn()}}))
const wrappers=[]
it('rendered environment cards expose delivered software and return form actions',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('程序设计');expect(w.text()).toContain('IDEA');await w.get('input[placeholder="搜索课程、实验室或软件环境"]').setValue('IDEA');expect(vm.keyword).toBe('IDEA');await w.findAll('button').find(b=>b.text()==='查看详情').trigger('click');expect(vm.drawerVisible).toBe(true);await w.findAll('button').find(b=>b.text()==='退回调整').trigger('click');await flushPromises();await w.get('textarea').setValue('版本不匹配');expect(vm.returnForm.reason).toBe('版本不匹配');await w.findAll('button').find(b=>b.text()==='提交退回').trigger('click');await flushPromises();expect(http.put).toHaveBeenCalled();const drawer=w.findComponent({name:'TeacherEnvironmentDrawer'});drawer.vm.$emit('update:modelValue',false);expect(vm.drawerVisible).toBe(false);w.findComponent({name:'TeacherEnvironmentRequestDialog'}).vm.$emit('update:modelValue',true);expect(vm.requestDialogVisible).toBe(true)})
const record={id:1,courseName:'程序设计',softwareRequirements:'IDEA',assignedLabId:1,assignedLabCode:'A101',assignedLabName:'机房',useTime:'周一上午',status:'待确认'}
beforeEach(()=>{vi.clearAllMocks();auth.role='teacher';auth.user={id:7,realName:'教师甲'};route.query={};http.get.mockResolvedValue({data:{data:[record]}});http.post.mockResolvedValue({data:{data:{...record,status:'已确认',confirmStatus:'已确认'}}});http.put.mockImplementation(async(url,payload)=>({data:{data:payload}}))})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','icon','card','input','table','table-column','tag','dialog','form','form-item','radio-group','radio-button','empty'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('normalizes backend delivery fields and computes metrics and combined filters',async()=>{
  const vm=await setup();expect(vm.environmentRecords[0]).toMatchObject({course:'程序设计',lab:'A101 机房',software:'IDEA',schedule:'周一上午',step:2,status:'待确认'});expect(vm.metrics.map(item=>item.value)).toEqual(['1','1','0','0']);vm.keyword='idea';expect(vm.filteredEnvironments).toHaveLength(1);vm.activeStatus='已确认';expect(vm.filteredEnvironments).toEqual([])
  expect(vm.normalizeEnvironmentRecord({status:'退回',specialRequirements:'更换版本'})).toMatchObject({status:'需调整',step:1,reminder:'更换版本',tagType:'danger'});expect(vm.normalizeEnvironmentRecord({confirmStatus:'已确认'})).toMatchObject({status:'已确认',step:3,tagType:'success'});expect(vm.normalizeEnvironmentRecord({}).lab).toBe('待分配实验室')
})
it('keeps existing defaults on empty or failed list responses and releases loading state',async()=>{
  http.get.mockResolvedValue({data:[]});const vm=await setup();expect(vm.environmentRecords).toHaveLength(3);http.get.mockRejectedValueOnce(new Error('offline'));await vm.loadEnvironmentRecords();expect(vm.environmentRecords).toHaveLength(3);expect(vm.isLoading).toBe(false);expect(ElMessage.warning).toHaveBeenCalledWith('offline')
})
it('route context selects existing courses or pre-fills a new course request',async()=>{
  route.query={course:'程序设计'};const vm=await setup();expect(vm.keyword).toBe('程序设计');expect(vm.selectedEnvironment.id).toBe(1);expect(vm.requestDialogVisible).toBe(false)
  route.query={course:'数据库',className:'软件一班',time:'周二',lab:'B202',environment:'MySQL'};vm.applyRouteCourseContext();expect(vm.requestDialogVisible).toBe(true);expect(vm.requestPrefill).toMatchObject({courseName:'数据库',className:'软件一班',labType:'B202',softwareRequirements:'MySQL',specialRequirements:'工作台带入实验室：B202'})
})
it('request submission includes authenticated teacher identity and replaces saved records',async()=>{
  const vm=await setup();await vm.submitEnvironmentRequest({courseName:'程序设计',softwareRequirements:'IDEA'});expect(http.post).toHaveBeenCalledWith(API_ENDPOINTS.courseEnvironmentRequests.list,expect.objectContaining({teacherUserId:7,teacherName:'教师甲',teacher:'教师甲',processStatus:'待配置',status:'待确认',confirmStatus:'待确认'}));expect(vm.environmentRecords).toHaveLength(1);expect(vm.selectedEnvironment.status).toBe('已确认')
  vm.replaceEnvironmentRecord({...record,id:2});expect(vm.environmentRecords.map(item=>item.id)).toEqual([2,1]);http.post.mockRejectedValueOnce(new Error('offline'));await vm.submitEnvironmentRequest({courseName:'数据库'});expect(ElMessage.error).toHaveBeenCalledWith('offline')
})
it('confirmation updates backend state and closes details only after success',async()=>{
  const vm=await setup();vm.handleOpenDrawer(vm.environmentRecords[0]);expect(vm.drawerVisible).toBe(true);await vm.handleConfirm(vm.selectedEnvironment);expect(http.post).toHaveBeenCalledWith(`${API_ENDPOINTS.courseEnvironmentRequests.detail}/1/confirm`);expect(vm.selectedEnvironment.status).toBe('已确认');expect(vm.drawerVisible).toBe(false)
  vm.drawerVisible=true;http.post.mockRejectedValueOnce(new Error('offline'));await vm.handleConfirm(vm.selectedEnvironment);expect(vm.drawerVisible).toBe(true)
})
it('return sends actionable adjustment details and preserves dialogs on failure',async()=>{
  const vm=await setup();vm.handleReturn(vm.environmentRecords[0]);vm.returnForm.reason='请升级软件';await vm.submitReturn();expect(http.put).toHaveBeenCalledWith(`${API_ENDPOINTS.courseEnvironmentRequests.detail}/1`,expect.objectContaining({courseName:'程序设计',specialRequirements:'请升级软件',processStatus:'需调整',confirmStatus:'待确认'}));expect(vm.selectedEnvironment.status).toBe('需调整');expect(vm.returnDialogVisible).toBe(false)
  vm.handleReturn(vm.selectedEnvironment);http.put.mockRejectedValueOnce(new Error('offline'));await vm.submitReturn();expect(vm.returnDialogVisible).toBe(true);vm.selectedEnvironment=null;await vm.submitReturn()
})
it('hero commands create, synchronize, export and navigate to the teacher home',async()=>{
  const vm=await setup();await vm.handleHeroAction({command:'create'});expect(vm.requestDialogVisible).toBe(true);expect(vm.requestPrefill).toBeNull();await vm.handleHeroAction({command:'remind'});expect(http.get).toHaveBeenCalledTimes(2);await vm.handleHeroAction({command:'export'});expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({filename:'课程环境清单',rows:vm.environmentRecords}));vm.keyword='missing';vm.exportEnvironmentRecords();expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的课程环境清单');vm.handleBack();expect(router.push).toHaveBeenCalledWith('/teacher')
})
it('response wrappers and invalid save responses fall back to the submitted record',async()=>{
  const vm=await setup();expect(vm.normalizeListResponse([record])).toEqual([record]);expect(vm.normalizeListResponse({data:[record]})).toEqual([record]);expect(vm.normalizeListResponse({})).toEqual([]);for(const data of [{code:200},[],null])expect(vm.normalizeRecordResponse(data,record)).toEqual(record);expect(vm.normalizeRecordResponse({data:record},{})).toEqual(record);expect(vm.normalizeRecordResponse(record,{})).toEqual(record)
})


it.each([
 [{id:'not-an-id',name:'历史教师'},undefined,'历史教师'],
 [{id:8,username:'teacher8'},8,'teacher8'],
 [null,undefined,'任课教师'],
])('legacy or missing session identity maps safely into a course environment request',async(user,id,name)=>{
 auth.user=user;http.post.mockResolvedValue({data:{code:200}})
 const vm=await setup();await vm.submitEnvironmentRequest({courseName:'新课程'})
 expect(http.post).toHaveBeenLastCalledWith(API_ENDPOINTS.courseEnvironmentRequests.list,expect.objectContaining({teacherUserId:id,teacherName:name,teacher:name}))
 expect(vm.selectedEnvironment.course).toBe('新课程')
})
it('sparse course links prefill only available context and return feedback retains its default',async()=>{
 route.query={course:'新课程'};auth.role='legacy-role'
 const vm=await setup();expect(vm.requestPrefill).toEqual({courseName:'新课程',className:'',useTime:'',labType:'',softwareRequirements:'',specialRequirements:''})
 vm.handleBack();expect(router.push).toHaveBeenLastCalledWith('/teacher')
 vm.replaceEnvironmentRecord({...record,id:2,courseName:'另一课程'})
 vm.replaceEnvironmentRecord({...record,courseName:'更新课程'})
 expect(vm.environmentRecords.map(item=>item.course)).toEqual(['另一课程','更新课程'])
 expect(vm.normalizeEnvironmentRecord({status:'退回'}).reminder).toBe('教师已退回环境，请管理员按意见调整')
 vm.handleReturn({id:1,course:'新课程',software:'IDEA',schedule:'周一'})
 await vm.submitReturn();expect(http.put).toHaveBeenLastCalledWith(expect.any(String),expect.objectContaining({specialRequirements:'教师退回环境，请按意见调整'}))
})
it('nonstandard transport errors keep course request and adjustment dialogs actionable',async()=>{
 const vm=await setup();http.get.mockRejectedValueOnce({});await vm.loadEnvironmentRecords()
 expect(ElMessage.warning).toHaveBeenLastCalledWith('后端课程环境列表暂不可用，已保留当前页面数据')
 http.post.mockRejectedValueOnce({});await vm.submitEnvironmentRequest({courseName:'新课程'})
 expect(ElMessage.error).toHaveBeenLastCalledWith('课程环境需求提交失败')
 vm.drawerVisible=true;http.post.mockRejectedValueOnce({});await vm.handleConfirm(vm.selectedEnvironment)
 expect(ElMessage.error).toHaveBeenLastCalledWith('课程环境确认失败');expect(vm.drawerVisible).toBe(true)
 vm.handleReturn(vm.selectedEnvironment);http.put.mockRejectedValueOnce({});await vm.submitReturn()
 expect(ElMessage.error).toHaveBeenLastCalledWith('退回课程环境失败');expect(vm.returnDialogVisible).toBe(true)
})