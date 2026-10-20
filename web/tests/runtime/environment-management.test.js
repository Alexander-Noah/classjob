import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { shallowMount, flushPromises } from '@vue/test-utils'
import Environment from '../../src/views/environments/EnvironmentManagement.vue'
import http from '../../src/api/http'
import { createCollectionService } from '../../src/api/services/collectionService'
const navigation=vi.hoisted(()=>({replace:vi.fn()}))
const collection=vi.hoisted(()=>({list:vi.fn(),create:vi.fn(),update:vi.fn(),replace:vi.fn(),remove:vi.fn(),reset:vi.fn()}))
let route
vi.mock('vue-router',()=>({useRoute:()=>route,useRouter:()=>navigation}))
vi.mock('../../src/api/http',()=>({default:{get:vi.fn()}}))
vi.mock('../../src/api/services/collectionService',()=>({createCollectionService:vi.fn(()=>collection)}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn()}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),warning:vi.fn()},ElMessageBox:{confirm:vi.fn().mockResolvedValue(true)}}))
const wrappers=[]
it('renders linked environment details and connects the table and editor events',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('IDEA');expect(w.text()).toContain('配置核验');const table=w.findComponent({name:'BusinessRecordTable'});table.vm.$emit('update:keyword','IDEA');expect(vm.business.keyword.value).toBe('IDEA');table.vm.$emit('update:statusFilter','all');table.vm.$emit('selection-change',vm.business.records.value);table.vm.$emit('page-change',1);table.vm.$emit('page-size-change',20);table.vm.$emit('row-click',software);await flushPromises();expect(vm.detailTitle).toBe('IDEA');table.vm.$emit('create');await flushPromises();expect(vm.business.dialogVisible.value).toBe(true);const editor=w.findComponent({name:'SoftwareEnvironmentDialog'});editor.vm.$emit('update:modelValue',false);await flushPromises();expect(vm.business.dialogVisible.value).toBe(false);vm.selectEnvironmentRecord(null);await flushPromises();expect(w.text()).not.toContain('配置核验');route.query.tab='template';await flushPromises();expect(w.findComponent({name:'EnvironmentTemplateDialog'}).exists()).toBe(true);route.query.tab='course';await flushPromises();expect(w.findComponent({name:'CourseEnvironmentDialog'}).exists()).toBe(true)})
const software={id:1,name:'IDEA',version:'2026',type:'开发工具',license:'教学授权',labId:7,labName:'机房',status:'已安装'}
const template={id:2,name:'程序设计模板',course:'程序设计',os:'Windows',softwareList:'IDEA',labs:'机房',status:'可复用'}
beforeEach(()=>{
  vi.clearAllMocks(); route=reactive({name:'lab-admin-environments',path:'/environments',query:{tab:'software'}})
  collection.list.mockResolvedValue([software]); collection.remove.mockResolvedValue([]); collection.replace.mockImplementation(async rows=>rows); collection.create.mockResolvedValue({...software,id:3})
  http.get.mockImplementation(async url=>({data:{data:url==='/labs'?[{id:7,labCode:'A101',labName:'机房'}]:url==='/class-timetables'?[{courseName:'程序设计',teacher:'教师甲',className:'软件一班'}]:url==='/lab-software'?[software]:[template]}}))
})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const names=['tabs','tab-pane','button','card','tag','icon','empty','descriptions','descriptions-item','progress','dialog']; const wrapper=shallowMount(Environment,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(names.map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads course, teacher, class, software and template options from real service results',async()=>{
  const vm=await setup()
  expect(vm.activeTab).toBe('software');expect(vm.pageTitle).toBe('软件环境管理');expect(createCollectionService).toHaveBeenCalled()
  expect(vm.courseOptions).toEqual([{label:'程序设计',value:'程序设计',name:'程序设计'}])
  expect(vm.labOptions[0]).toMatchObject({value:7,label:'A101 机房'})
  expect(vm.softwareNameOptions[0].value).toBe('IDEA');expect(vm.templateNameOptions[0].value).toBe('程序设计模板')
  expect(vm.softwareVersionOptions[0].value).toBe('2026');expect(vm.softwareListOptions[0].value).toBe('IDEA')
  expect(vm.metrics[0].value).toBe(1);expect(vm.workflowSteps).toHaveLength(3)
  expect(vm.detailTitle).toBe('IDEA');expect(vm.detailHighlights.length).toBeGreaterThan(0);expect(vm.checklistItems.every(item=>item.done)).toBe(true)
})
it('software selection fills linked fields while preserving explicit form input',async()=>{
  const vm=await setup(); vm.business.openCreateDialog();vm.business.formModel.name='IDEA';await flushPromises()
  expect(vm.business.formModel).toMatchObject({version:'最新版',license:'校内授权',labId:7})
  vm.business.formModel.version='custom';vm.applySoftwareLinkage('IDEA');expect(vm.business.formModel.version).toBe('custom')
  vm.applySoftwareLinkage('IDEA',{overwrite:true});expect(vm.business.formModel.version).toBe('2026')
  vm.applySoftwareLinkage('missing');expect(vm.softwareRecords).toHaveLength(1)
})
it('course tab auto-selects unique teacher/class and suggests reusable template software',async()=>{
  const vm=await setup();route.query.tab='course';await flushPromises()
  vm.business.openCreateDialog();vm.business.formModel.course='程序设计';await flushPromises()
  expect(vm.business.formModel).toMatchObject({teacher:'教师甲',className:'软件一班',software:'IDEA',labType:'机房'})
  expect(vm.dialogFieldOptions.course[0].value).toBe('程序设计');expect(vm.metrics[0].label).toBe('环境需求')
  vm.updateCourseLinkedOptions('missing');expect(vm.teacherOptions).toEqual([]);expect(vm.business.formModel.teacher).toBe('')
  vm.updateCourseLinkedOptions('');expect(vm.teacherOptions).toHaveLength(1)
})
it('template linkage supports course/name lookup and selected record details',async()=>{
  const vm=await setup();route.query.tab='template';await flushPromises();vm.business.openCreateDialog();vm.business.formModel.name=template.name;await flushPromises()
  expect(vm.business.formModel).toMatchObject({course:'适用课程',os:'Windows 11',softwareList:'待维护软件清单',labs:'待选择实验室'})
  vm.applyTemplateLinkage(template,{overwrite:true})
  expect(vm.business.formModel).toMatchObject({course:'程序设计',os:'Windows',softwareList:'IDEA',labs:'机房'})
  expect(vm.findTemplateByCourse('程序设计实训')).toEqual(template);expect(vm.findTemplateByCourse('')).toBeNull()
  vm.selectEnvironmentRecord(template);expect(vm.detailTitle).toBe(template.name);expect(vm.checklistItems.every(item=>item.done)).toBe(true)
  expect(vm.templateCourseOptions.map(option=>option.value)).toEqual(['程序设计'])
  expect(vm.metrics[2].value).toBe(1);expect(vm.statusOverview).toBeInstanceOf(Array)
})
it('tab navigation preserves query parameters and rejects unknown tabs',async()=>{
  const vm=await setup();vm.activeTab='course';expect(navigation.replace).toHaveBeenCalledWith({path:'/environments',query:{tab:'course'}})
  route.query.tab='invalid';expect(vm.activeTab).toBe('software')
})
it('option response wrappers normalize consistently and load errors preserve a usable page',async()=>{
  const vm=await setup()
  for(const payload of [[software],{data:[software]},{data:{data:[software]}},{records:[software]},{data:{records:[software]}},{data:{list:[software]}},{list:[software]}]) expect(vm.getResponseList(payload)).toEqual([software])
  expect(vm.getResponseList({})).toEqual([])
  const error=vi.spyOn(console,'error').mockImplementation(()=>{});http.get.mockRejectedValue(new Error('offline'))
  await vm.loadCourseEnvironmentOptions();await vm.loadLabOptions();await vm.loadLinkedEnvironmentRecords()
  expect(vm.courseOptions).toEqual([]);expect(vm.softwareRecords).toEqual([]);expect(vm.templateRecords).toEqual([]);error.mockRestore()
})


