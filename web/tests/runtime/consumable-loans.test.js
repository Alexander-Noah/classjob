// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/consumables/components/ConsumableLoans.vue'
import http from '../../src/api/http'
import { tableStubs } from '../support/formStubs'
import { ElMessage } from 'element-plus'
vi.mock('../../src/api/http',()=>({default:{get:vi.fn(),post:vi.fn()}}))
vi.mock('element-plus',()=>({ElMessage:{error:vi.fn(),success:vi.fn(),info:vi.fn()}}))
const loans=[{id:1,consumableName:'网线',borrower:'教师甲',contact:'123',purpose:'教学',remainingQuantity:2,status:'借用中'},{id:2,consumableName:'电池',borrower:'教师乙',remainingQuantity:1,status:'已逾期'},{id:3,remainingQuantity:0,status:'已归还'}]
it('rendered loan table and dialogs bind filters, borrow fields, returns and detail actions',async()=>{
  const {vm,wrapper:w}=await setup(true)
  expect(w.text()).toContain('网线');expect(w.text()).toContain('逾期 1 笔')
  await w.get('input[placeholder="搜索耗材、借用人、用途"]').setValue('教师甲');expect(vm.filtered).toHaveLength(1)
  await w.findAll('select')[0].setValue('借用中');expect(vm.filters.status).toBe('借用中')
  const click=async label=>{await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()}
  await click('借用登记');await w.get('select[aria-label="请选择耗材"]').setValue('7')
  for(const input of w.findAll('input').slice(1))await input.setValue(input.attributes('placeholder')?.includes('日期')?'2026-10-20':'2')
  for(const area of w.findAll('textarea'))await area.setValue('教学测试')
  await click('确认借用');expect(http.post).toHaveBeenCalledWith('/consumables/loans',expect.objectContaining({consumableId:7}))
  await click('归还');await click('确认归还');expect(http.post).toHaveBeenCalledWith('/consumables/loans/1/return',expect.any(Object))
  await click('详情');expect(w.text()).toContain('借用人 / 联系方式');await click('关闭');expect(vm.detailVisible).toBe(false)
  http.get.mockRejectedValueOnce({});await vm.load();await flushPromises();await click('重新加载');expect(vm.loadError).toBe('')
})
const wrappers=[]
beforeEach(()=>{vi.clearAllMocks();vi.useFakeTimers({toFake:['Date']});vi.setSystemTime(new Date(2026,9,18,10));http.get.mockImplementation(async url=>({data:{data:url==='/consumables/loans'?loans:loans[0]}}));http.post.mockResolvedValue({})})
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.useRealTimers()})
async function setup(render=false){const wrapper=shallowMount(Page,{props:{consumables:[{id:7,name:'网线',stock:5}]},global:{directives:{loading:{}},stubs:render?tableStubs():Object.fromEntries(['button','card','input','select','option','table','table-column','tag','dialog','form','form-item','input-number','date-picker','empty','alert','descriptions','descriptions-item'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return {vm:wrapper.vm.$.setupState,wrapper}}
it('loads outstanding and overdue counts with case-insensitive filters',async()=>{const {vm}=await setup();expect(vm.outstanding).toBe(2);expect(vm.overdue).toBe(1);expect(vm.pagedRecords).toHaveLength(3);vm.filters.keyword='教师甲';vm.filters.status='借用中';expect(vm.filtered.map(row=>row.id)).toEqual([1]);expect(['已归还','已逾期','部分归还','借用中','未知'].map(vm.statusType)).toEqual(['success','danger','warning','primary','info'])})
it('creates a stable borrow idempotency key and validates quantities against available stock',async()=>{
  const {vm}=await setup();vm.openBorrow({id:7});expect(vm.borrowForm.consumableId).toBe('7');expect(vm.borrowForm.requestId).toMatch(/^[\da-f-]{36}$/);expect(vm.borrowVisible).toBe(true);const validate=value=>{const done=vi.fn();vm.borrowRules.quantity[0].validator({},value,done);return done.mock.calls[0][0]};expect(validate(5)).toBeUndefined();for(const value of [0,-1,1.5,6,Number.MAX_SAFE_INTEGER+1])expect(validate(value)).toBeInstanceOf(Error);expect(vm.disabledDueDate(new Date(2026,9,17))).toBe(true);expect(vm.disabledDueDate(new Date(2026,9,18))).toBe(false)
})
it('borrow validation and double-submit guards prevent network writes',async()=>{
  const {vm}=await setup();vm.openBorrow({id:7});vm.borrowRef={validate:vi.fn().mockRejectedValue('invalid')};await vm.saveBorrow();expect(http.post).not.toHaveBeenCalled();vm.borrowRef.validate.mockResolvedValue(true);vm.saving=true;await vm.saveBorrow();expect(http.post).not.toHaveBeenCalled()
})
it('successful borrow posts numeric identity and emits stock refresh after closing dialog',async()=>{
  const {vm,wrapper}=await setup();vm.openBorrow({id:7});vm.borrowRef={validate:vi.fn().mockResolvedValue(true)};Object.assign(vm.borrowForm,{borrower:'教师甲',quantity:2,purpose:'教学',dueDate:'2026-10-20'});const requestId=vm.borrowForm.requestId;await vm.saveBorrow();expect(http.post).toHaveBeenCalledWith('/consumables/loans',expect.objectContaining({consumableId:7,quantity:2,requestId}));expect(vm.borrowVisible).toBe(false);expect(vm.saving).toBe(false);expect(wrapper.emitted('changed')).toHaveLength(1)
})
it('failed borrow retains input and idempotency key and allows retry without duplicate error messages',async()=>{
  const {vm}=await setup();vm.openBorrow({id:7});vm.borrowRef={validate:vi.fn().mockResolvedValue(true)};const requestId=vm.borrowForm.requestId;http.post.mockRejectedValueOnce({userMessage:'库存不足'});await vm.saveBorrow();expect(vm.borrowVisible).toBe(true);expect(vm.borrowForm.requestId).toBe(requestId);expect(vm.saving).toBe(false);expect(ElMessage.error).toHaveBeenCalledWith('库存不足');ElMessage.error.mockClear();http.post.mockRejectedValueOnce({__handled:true});await vm.saveBorrow();expect(ElMessage.error).not.toHaveBeenCalled()
})
it('return refreshes remaining quantity before opening and validates partial returns',async()=>{
  const {vm,wrapper}=await setup();await vm.openReturn(loans[0]);expect(vm.returnForm.quantity).toBe(2);const done=vi.fn();vm.returnRules.quantity[0].validator({},3,done);expect(done.mock.calls[0][0]).toBeInstanceOf(Error);vm.returnRules.quantity[0].validator({},1,done);expect(done.mock.calls[1][0]).toBeUndefined();vm.returnRef={validate:vi.fn().mockResolvedValue(true)};vm.returnForm.quantity=1;await vm.saveReturn();expect(http.post).toHaveBeenCalledWith('/consumables/loans/1/return',expect.objectContaining({quantity:1,requestId:expect.any(String)}));expect(vm.returnVisible).toBe(false);expect(wrapper.emitted('changed')).toHaveLength(1)
})
it('fully returned records cannot open another return and detail loads independently',async()=>{
  const {vm}=await setup();http.get.mockResolvedValueOnce({data:{data:loans[2]}});await vm.openReturn(loans[2]);expect(vm.returnVisible).toBe(false);expect(ElMessage.info).toHaveBeenCalledWith('该借用记录已全部归还');await vm.openDetail(loans[0]);expect(vm.detailVisible).toBe(true);expect(vm.detail.id).toBe(1)
})
it('load, detail and return failures remain recoverable',async()=>{
  const {vm}=await setup();http.get.mockRejectedValueOnce(new Error('offline'));await vm.load();expect(vm.loadError).toContain('加载失败');expect(vm.loading).toBe(false);http.get.mockRejectedValueOnce(new Error('detail offline'));await vm.openDetail(loans[0]);expect(ElMessage.error).toHaveBeenCalledWith('detail offline');await vm.openReturn(loans[0]);vm.returnRef={validate:vi.fn().mockResolvedValue(true)};http.post.mockRejectedValueOnce(new Error('return offline'));await vm.saveReturn();expect(vm.returnVisible).toBe(true);expect(vm.saving).toBe(false);expect(ElMessage.error).toHaveBeenCalledWith('return offline')
})


