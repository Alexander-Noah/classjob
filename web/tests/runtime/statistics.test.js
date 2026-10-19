// @vitest-environment happy-dom
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { shallowMount, flushPromises } from '@vue/test-utils'
import Statistics from '../../src/views/statistics/DataStatistics.vue'
import http from '../../src/api/http'
import { fetchPlatformSnapshot } from '../../src/api/services/platformOverview'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage } from 'element-plus'
import { panelStubs as tableStubs } from '../support/formStubs'
const navigation = vi.hoisted(() => ({ push: vi.fn() }))
const charts = vi.hoisted(() => ({ builders: [], render: vi.fn(), resize: vi.fn() }))
let route
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => navigation }))
vi.mock('../../src/api/http', () => ({ default: { get: vi.fn() } }))
vi.mock('../../src/api/dashboard', () => ({ fetchDashboardOverview: vi.fn() }))
vi.mock('../../src/api/services/platformOverview', () => ({ fetchPlatformSnapshot: vi.fn() }))
vi.mock('../../src/utils/exportExcel', () => ({ exportTableToExcel: vi.fn(() => 10) }))
vi.mock('../../src/utils/echartsCore', () => ({ echarts: { graphic: { LinearGradient: class {} } } }))
vi.mock('../../src/composables/useECharts', () => ({ useECharts: (element, builder) => { charts.builders.push(builder); return { renderChart: charts.render, resizeChart: charts.resize } } }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn() } }))
const wrappers = []
beforeEach(() => {
  vi.resetAllMocks(); charts.builders = []; route = reactive({ meta: { roleScope: 'labAdmin' } })
  vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-10-18T12:00:00'))
  http.get.mockResolvedValue({ data: { data: [] } })
  fetchPlatformSnapshot.mockResolvedValue({ users: [], notices: [], errors: [] })
})
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()); vi.useRealTimers() })
async function setup(render = false) {
  const names = ['alert','card','col','radio-button','radio-group','row','table','table-column','tag']
  const wrapper = shallowMount(Statistics, { global: { stubs: render ? tableStubs() : Object.fromEntries(names.map(name => [`el-${name}`, true])) } }); wrappers.push(wrapper); await flushPromises(); return wrapper.vm
}
it('rendered statistics exposes empty-source summaries and role-specific platform totals',async()=>{
  const vm=await setup(true),wrapper=wrappers.at(-1);expect(wrapper.text()).toContain('业务接口联动');expect(wrapper.text()).toContain('0')
  route.meta.roleScope='systemAdmin';await flushPromises();await vm.loadStatisticsData();expect(wrapper.text()).toContain('平台')
})
it('normalizes linked response shapes and recovers from per-source read failures',async()=>{const vm=await setup();for(const data of [[1],{data:[1]},{data:{records:[1]}},{data:{items:[1]}},{records:[1]},{items:[1]},{list:[1]},{}])expect(vm.normalizeResponseList(data)).toEqual(Array.isArray(data)?data:data.data?.records||data.data?.items||data.data||data.records||data.items||data.list||[]);const warn=vi.spyOn(console,'warn').mockImplementation(()=>{});http.get.mockRejectedValueOnce(Error('offline'));expect(await vm.loadLinkedRecords('/missing')).toEqual([]);http.get.mockRejectedValueOnce(Error('offline'));expect(await vm.loadLinkedPayload('/missing')).toEqual({});warn.mockRestore();http.get.mockResolvedValueOnce({data:{data:{count:2}}});expect(await vm.loadLinkedPayload('/summary')).toEqual({count:2})})
it('empty business snapshots show zero counts and build four renderable chart options', async () => {
  const vm = await setup()
  expect(vm.summaryCards[0].value).toBe('0'); expect(vm.dataSourceLabel).toBe('业务接口联动')
  expect(vm.toolbarMetaItems).toHaveLength(3)
  const options = charts.builders.map(builder => builder())
  expect(options.map(option => option.series[0].type)).toEqual(['line','bar','pie','bar'])
  expect(options[0].series[0].data.every(value => value === 0)).toBe(true)
  vm.handleWindowResize(); expect(charts.resize).toHaveBeenCalledTimes(4)
  vm.exportStatisticsData(); expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({ rows: expect.any(Array) }))
  vm.handleBack(); expect(navigation.push).toHaveBeenCalledWith('/lab-admin')
})
it('linked statistics count matching lab usage and approved reservations and exclude deleted devices', async () => {
  const vm = await setup()
  const snapshot = {
    labs: [{id:7,labCode:'A101',labName:'机房'}],
    devices: [{labId:7,status:'正常'}, {lab:'A101',status:'故障'}, {labId:7,status:'正常',deleted:true}, {lab:'未知',status:'正常'}],
    reservations: [{labId:7,status:'已通过',date:'2026-10-18'}, {labId:7,status:'待审核',date:'2026-10-18'}],
    usageRecords: [{labId:7,useTime:'2026-10-18 08:30:00'}],
    repairs: [{labId:7,status:'待处理',device:'电脑',createdAt:'2026-10-18'}],
    courseEnvironments: [{status:'待配置',createdAt:'2026-10-18'}], templates: [{id:1}],
  }
  const data = vm.buildLinkedStatistics(snapshot,'week')
  expect(data.summary.find(card=>card.label==='实验室使用').value).toBe('2')
  expect(data.summary.find(card=>card.label==='设备在线率').value).toBe('50%')
  expect(data.compare.categories).toContain('A101 机房')
  expect(data.distribution.data).toContainEqual({name:'故障',value:1})
  expect(data.ranking.values).toEqual([1])
  expect(data.details.find(row=>row.metric==='处理中工单').value).toBe('1 单')
  vm.linkedStatistics=data
  expect(vm.buildTrendOption().series[0].data.reduce((a,b)=>a+b,0)).toBe(2)
  expect(vm.buildDistributionOption().series[0].data).toEqual(data.distribution.data)
})
it('lab resolver prioritizes identifiers and aliases without assigning unrelated records', async () => {
  const vm=await setup(); const resolve=vm.createLabResolver([{id:7,labCode:'A101',labName:'软件机房',roomNo:'101',building:'信息楼'}])
  for(const record of [{labId:7},{lab:'A101'},{classroom:'软件机房'},{labName:'信息楼101'}]) expect(resolve(record)).toBe('A101 软件机房')
  expect(resolve({lab:'未知实验室'})).toBe('')
  expect(vm.topCountRows([{name:'a'},{name:'a'},{name:'b'}],record=>record.name,1)).toEqual([{name:'a',value:2}])
  expect(vm.percentText(1,4)).toBe('25%'); expect(vm.percentText(0,0)).toBe('0%')
})
it.each(['week','month','term'])('period %s builds bounded series and expands only matching timetable occurrences',async period=>{
  const vm=await setup()
  const data=vm.buildLinkedStatistics({labs:[{id:7,code:'A101',name:'机房'}],classTimetables:[{classroom:'A101',weekday:'星期日',weekExpanded:'1-3',courseName:'课程'}],timetableConfig:{semesterStartDate:'2026-10-05'}},period)
  expect(data.trend.categories.length).toBeGreaterThan(0); expect(data.trend.categories).toHaveLength(data.trend.values.length)
  expect(data.compare.values[0]).toBeGreaterThanOrEqual(0)
  const filtered=vm.filterRecordsByPeriod([{date:'2026-10-18'},{date:'2020-01-01'}],period)
  expect(filtered.some(record=>record.date==='2020-01-01')).toBe(false)
  vm.activePeriod=period; await flushPromises(); expect(charts.render).toHaveBeenCalled()
})
it('device online state accepts explicit telemetry and safely infers asset status',async()=>{
  const vm=await setup()
  for(const record of [{online:true},{onlineStatus:'online'},{status:'正常'},{status:'借用中'}]) expect(vm.isDeviceOnline(record)).toBe(true)
  for(const record of [{online:false},{onlineStatus:'offline'},{status:'维修中'},{status:'未知'}]) expect(vm.isDeviceOnline(record)).toBe(false)
})
it('loop cards, trends and ranking normalize numerical values and preserve fallback data',async()=>{
  const vm=await setup()
  expect(vm.buildLoopCards([{label:'预约',value:2}])[0]).toMatchObject({label:'预约',value:'2'})
  expect(vm.buildLoopDetails([{title:'维修',completed:1,total:2}])[0].value).toBe('1/2')
  expect(vm.buildLoopInsights([])[0].type).toBe('success'); expect(vm.buildLoopInsights([{title:'待处理',count:2}])[0].type).toBe('warning')
  expect(vm.buildLoopTrend([{name:'周一',value:'2'}],{}).values).toEqual([2]); expect(vm.buildLoopRanking([{name:'故障',value:'3'}],{}).values).toEqual([3])
  expect(vm.buildLoopTrend([], {values:[0]})).toEqual({values:[0]})
  expect(vm.buildLoopRanking([], {values:[0]})).toEqual({values:[0]})
})
it('platform role loads account snapshot and exports real current counts',async()=>{
  route.meta.roleScope='systemAdmin'; fetchPlatformSnapshot.mockResolvedValue({users:[{id:1,roleCode:'systemAdmin',status:1}],notices:[],errors:[]})
  const vm=await setup(); expect(fetchPlatformSnapshot).toHaveBeenCalled(); expect(vm.dataSourceLabel).toBe('账号与通知接口'); expect(vm.activePeriodLabel).toBe('当前数据库快照')
  expect(vm.summaryCards.length).toBeGreaterThan(0)
  vm.handleHeroAction({command:'export'}); expect(exportTableToExcel).toHaveBeenCalled()
  vm.handleHeroAction({command:'report'}); await flushPromises(); expect(ElMessage.success).toHaveBeenCalledWith('统计报表已加入生成队列')
  vm.handleHeroAction({label:'刷新'}); expect(ElMessage.success).toHaveBeenCalledWith('刷新已处理')
  vm.handleBack(); expect(navigation.push).toHaveBeenCalledWith('/system-admin')
})

