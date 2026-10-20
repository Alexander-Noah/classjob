import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/labs/LabAdminClassTimetableData.vue'
import { getClassTimetableSemesters,listClassTimetables } from '../../src/api/services/classTimetable'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage } from 'element-plus'
vi.mock('../../src/api/services/classTimetable',()=>({getClassTimetableSemesters:vi.fn(),listClassTimetables:vi.fn()}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn(rows=>rows.rows.length)}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),error:vi.fn(),warning:vi.fn()}}))
const rows=[{id:1,semester:'2026秋',className:'软件2401班',weekday:'星期二',startSection:3,endSection:4,weekExpanded:'1,3,5',classroom:'A101',courseName:'程序设计',teacher:'教师甲'}, {id:2,className:'软件2302班',weekday:'星期一',sectionCode:'1-2',weekText:'2-4',classroom:'A102',courseName:'数据库',teacher:'教师乙'},{id:3,rowClassName:'网络2401班',weekday:'周三',sectionText:'5~6',weekRaw:'1至3',classroom:'B201',courseName:'网络原理'},{id:4,className:'实验班',weekday:'未知',courseName:'其他'}]
const wrappers=[]
it('renders timetable records and connects every filter control to its query field',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('程序设计');expect(w.text()).toContain('软件2401班');const selects=w.findAll('select');const fields=['semester','timeMode','classroom','grade','major','className','weekStart','weekEnd','weekdayStart','weekdayEnd','sectionStart','sectionEnd'];for(let i=0;i<selects.length;i++){const options=selects[i].findAll('option');if(!options.length)continue;const value=options[0].element.value;await selects[i].setValue(value);expect(String(vm.filters[fields[i]])).toBe(value)}await w.get('input').setValue('程序');expect(vm.filters.keyword).toBe('程序');for(const label of ['查询','重置','刷新','导出']){await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()}expect(exportTableToExcel).toHaveBeenCalled();const pager=w.findComponent({name:'Testpagination'});pager.vm.$emit('update:current-page',2);pager.vm.$emit('update:page-size',20);expect(vm.pagination).toMatchObject({page:2,pageSize:20})})
beforeEach(()=>{vi.clearAllMocks();getClassTimetableSemesters.mockResolvedValue(['2026秋','2026春']);listClassTimetables.mockResolvedValue(rows)})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','card','icon','select','option','input','input-number','radio-group','radio-button','table','table-column','pagination'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads the newest semester and builds sorted dependent filter options',async()=>{
  const vm=await setup();expect(vm.filters.semester).toBe('2026秋');expect(listClassTimetables).toHaveBeenCalledWith(expect.objectContaining({semester:'2026秋',limit:5000}))
  expect(vm.availableClassrooms).toEqual(['A101','A102','B201']);expect(vm.availableGrades).toEqual(['23级','24级']);expect(vm.availableMajors).toContain('软件')
  expect(vm.availableWeekdays).toEqual(['星期一','星期二','周三','未知']);expect(vm.availableWeeks).toEqual([1,2,3,4,5]);expect(vm.availableSections).toEqual([1,3,5]);expect(vm.sortedRows.map(row=>row.id)).toEqual([2,1,3,4]);expect(vm.pagedRows).toHaveLength(4)
})
it.each([['classroom','a101',1],['grade','23级',2],['major','网络',3],['className','软件2401班',1],['keyword','  数据库  ',2]])('filters %s case insensitively where appropriate',async(field,value,id)=>{const vm=await setup();vm.filters[field]=value;expect(vm.filteredRows.map(row=>row.id)).toEqual([id])})
it('intersects inclusive week, weekday and section ranges including one-sided ranges',async()=>{
  const vm=await setup();Object.assign(vm.filters,{weekStart:2,weekEnd:2});expect(vm.filteredRows.map(row=>row.id)).toEqual([2,3])
  Object.assign(vm.filters,{weekStart:undefined,weekEnd:5,weekdayStart:'星期二',weekdayEnd:'周三',sectionStart:3,sectionEnd:5});expect(vm.filteredRows.map(row=>row.id)).toEqual([1])
  Object.assign(vm.filters,{weekEnd:undefined,weekdayStart:'',weekdayEnd:'星期一',sectionStart:undefined,sectionEnd:1});expect(vm.filteredRows.map(row=>row.id)).toEqual([2])
  vm.pagination.pageSize=1;expect(vm.pagedRows[0].id).toBe(2)
})
it('parses class metadata and reversed number ranges with deduplication',async()=>{
  const vm=await setup();expect(vm.resolveClassMeta('软件2401班')).toEqual({major:'软件',grade:'24级'});expect(vm.resolveClassMeta('物理1班')).toEqual({major:'物理',grade:''})
  expect(vm.parseNumberList('3-1,2,7至8,foo')).toEqual([1,2,3,7,8]);expect(vm.getRecordWeeks({weekExpanded:'',weekRaw:'2~3'})).toEqual([2,3]);expect(vm.getRecordEndSection({startSection:5})).toBe(5)
  expect(vm.compareTimetableRows({...rows[0],endSection:3},{...rows[0],endSection:4})).toBeLessThan(0)
  expect(vm.compareTimetableRows({...rows[0],courseName:'A'},{...rows[0],courseName:'B'})).toBeLessThan(0)
})
it('clears incompatible class selections and retains matching ones',async()=>{
  const vm=await setup();Object.assign(vm.filters,{major:'软件',grade:'24级',className:'软件2401班'});expect(vm.classOptions).toEqual(['软件2401班']);vm.handleMajorChange();expect(vm.filters.className).toBe('软件2401班')
  vm.filters.major='网络';vm.handleMajorChange();expect(vm.filters.className).toBe('');vm.filters.className='网络2401班';vm.filters.grade='23级';vm.handleGradeChange();expect(vm.filters.className).toBe('')
})
it('passes exact week filters to the API and restores defaults on reset',async()=>{
  const vm=await setup();Object.assign(vm.filters,{className:'软件2401班',keyword:'程序',weekStart:3,weekEnd:3});vm.pagination.page=4;await vm.loadRows();expect(listClassTimetables).toHaveBeenLastCalledWith(expect.objectContaining({week:3,className:'软件2401班',keyword:'程序'}));expect(vm.pagination.page).toBe(1)
  vm.resetFilters();await flushPromises();expect(vm.filters).toMatchObject({semester:'2026秋',className:'',keyword:'',timeMode:'default',weekStart:undefined});expect(vm.filteredRows).toHaveLength(4)
})
it('exports all filtered rows and reports empty result sets',async()=>{
  const vm=await setup();vm.exportRows();expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({filename:'校园课表',rows:vm.sortedRows,columns:expect.arrayContaining([{label:'教师',prop:'teacher'}])}));expect(ElMessage.success).toHaveBeenCalledWith('已导出 4 条校园课表')
  vm.filters.keyword='missing';vm.exportRows();expect(ElMessage.warning).toHaveBeenCalled()
})
it('releases loading state and preserves existing data after request failure',async()=>{
  const vm=await setup();listClassTimetables.mockRejectedValue(new Error('offline'));await vm.loadRows();expect(vm.rows).toEqual(rows);expect(vm.isLoading).toBe(false);expect(ElMessage.error).toHaveBeenCalledWith('offline')
  getClassTimetableSemesters.mockRejectedValue(new Error('offline'));await vm.loadSemesters();expect(vm.semesters).toEqual([])
})
it('one-sided ranges select a single boundary and never include rows missing a section',async()=>{
  const vm=await setup()
  Object.assign(vm.filters,{weekStart:3,weekEnd:undefined});expect(vm.filteredRows.map(x=>x.id)).toEqual([1,2,3])
  Object.assign(vm.filters,{weekStart:undefined,weekdayStart:'星期二',weekdayEnd:undefined});expect(vm.filteredRows.map(x=>x.id)).toEqual([1])
  Object.assign(vm.filters,{weekdayStart:'',sectionStart:3,sectionEnd:undefined});expect(vm.filteredRows.map(x=>x.id)).toEqual([1])
  Object.assign(vm.filters,{sectionStart:undefined,sectionEnd:3});expect(vm.filteredRows.map(x=>x.id)).toEqual([1])
})
it('sorting sparse legacy rows remains deterministic and absent semester is omitted from requests',async()=>{
  getClassTimetableSemesters.mockResolvedValueOnce([])
  const vm=await setup();expect(listClassTimetables).toHaveBeenLastCalledWith(expect.objectContaining({semester:undefined}))
  vm.rows=[{id:1,weekday:'unknown',startSection:3,className:'乙',courseName:'乙'},
    {id:2,weekday:'unknown',startSection:1,className:'甲',courseName:'甲'},
    {id:3,weekday:'unknown',startSection:1,className:'甲'},
    {id:4,weekday:'unknown',startSection:1,className:'乙'}]
  expect(vm.sortedRows.map(x=>x.id)).toEqual([3,2,4,1])
  expect(vm.getClassName({})).toBe('')
  listClassTimetables.mockRejectedValueOnce({});await vm.loadRows();expect(ElMessage.error).toHaveBeenCalledWith('校园课表加载失败')
})

