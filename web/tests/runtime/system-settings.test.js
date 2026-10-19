// @vitest-environment happy-dom
import { beforeEach,it,expect,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import { defineComponent,h } from 'vue'
import { formStubs } from '../support/formStubs'
import SystemSettings from '../../src/views/system/SystemSettings.vue'
import { getSystemSettings,saveSystemSettings,resetSystemSettings } from '../../src/api/services/systemSettings'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage } from 'element-plus'
const mocks=vi.hoisted(()=>({push:vi.fn()}))
vi.mock('vue-router',()=>({useRouter:()=>({push:mocks.push})}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),warning:vi.fn(),error:vi.fn(),info:vi.fn()}}))
vi.mock('../../src/api/services/systemSettings',()=>({getSystemSettings:vi.fn(),saveSystemSettings:vi.fn(),resetSystemSettings:vi.fn()}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn()}))
beforeEach(()=>{vi.resetAllMocks();getSystemSettings.mockResolvedValue({basicConfig:{platformName:'测试平台'}});saveSystemSettings.mockImplementation(async x=>x);resetSystemSettings.mockResolvedValue({basicConfig:{platformName:'默认平台'}});exportTableToExcel.mockReturnValue(15)})
async function setup(){const stubs=formStubs();stubs['el-page-header']=defineComponent({name:'TestPageHeader',emits:['back'],setup(_,{slots,emit}){return()=>h('header',[h('button',{onClick:()=>emit('back')},'返回'),slots.extra?.()])}});const w=shallowMount(SystemSettings,{global:{stubs,directives:{loading:()=>{}}}});await flushPromises();return w}
async function click(w,label){await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()}
it('loads settings and saves edited form values across all groups',async()=>{const w=await setup();expect(w.findAll('input')[0].element.value).toBe('测试平台');let index=0;for(const input of w.findAll('input')){if(input.attributes('type')==='checkbox')await input.setValue(false);else await input.setValue(String(++index))}for(const select of w.findAll('select')){const value=select.findAll('option').at(-1).element.value;await select.setValue(value)}await w.get('textarea').setValue('本地回复');await click(w,'保存配置');expect(saveSystemSettings).toHaveBeenCalledWith(expect.objectContaining({basicConfig:expect.objectContaining({platformName:'1',autoBackup:false}),loginPolicy:expect.objectContaining({singleSignOn:false}),aiSettings:expect.objectContaining({fallbackReply:'本地回复',autoSuggestion:false})}));expect(ElMessage.success).toHaveBeenCalledWith('系统配置已保存');await click(w,'导出配置');expect(exportTableToExcel.mock.calls[0][0].rows).toHaveLength(15);await click(w,'生成变更说明');expect(ElMessage.success).toHaveBeenCalledWith('配置变更说明已生成');await click(w,'恢复默认');expect(w.findAll('input')[0].element.value).toBe('默认平台');await click(w,'返回');expect(mocks.push).toHaveBeenCalled();w.unmount()})
it('keeps defaults and reports failures from load, save and reset',async()=>{getSystemSettings.mockRejectedValue({});saveSystemSettings.mockRejectedValue({});resetSystemSettings.mockRejectedValue({});const w=await setup();expect(ElMessage.warning).toHaveBeenCalledWith('系统配置加载失败，已使用默认配置');await click(w,'保存配置');expect(ElMessage.error).toHaveBeenCalledWith('系统配置保存失败');await click(w,'恢复默认');expect(ElMessage.error).toHaveBeenCalledWith('恢复默认配置失败');w.unmount()})
