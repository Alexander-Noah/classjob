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

it('statistics accept sparse lab aliases without attaching unrelated records',async()=>{
  const vm=await setup()
  for(const [lab,expected] of [[{},''],[{code:'L1'},'L1'],[{name:'L1 Lab',code:'L1'},'L1 Lab'],[{name:'Lab'},'Lab']])expect(vm.getLabDisplayName(lab)).toBe(expected)
  const labs=[{}, {name:'Only name'}, {id:8,name:'二号',code:'L2',building:'楼',roomNo:'203'}, {code:'X',name:'短名'}]
  const resolve=vm.createLabResolver(labs)
  expect(resolve({assignedLabId:8})).toBe('L2 二号')
  for(const field of ['lab','labName','assignedLabName','resource','classroom','roomName','location','name'])expect(resolve({[field]:'楼203'})).toBe('L2 二号')
  expect(resolve({lab:'地点：二号实验室'})).toBe('L2 二号');expect(resolve({})).toBe('');expect(resolve({labId:999,lab:'unknown'})).toBe('')
  expect(vm.createLabUsageRows(labs,[{lab:'unknown'}],resolve).every(row=>row.value===0)).toBe(true)
  for(const field of ['faultType','problemType','reason','type','device','deviceName','status'])expect(vm.getRepairName({[field]:'network'})).toBe('network')
})
it('timetable parsing supports ranges, individual weeks, fallback weeks and weekday aliases',async()=>{
  const vm=await setup()
  expect(vm.parseNumberList('3-1,5~6,8至9,11,0,bad')).toEqual([1,2,3,5,6,8,9,11])
  expect(vm.parseNumberList(null)).toEqual([])
  expect(vm.getTimetableWeeks({weekText:'2,4'})).toEqual([2,4]);expect(vm.getTimetableWeeks({weekRaw:'7'})).toEqual([7])
  for(const [weekday,index] of [['星期天',7],['周3',3],['第六天',6],['第七天',7],['invalid',0]])expect(vm.getTimetableWeekdayIndex({weekday})).toBe(index)
  expect(vm.parseDateText('bad')).toBeNull();expect(vm.parseDateText('2026/10/18').getDate()).toBe(18)
  expect(vm.expandTimetableOccurrences([{weekday:'invalid',weekText:'1'},{weekday:'周一',weekText:'1,40'}],'week','bad')).toEqual([])
})
it.each(['2026-01-15T12:00:00','2026-03-15T12:00:00','2026-09-15T12:00:00'])('semester ranges follow January, spring and autumn boundaries at %s',async date=>{
  vi.setSystemTime(new Date(date));const vm=await setup();const current=new Date(date)
  const expected=current.getMonth()===0?new Date(2025,7,1):current.getMonth()>=7?new Date(2026,7,1):new Date(2026,1,1)
  expect(vm.getDefaultSemesterStartDate().getTime()).toBe(expected.getTime())
  expect(vm.getPeriodRange('term').start.getTime()).toBe(expected.getTime())
  expect(vm.getTermMonths()).toEqual(current.getMonth()===0||current.getMonth()>=7?[8,9,10,11,12,1]:[2,3,4,5,6])
})
it('record dates accept every supported import alias and reject absent or non-date text',async()=>{
  const vm=await setup()
  for(const field of ['entryTime','scheduleDate','date','applyDate','reservationDate','time','useTime','createdAt','createdTime','updatedAt','updatedTime','startTime','reservationTime','inspectedAt','reportedAt'])expect(vm.parseRecordDate({[field]:'2026-10-18 09:30'}).getHours()).toBe(9)
  for(const value of ['', '-', 'bad'])expect(vm.parseRecordDate({date:value})).toBeNull()
  expect(vm.parseRecordDate({date:'10-18'}).getFullYear()).toBe(2026)
  expect(vm.inferBucketIndex({date:'bad'},'week',[])).toBe(-1)
  expect(vm.buildPeriodSeries([{date:'bad'}],'week').values).toEqual([0,0,0,0,0,0,0])
})
it('chart labels and tooltip formatters remain usable with long imported names and absent units',async()=>{
  const vm=await setup()
  expect(vm.wrapAxisLabel('short',8)).toBe('short');expect(vm.wrapAxisLabel('abcdefghij',4)).toBe('abcd\nefgh\nij')
  expect(vm.formatValue(5,'')).toBe('5');expect(vm.formatValue(5,'%')).toBe('5%')
  for(const build of [vm.buildTrendOption,vm.buildCompareOption,vm.buildRankingOption]){
    const option=build();expect(option.tooltip.valueFormatter(2)).toBeTruthy();for(const axis of [option.xAxis,option.yAxis]){const formatter=axis.axisLabel.formatter;if(typeof formatter==='function')expect(formatter('abcdefghijklmnop')).toBeTruthy();}
  }
  expect(vm.createChartRows({categories:['empty'],name:'test'},'section')).toEqual([{section:'section',name:'empty',value:'',description:'test'}])
  expect(vm.createChartRows({},'section')).toEqual([])
})
it('partial and failed live payloads preserve safe fallbacks and explicit warnings',async()=>{
  const vm=await setup()
  for(const payload of [null,{}, {value:1}, {data:{value:2}}]){http.get.mockResolvedValueOnce({data:payload});expect(await vm.loadLinkedPayload('/test')).toEqual(payload?.data||payload||{})}
  const warn=vi.spyOn(console,'warn').mockImplementation(()=>{});http.get.mockRejectedValueOnce(new Error('offline'));expect(await vm.loadLinkedPayload('/test')).toEqual({});warn.mockRestore()
  const {fetchDashboardOverview}=await import('../../src/api/dashboard');fetchDashboardOverview.mockRejectedValueOnce({});await vm.loadDashboardOverview();expect(ElMessage.warning).toHaveBeenCalledWith('实时统计加载失败，已使用本地统计口径')
  fetchDashboardOverview.mockResolvedValueOnce({businessLoop:{}});await vm.loadDashboardOverview();expect(vm.getLoopData()).toEqual({})
  vm.activePeriod='unknown';expect(vm.activePeriodLabel).toBe('近 7 天');route.meta={};expect(vm.roleScope).toBe('labAdmin')
})


