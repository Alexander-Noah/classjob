// @vitest-environment happy-dom
import { beforeEach,afterEach,it,expect,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import { panelStubs } from '../support/formStubs'
import Page from '../../src/components/business-management/BusinessManagementPage.vue'
const service=vi.hoisted(()=>Object.fromEntries(['list','replace','create','update','remove','reset'].map(key=>[key,vi.fn()])))
const router=vi.hoisted(()=>({push:vi.fn()}))
vi.mock('vue-router',()=>({useRoute:()=>({name:'notices',meta:{roleScope:'labAdmin',title:'通知管理'}}),useRouter:()=>router}))
vi.mock('../../src/api/services/collectionService',()=>({createCollectionService:()=>service}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn(()=>1)}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),warning:vi.fn()},ElMessageBox:{confirm:vi.fn().mockResolvedValue(true)}}))
const wrappers=[]
beforeEach(()=>{vi.clearAllMocks();service.list.mockResolvedValue([{id:1,name:'通知甲',status:'待处理'}]);service.create.mockImplementation(async record=>({...record,id:2}));service.update.mockImplementation(async(id,record)=>({...record,id}));service.replace.mockImplementation(async rows=>rows);service.remove.mockResolvedValue([])})
afterEach(()=>wrappers.splice(0).forEach(w=>w.unmount()))
it('business page connects table, status summary, editors, drawer and role navigation',async()=>{const w=shallowMount(Page,{props:{moduleConfig:{badge:'业务',recordName:'通知',tableColumns:[{prop:'name',label:'名称'},{prop:'status',label:'状态',type:'tag'}],statusOptions:[{label:'待处理'},{label:'已处理'}],featureCards:[{title:'通知流程',tag:'审核'}],metrics:[{label:'总数',value:1}]}},global:{stubs:panelStubs()}});wrappers.push(w);await flushPromises();expect(w.text()).toContain('通知流程');const vm=w.vm,table=w.findComponent({name:'BusinessRecordTable'}),dialog=w.findComponent({name:'BusinessRecordDialog'});const row=vm.business.records.value[0];table.vm.$emit('update:keyword','通知');table.vm.$emit('update:statusFilter','待处理');table.vm.$emit('page-change',1);table.vm.$emit('page-size-change',20);table.vm.$emit('selection-change',[row]);expect(vm.business.keyword.value).toBe('通知');expect(vm.business.statusFilter.value).toBe('待处理');expect(vm.business.pageSize.value).toBe(20);table.vm.$emit('row-click',row);expect(vm.business.detailVisible.value).toBe(true);w.findComponent({name:'BusinessRecordDrawer'}).vm.$emit('update:modelValue',false);expect(vm.business.detailVisible.value).toBe(false);table.vm.$emit('create');expect(vm.business.dialogVisible.value).toBe(true);dialog.vm.$emit('update:modelValue',false);expect(vm.business.dialogVisible.value).toBe(false);table.vm.$emit('edit',row);Object.assign(vm.business.formModel,{name:'通知乙'});dialog.vm.$emit('submit');await flushPromises();expect(service.replace).toHaveBeenCalled();w.findComponent({name:'BusinessStatusSummary'}).vm.$emit('update:statusFilter','all');expect(vm.business.statusFilter.value).toBe('all');w.findComponent({name:'PageHero'}).vm.$emit('back');expect(router.push).toHaveBeenCalledWith('/lab-admin')})

