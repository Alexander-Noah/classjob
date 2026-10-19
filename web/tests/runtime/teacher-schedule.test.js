import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/teacher/TeacherSchedule.vue'
import { getClassTimetableSemesters,listClassTimetables } from '../../src/api/services/classTimetable'
import { loadTeachingCalendar } from '../../src/modules/teaching-calendar/teachingCalendar'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { ElMessage } from 'element-plus'
const router=vi.hoisted(()=>({push:vi.fn()}))
vi.mock('vue-router',()=>({useRouter:()=>router}))
vi.mock('../../src/api/services/auth',()=>({getStoredUser:()=>({id:7,realName:'教师甲'}),getStoredRole:()=> 'teacher'}))
vi.mock('../../src/api/services/classTimetable',()=>({getClassTimetableSemesters:vi.fn(),listClassTimetables:vi.fn()}))
vi.mock('../../src/modules/teaching-calendar/teachingCalendar',()=>({loadTeachingCalendar:vi.fn(),getAcademicWeek:()=>7,getTeachingWeekStartDate:(calendar,week)=>new Date(2026,8,7+(week-1)*7)}))
vi.mock('../../src/utils/exportExcel',()=>({exportTableToExcel:vi.fn(options=>options.rows.length)}))
vi.mock('element-plus',()=>({ElMessage:{warning:vi.fn(),error:vi.fn(),success:vi.fn()}}))
const wrappers=[];const rows=[{id:1,teacher:'教师甲',courseName:'程序设计',weekday:'星期一',startSection:2,endSection:2,className:'软件一班',classroom:'A101'},{id:2,courseName:'数据库',weekday:'周三',sectionText:'3-4节',rowClassName:'软件二班',classroom:'A101'}]
it('renders weekly courses and supports teacher binding, keyword search and week navigation',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('程序设计');expect(w.text()).toContain('数据库');await w.get('input[placeholder="教师姓名，例如：张明"]').setValue('教师丙');await w.get('input[placeholder="关键词搜索"]').setValue('程序');expect(vm.filters).toMatchObject({teacherName:'教师丙',keyword:'程序'});for(const label of ['保存绑定','查询','重置','刷新','导出','下一周','上一周']){await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()}expect(localStorage.getItem('teacher-schedule-binding:7')).toBe('教师丙');expect(exportTableToExcel).toHaveBeenCalled();await w.findAll('select')[0].setValue('2026秋');await w.findAll('select')[1].setValue('8');expect(String(vm.filters.week)).toBe('8')})
beforeEach(()=>{vi.clearAllMocks();localStorage.clear();loadTeachingCalendar.mockResolvedValue({semester:'2026秋'});getClassTimetableSemesters.mockResolvedValue(['2026秋']);listClassTimetables.mockResolvedValue(rows)})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','icon','card','input','select','option','table','table-column','pagination','tag','empty','tooltip'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads calendar, binds current teacher and maps courses to weekday/period cells',async()=>{
  const vm=await setup();expect(vm.filters).toMatchObject({teacherName:'教师甲',semester:'2026秋',week:7});expect(listClassTimetables).toHaveBeenCalledWith(expect.objectContaining({teacher:'教师甲',week:7,limit:5000}));expect(vm.metrics.map(item=>item.value)).toEqual(['2','2','2','1']);expect(vm.getCellCourses('mon',1)[0].title).toBe('程序设计');expect(vm.getCellCourses('wed',3)[0].title).toBe('数据库');expect(vm.weekDays).toHaveLength(7);expect(vm.weekMonthLabel).toBe('10月');expect(vm.latestLoadedAt).not.toBe('');expect(vm.courseColorClass({title:''})).toBe('course-color-1');expect(vm.normalizeCourse({})).toMatchObject({title:'未命名课程',day:'mon',periodStart:1,classroom:'未标注教室'})
})
it('persisted teacher binding takes precedence and saving trims identity',async()=>{
  localStorage.setItem('teacher-schedule-binding:7','教师乙');const vm=await setup();expect(vm.filters.teacherName).toBe('教师乙');vm.filters.teacherName=' 教师丙 ';await vm.saveTeacherBinding();expect(localStorage.getItem('teacher-schedule-binding:7')).toBe('教师丙');expect(listClassTimetables).toHaveBeenLastCalledWith(expect.objectContaining({teacher:'教师丙'}));vm.filters.teacherName=' ';listClassTimetables.mockClear();await vm.saveTeacherBinding();expect(await vm.loadTeacherSchedule()).toBe(false);expect(listClassTimetables).not.toHaveBeenCalled()
})
it('week changes are clamped and filters/pagination are sent and reset consistently',async()=>{
  const vm=await setup();vm.filters.week=1;await vm.changeWeek(-1);expect(vm.filters.week).toBe(1);vm.filters.week=25;await vm.changeWeek(1);expect(vm.filters.week).toBe(25);Object.assign(vm.filters,{className:'软件一班',classroom:'A101',courseName:'程序设计',keyword:'程序'});await vm.loadTeacherSchedule();expect(listClassTimetables).toHaveBeenLastCalledWith(expect.objectContaining({className:'软件一班',classroom:'A101',courseName:'程序设计',keyword:'程序'}));vm.resetFilters();await flushPromises();expect(vm.filters).toMatchObject({className:'',classroom:'',courseName:'',keyword:''});vm.pagination.pageSize=1;vm.pagination.page=2;expect(vm.pagedRows[0].id).toBe(2)
})
it('empty and failed requests report useful messages and release loading state',async()=>{
  const vm=await setup();listClassTimetables.mockResolvedValueOnce([]);expect(await vm.loadTeacherSchedule()).toBe(true);expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('未匹配到教师'));listClassTimetables.mockRejectedValueOnce(new Error('offline'));expect(await vm.loadTeacherSchedule()).toBe(false);expect(vm.isLoading).toBe(false);expect(ElMessage.error).toHaveBeenCalledWith('offline');getClassTimetableSemesters.mockRejectedValueOnce(new Error('offline'));await vm.loadSemesters();expect(vm.semesters).toEqual([])
})
it('calendar failures stop initial schedule loading',async()=>{
  loadTeachingCalendar.mockRejectedValueOnce(new Error('calendar offline'));await setup();expect(listClassTimetables).not.toHaveBeenCalled();expect(ElMessage.error).toHaveBeenCalledWith('calendar offline')
})
it('hero synchronization/export use current rows and back returns to teacher home',async()=>{
  const vm=await setup();await vm.handleHeroAction({command:'sync'});expect(listClassTimetables).toHaveBeenCalledTimes(2);await vm.handleHeroAction({command:'export'});expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({filename:'教师甲第7周课表',rows}));vm.rows=[];vm.exportTeacherSchedule();expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的教师课表');vm.handleBack();expect(router.push).toHaveBeenCalledWith('/teacher')
})

