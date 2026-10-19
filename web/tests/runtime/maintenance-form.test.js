import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/maintenance-records/components/MaintenanceRecordForm.vue'
import * as api from '../../src/api/services/maintenanceRecords'
import { labResourceService } from '../../src/api/services/basicResources'
import { deviceAssetService } from '../../src/api/services/deviceAssets'
import { ElMessage } from 'element-plus'
vi.mock('../../src/api/services/maintenanceRecords',()=>({createMaintenanceRecord:vi.fn(),updateMaintenanceRecord:vi.fn()}))
vi.mock('../../src/api/services/basicResources',()=>({labResourceService:{list:vi.fn()}}))
vi.mock('../../src/api/services/deviceAssets',()=>({deviceAssetService:{list:vi.fn()}}))
vi.mock('../../src/api/services/auth',()=>({getStoredUser:()=>({realName:'管理员甲'})}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),error:vi.fn()}}))
const wrappers=[]
it('rendered maintenance form binds metadata, narrative, date and save/cancel actions',async()=>{const {vm,wrapper:w}=await setup({recordNo:'M1'},true);expect(w.text()).toContain('记录编号：M1');const selects=w.findAll('select');await selects[0].setValue('1');await selects[1].setValue('7');await selects[2].setValue('安全检查');await w.get('input[placeholder="选择维护日期"]').setValue('2026-10-19');await w.get('input[placeholder="填写维护人员姓名"]').setValue('维修员甲');for(const [index,area] of w.findAll('textarea').entries())await area.setValue(['检查网络','正常','已处理'][index]);expect(vm.form).toMatchObject({labId:'1',deviceId:'7',maintenanceType:'安全检查',maintenanceDate:'2026-10-19',maintainer:'维修员甲',content:'检查网络',result:'正常',remark:'已处理'});await w.findAll('button').find(b=>b.text()==='保存维护记录').trigger('click');await flushPromises();expect(api.updateMaintenanceRecord).toHaveBeenCalledWith('M1',expect.any(Object));await w.findAll('button').find(b=>b.text()==='取消').trigger('click');expect(w.emitted('cancel')).toHaveLength(1)})
beforeEach(()=>{vi.clearAllMocks();labResourceService.list.mockResolvedValue([{id:1,name:'A101'}]);deviceAssetService.list.mockResolvedValue([{id:7,labId:1,deviceName:'主机'},{id:8,labId:2,deviceName:'其他'}]);api.createMaintenanceRecord.mockResolvedValue({id:9});api.updateMaintenanceRecord.mockResolvedValue({id:1})})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(record={},render=false){
  const stubs=Object.fromEntries(['form-item','select','option','input','date-picker','button','alert','row','col'].map(name=>[`el-${name}`,true]))
  stubs['el-form']={template:'<form><slot /></form>',methods:{validate(){return Promise.resolve(true)},clearValidate(){}}}
  const wrapper=shallowMount(Page,{props:{record},global:{directives:{loading:{}},stubs:render?panelStubs():stubs}})
  wrappers.push(wrapper);await flushPromises();const vm=wrapper.vm.$.setupState
  vm.formRef={validate:vi.fn().mockResolvedValue(true),clearValidate:vi.fn()};return {vm,wrapper}
}
it('initializes a clean independent draft and lab changes clear incompatible device selections',async()=>{const {vm,wrapper}=await setup({id:1,labId:1,deviceId:7,content:'原内容'});expect(wrapper.vm.isDirty()).toBe(false);expect(vm.form.maintainer).toBe('管理员甲');expect(vm.availableDevices.map(device=>device.id)).toEqual([7]);vm.form.content='新内容';expect(wrapper.vm.isDirty()).toBe(true);vm.labChanged();expect(vm.form.deviceId).toBe('');expect(vm.formRef.clearValidate).toHaveBeenCalledWith('deviceId');await wrapper.setProps({record:{id:2,content:'另一记录'}});await flushPromises();expect(vm.form.content).toBe('另一记录');expect(wrapper.vm.isDirty()).toBe(false)})
it('creates or updates using authoritative lab/device metadata and emits saved data',async()=>{const {vm,wrapper}=await setup();Object.assign(vm.form,{labId:'1',deviceId:'7',maintenanceType:'巡检',content:' 检查 ',result:'正常'});await vm.submit();expect(api.createMaintenanceRecord).toHaveBeenCalledWith(expect.objectContaining({labId:1,deviceId:7,lab:'A101',device:'主机',content:'检查'}));expect(wrapper.emitted('saved')).toEqual([[{id:9}]]);expect(wrapper.vm.isDirty()).toBe(false);vm.form.id=1;await vm.submit();expect(api.updateMaintenanceRecord).toHaveBeenCalledWith(1,expect.any(Object));expect(vm.saving).toBe(false)})
it('invalid form, load errors and duplicate saving state block writes',async()=>{const {vm}=await setup();vm.formRef.validate.mockRejectedValueOnce('invalid');await vm.submit();expect(api.createMaintenanceRecord).not.toHaveBeenCalled();vm.loadingOptions=true;await vm.submit();vm.loadingOptions=false;vm.saving=true;await vm.submit();expect(api.createMaintenanceRecord).not.toHaveBeenCalled();vm.saving=false;labResourceService.list.mockRejectedValueOnce(new Error('offline'));await vm.loadOptions();expect(vm.optionError).toContain('加载失败');expect(vm.loadingOptions).toBe(false);await vm.submit();expect(api.createMaintenanceRecord).not.toHaveBeenCalled()})
it('failed metadata validation or persistence preserves dirty input and releases save state',async()=>{const {vm,wrapper}=await setup();vm.form.labId='99';await vm.submit();expect(ElMessage.error).toHaveBeenCalledWith('请选择有效的实验室');expect(api.createMaintenanceRecord).not.toHaveBeenCalled();vm.form.labId='1';vm.form.content='输入';api.createMaintenanceRecord.mockRejectedValueOnce({userMessage:'保存失败'});await vm.submit();expect(ElMessage.error).toHaveBeenCalledWith('保存失败');expect(vm.form.content).toBe('输入');expect(vm.saving).toBe(false);expect(wrapper.vm.isDirty()).toBe(true);ElMessage.error.mockClear();api.createMaintenanceRecord.mockRejectedValueOnce({__handled:true});await vm.submit();expect(ElMessage.error).not.toHaveBeenCalled()})