it('explicit and absent loop metadata preserve backend labels without leaking undefined text',async()=>{
  const vm=await setup()
  expect(vm.buildLoopCards([{label:'empty'},{label:'zero',value:0,trend:'steady'}])).toEqual([
    {label:'empty',value:'-',trend:'后端实时闭环',iconKey:'schedule'},
    {label:'zero',value:'0',trend:'steady',iconKey:'repair'},
  ])
  expect(vm.buildLoopDetails([{title:'empty'},{title:'full',completed:2,total:3,rate:'67%',owner:'owner',status:'ready',tagType:'success'}])).toEqual([
    {metric:'empty',value:'0/0',change:'-',owner:'业务闭环',status:'已同步',tagType:'primary'},
    {metric:'full',value:'2/3',change:'67%',owner:'owner',status:'ready',tagType:'success'},
  ])
  expect(vm.buildLoopInsights([{title:'missing'}])[0].title).toBe('missing ')
  for(const invalid of [null,{},'bad']){const fallback={values:[0]};expect(vm.buildLoopTrend(invalid,fallback)).toBe(fallback);expect(vm.buildLoopRanking(invalid,fallback)).toBe(fallback)}
  expect(vm.buildLoopCards()).toEqual([]);expect(vm.buildLoopDetails()).toEqual([])
})
it('device flags retain explicit offline values and distinguish imported status aliases',async()=>{
  const vm=await setup()
  for(const field of ['online','isOnline','onlineStatus','networkStatus','connectStatus']){
    for(const value of [true,'true','1','yes','y','online','设备在线'])expect(vm.isDeviceOnline({[field]:value})).toBe(true)
    for(const value of [false,'false','0','no','n','offline','设备离线'])expect(vm.isDeviceOnline({[field]:value})).toBe(false)
  }
  for(const field of ['status','deviceStatus','assetStatus']){expect(vm.getDeviceStatus({[field]:'正常'})).toBe('正常');expect(vm.isDeviceOnline({[field]:'可用'})).toBe(true)}
  expect(vm.getDeviceStatus({})).toBe('未标记状态')
  expect(vm.isDeviceDeleted({isDeleted:true})).toBe(true);expect(vm.isDeviceDeleted({status:'已删除'})).toBe(true);expect(vm.isDeviceDeleted({})).toBe(false)
  expect(vm.topCountRows([{name:''},{name:'a'},{name:'a'},{name:'b'}],v=>v.name)).toEqual([{name:'a',value:2},{name:'b',value:1}])
})
it('export rejects truly empty statistics and handles missing optional chart and detail fields',async()=>{
  const vm=await setup();vm.linkedStatistics={summary:[],trend:{},compare:{},distribution:{},ranking:{}}
  vm.exportStatisticsData();expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的统计数据');expect(exportTableToExcel).not.toHaveBeenCalled()
  vm.linkedStatistics.details=[{metric:'sparse',value:0}];vm.exportStatisticsData()
  expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({rows:[{section:'关键明细',name:'sparse',value:0,description:''}]}))
})
it('linked snapshot exposes healthy empty-data and active workflow states independently',async()=>{
  const vm=await setup()
  const empty=vm.buildLinkedStatistics({},'week');expect(empty.details.map(row=>row.status)).toEqual(['暂无','稳定','稳定','待同步','暂无','稳定','稳定']);expect(empty.insights.map(row=>row.type)).toEqual(['info','success','info'])
  const active=vm.buildLinkedStatistics({labs:[{id:7,name:'Lab'}],usageRecords:[{labId:7,date:'2026-10-18'}],reservations:[{labId:7,date:'2026-10-18',status:'待审核'},{labId:7,date:'2026-10-18',status:'已通过'}],devices:[{labId:7,status:'故障'}],classTimetables:[{labId:7,weekday:'周日',weekText:'3'}],timetableConfig:{semesterStartDate:'2026-09-28'},repairs:[{labId:7,date:'2026-10-18',status:'待处理',faultType:'network'}],courseEnvironments:[{date:'2026-10-18',status:'待确认'}]},'week')
  expect(active.details.map(row=>row.status)).toEqual(['有数据','待审','关注','已联动','已纳入','处理中','关注']);expect(active.insights.map(row=>row.type)).toEqual(['primary','warning','warning'])
})
