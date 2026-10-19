// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/consumables/ConsumableInventory.vue'
import http from '../../src/api/http'
import { API_ENDPOINTS } from '../../src/api/endpoints'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { readSpreadsheetRows } from '../../src/utils/importExcel'
import { ElMessage,ElMessageBox } from 'element-plus'
import { tableStubs } from '../support/formStubs'
vi.mock('../../src/api/http',()=>({default:{get:vi.fn(),post:vi.fn(),put:vi.fn(),delete:vi.fn()}}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn(options=>options.rows.length)}))
vi.mock('../../src/utils/importExcel',()=>({readSpreadsheetRows:vi.fn()}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),warning:vi.fn()},ElMessageBox:{confirm:vi.fn()}}))
const wrappers=[]
const records=[{id:1,name:'网线',category:'网络耗材',stock:10,unit:'根',location:'A101',warnThreshold:5},{id:2,name:'电池',category:'电子元器件',stock:2,warnThreshold:5},{id:3,name:'纸张',stock:0}]
const movements=[{id:1,consumableId:1,name:'网线',movementType:'出库',quantity:2,beforeStock:12,afterStock:10,operatorName:'教师甲',operatedAt:'2026-10-18 10:00',sourceTarget:'教学领用'}]
beforeEach(()=>{vi.clearAllMocks();vi.useFakeTimers({toFake:['Date']});vi.setSystemTime(new Date('2026-10-18T10:00:00+08:00'));http.get.mockImplementation(async url=>({data:url===API_ENDPOINTS.consumables.records?movements:records}));http.post.mockResolvedValue({});http.put.mockResolvedValue({});http.delete.mockResolvedValue({});ElMessageBox.confirm.mockResolvedValue(true)})
afterEach(()=>{wrappers.splice(0).forEach(wrapper=>wrapper.unmount());vi.useRealTimers()})
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?tableStubs():Object.fromEntries(['button','icon','card','input','select','option','input-number','table','table-column','tag','dialog','form','form-item','date-picker','empty','tabs','tab-pane'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('rendered stock rows and create form expose authoritative balances and submit edited inventory',async()=>{
  const vm=await setup(true),wrapper=wrappers.at(-1);expect(wrapper.text()).toContain('网线');expect(wrapper.text()).toContain('低库存')
  vm.openCreateDialog();await flushPromises();const fields=wrapper.findAll('[data-element="form-item"]');const find=label=>fields.find(item=>item.find('label').text()===label)
  await find('耗材名称').find('input').setValue('新耗材');vm.inventoryForm.category='网络耗材';await flushPromises()
  const button=wrapper.findAll('button').find(button=>button.text().includes('保存'));expect(button).toBeTruthy();await button.trigger('click');await flushPromises()
  expect(http.post).toHaveBeenCalledWith(API_ENDPOINTS.consumables.list,expect.objectContaining({name:'新耗材',category:'网络耗材'}))
})
it('normalizes inventory and stock movements and computes warning metrics',async()=>{
  const vm=await setup();expect(vm.selectedRecord.id).toBe(1);expect(vm.warningItems).toHaveLength(2);expect(vm.dashboard.map(item=>item.value)).toEqual([3,12,2,1]);expect(vm.records[2]).toMatchObject({category:'未分类',status:'待补货',tagType:'danger'});expect(vm.stockRecords[0]).toMatchObject({type:'出库',consumableName:'网线',operator:'教师甲',source:'教学领用'});expect(vm.categorySelectOptions).toContain('网络耗材')
  expect(vm.normalizeStockRecord({source:'借用归还#7:12345678-1234-1234-1234-123456789abc'}).source).toBe('借用归还#7');expect(vm.normalizeConsumable({stock:-1}).stock).toBe(0)
  for(const payload of [records,{data:records},{data:{records}},{records}])expect(vm.getResponseData(payload)).toEqual(records);expect(vm.getResponseData({})).toEqual([])
})
it('filters inventory and movement history independently with inclusive dates',async()=>{
  const vm=await setup();Object.assign(vm.filters,{keyword:'a101',category:'网络耗材',status:'库存充足'});expect(vm.filteredRecords.map(row=>row.id)).toEqual([1]);vm.filters.status='低库存';expect(vm.filteredRecords).toEqual([])
  Object.assign(vm.recordFilters,{keyword:'教师甲',type:'出库',consumableId:'1',dateRange:['2026-10-18','2026-10-18']});expect(vm.filteredStockRecords).toHaveLength(1);vm.recordFilters.dateRange=['2026-10-19','2026-10-20'];expect(vm.filteredStockRecords).toEqual([])
})
it('validates required inventory fields and posts or updates normalized payloads',async()=>{
  const vm=await setup();vm.openCreateDialog();await vm.submitInventory();expect(http.post).not.toHaveBeenCalled();vm.inventoryForm.name='耗材';await vm.submitInventory();expect(http.post).not.toHaveBeenCalled();Object.assign(vm.inventoryForm,{name:' 新耗材 ',category:'其他',stock:-2});await vm.submitInventory();expect(http.post).toHaveBeenCalledWith(API_ENDPOINTS.consumables.list,expect.objectContaining({name:'新耗材',stock:0,status:'待补货'}));expect(vm.inventoryDialogVisible).toBe(false)
  vm.openEditDialog(vm.records[0]);await vm.submitInventory();expect(http.put).toHaveBeenCalledWith(`${API_ENDPOINTS.consumables.list}/1`,expect.objectContaining({name:'网线'}));expect(vm.saving).toBe(false)
})
it('server failures keep the edit dialog open and release saving state',async()=>{
  const vm=await setup();vm.openEditDialog(vm.records[0]);http.put.mockRejectedValueOnce(new Error('offline'));await expect(vm.submitInventory()).rejects.toThrow('offline');expect(vm.saving).toBe(false);expect(vm.inventoryDialogVisible).toBe(true)
  http.get.mockRejectedValueOnce(new Error('offline'));await expect(vm.loadRecords()).rejects.toThrow('offline');expect(vm.loading).toBe(false)
})
it('movement validation prevents missing, zero and excessive stock withdrawal',async()=>{
  const vm=await setup();vm.openMovementDialog('out',null);expect(vm.movementDialogVisible).toBe(false);vm.openMovementDialog('out',vm.records[0]);vm.movementForm.quantity=0;await vm.submitMovement();vm.movementForm.quantity=11;await vm.submitMovement();vm.movementForm.consumableId='missing';await vm.submitMovement();expect(http.post).not.toHaveBeenCalled()
})
it.each(['in','out'])('submits %s stock movement and refreshes authoritative inventory',async type=>{
  const vm=await setup();vm.openMovementDialog(type,vm.records[0]);Object.assign(vm.movementForm,{quantity:2,operator:' 教师甲 ',reason:' 教学用途 '});await vm.submitMovement();expect(http.post).toHaveBeenCalledWith(API_ENDPOINTS.consumables.movement(1),expect.objectContaining({type,quantity:2,operator:'教师甲',reason:'教学用途',unit:'根'}));expect(vm.movementDialogVisible).toBe(false);expect(vm.selectedId).toBe(1);expect(vm.saving).toBe(false)
})
it('delete waits for confirmation and refreshes stock history',async()=>{
  const vm=await setup();ElMessageBox.confirm.mockRejectedValueOnce('cancel');await expect(vm.deleteRecord(vm.records[0])).rejects.toBe('cancel');expect(http.delete).not.toHaveBeenCalled();await vm.deleteRecord(vm.records[0]);expect(http.delete).toHaveBeenCalledWith(`${API_ENDPOINTS.consumables.list}/1`);expect(http.get).toHaveBeenCalledWith(API_ENDPOINTS.consumables.records)
})
it('exports filtered inventory and stock movement formatting',async()=>{
  const vm=await setup();vm.filters.keyword='网线';vm.exportInventory();expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({rows:[vm.records[0]],filename:'耗材库存清单'}));vm.exportStockRecords();const options=exportTableToExcel.mock.calls[1][0];expect(options.columns.find(col=>col.label==='数量').formatter(vm.stockRecords[0])).toBe('2件');expect(options.columns.find(col=>col.label==='库存变化').formatter(vm.stockRecords[0])).toBe('12 -> 10')
})
it('imports Chinese headers, updates matching items and ignores unnamed rows',async()=>{
  const vm=await setup();readSpreadsheetRows.mockResolvedValue([{耗材名称:'网线',分类:'网络耗材',库存:'12根'},{名称:'新耗材',当前库存:3,预警线:1},{}]);const event={target:{files:[new File(['csv'],'stock.csv')],value:'chosen'}};await vm.handleImportFile(event);expect(event.target.value).toBe('');expect(http.put).toHaveBeenCalledWith(`${API_ENDPOINTS.consumables.list}/1`,expect.objectContaining({stock:12}));expect(http.post).toHaveBeenCalledWith(API_ENDPOINTS.consumables.list,expect.objectContaining({name:'新耗材',stock:3,warnThreshold:1}));expect(vm.saving).toBe(false)
  readSpreadsheetRows.mockResolvedValue([{}]);http.post.mockClear();await vm.handleImportFile(event);expect(http.post).not.toHaveBeenCalled();await vm.handleImportFile({target:{files:[],value:'chosen'}})
})
it('file selection and failed post-loan refresh remain usable',async()=>{
  const vm=await setup();vm.fileInputRef={click:vi.fn()};vm.pickImportFile();expect(vm.fileInputRef.click).toHaveBeenCalled();http.get.mockRejectedValue(new Error('offline'));await vm.loansChanged();expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('借还已登记'))
})