it('loads legacy timetable identities, filters empty names and preserves nonnumeric lab keys',async()=>{
 const vm=await setup();http.get.mockImplementation(async url=>url==='/class-timetables'?[{course:'课程甲',teacherName:'教师甲',rowClassName:'甲班'},{name:'课程乙',realName:'教师乙',class_name:'乙班'},{subjectName:'课程丙',username:'教师丙',row_class_name:'丙班'},{lessonName:'课程丁',clazz:'丁班'},{class:'戊班'},{}]:[{labId:'lab-alpha',location:'西楼机房'},{}]);await vm.loadCourseEnvironmentOptions();expect(vm.courseOptions.map(o=>o.value)).toEqual(expect.arrayContaining(['课程丁','课程丙','课程乙','课程甲']));expect(vm.allClassOptions).toHaveLength(5);await vm.loadLabOptions();expect(vm.labOptions[0].value).toBe('lab-alpha')
 expect(vm.getTeacherName({})).toBe('');expect(vm.getClassName({})).toBe('');vm.softwareRecords=[{softwareName:'备用软件',softwareVersion:'1.2',licenseInfo:'免费',lab:'西楼机房'}];expect(vm.softwareNameOptions[0].value).toBe('备用软件');expect(vm.softwareVersionOptions[0].value).toBe('1.2');vm.business.openCreateDialog();vm.applySoftwareLinkage('备用软件',{overwrite:true});expect(vm.business.formModel).toMatchObject({version:'1.2',license:'免费',lab:'西楼机房'});vm.applyTemplateLinkage(null);vm.applyCourseTemplateSuggestion('不存在课程');vm.setIfPresent('version','');expect(vm.business.formModel.version).toBe('1.2')
})
it('submits environment changes, deletes selected details and reloads empty linked options',async()=>{
 const vm=await setup(true);vm.business.openCreateDialog();Object.assign(vm.business.formModel,{...software,name:'新软件'});await vm.handleSubmitRecord();expect(collection.create).toHaveBeenCalled();expect(vm.business.dialogVisible.value).toBe(false)
 vm.selectEnvironmentRecord(vm.business.records.value[0]);await vm.handleDeleteRecord(vm.selectedEnvironmentRecord);await flushPromises();expect(collection.remove).toHaveBeenCalled();vm.selectEnvironmentRecord(null);expect(vm.detailTitle).toBeTruthy();expect(vm.checklistItems.some(i=>!i.done)).toBe(true)
 vm.labOptions=[];vm.softwareRecords=[];vm.templateRecords=[];route.query.tab='template';await flushPromises();vm.business.openCreateDialog();await flushPromises();expect(vm.labOptions).toHaveLength(1);expect(vm.softwareRecords).toHaveLength(1);vm.business.formModel.course='程序设计';await flushPromises();expect(vm.business.formModel.os).toBeTruthy()
})

it('course checklist and legacy software room names expose missing configuration accurately',async()=>{
 const vm=await setup(true),w=wrappers.at(-1)
 vm.business.records.value=[{...software,labId:'',lab:'历史机房'}]
 expect(vm.metrics[0].trend).toBe('覆盖 1 间实验室')
 route.query.tab='course';await flushPromises()
 vm.selectEnvironmentRecord({course:'程序设计',teacher:'教师甲',className:'一班',labType:'机房',software:'IDEA',status:'已确认'})
 expect(vm.checklistItems.every(item=>item.done)).toBe(true)
 vm.selectEnvironmentRecord({course:'程序设计',teacher:'教师甲'})
 expect(vm.checklistItems[0].done).toBe(false)
 vm.business.formModel.name='后台切换';await flushPromises();expect(vm.business.dialogVisible.value).toBe(false)
 vm.business.openCreateDialog();await vm.handleSubmitRecord()
 expect(vm.business.dialogVisible.value).toBe(true);expect(collection.create).not.toHaveBeenCalled()
 const different={_recordId:'different',course:'其他课程'};vm.selectEnvironmentRecord(different)
 vm.handleDeleteRecord({_recordId:'missing'});expect(vm.selectedEnvironmentRecord).toEqual(different)
 await flushPromises();expect(vm.selectedEnvironmentRecord).toBeTruthy()
})
it('raw linked-list responses are accepted and an absent tab defaults to software',async()=>{
 delete route.query.tab;http.get.mockImplementation(async url=>url==='/lab-software'?[software]:url==='/environment-templates'?[template]:[])
 const vm=await setup();await vm.loadLinkedEnvironmentRecords()
 expect(vm.activeTab).toBe('software');expect(vm.softwareRecords).toEqual([software])
})