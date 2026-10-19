import { panelStubs } from '../support/formStubs'
// @vitest-environment happy-dom
import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import { shallowMount,flushPromises } from '@vue/test-utils'
import Page from '../../src/views/roles/TeacherPortal.vue'
import http from '../../src/api/http'
import { getClassTimetableSemesters,listClassTimetables } from '../../src/api/services/classTimetable'
import { loadTeachingCalendar } from '../../src/modules/teaching-calendar/teachingCalendar'
import { ElMessage } from 'element-plus'
const router=vi.hoisted(()=>({push:vi.fn()}))
vi.mock('vue-router',()=>({useRouter:()=>router}))
vi.mock('../../src/api/services/auth',()=>({getStoredUser:()=>({id:7,realName:'教师甲',permissions:['课表数据']})}))
vi.mock('../../src/api/http',()=>({default:{get:vi.fn()}}))
vi.mock('../../src/api/services/classTimetable',()=>({getClassTimetableSemesters:vi.fn(),listClassTimetables:vi.fn()}))
vi.mock('../../src/modules/teaching-calendar/teachingCalendar',()=>({loadTeachingCalendar:vi.fn(),getAcademicWeek:()=>7,getTeachingWeekStartDate:()=>new Date(2026,9,19)}))
vi.mock('element-plus',()=>({ElMessage:{warning:vi.fn(),error:vi.fn(),success:vi.fn()}}))
const wrappers=[]
it('rendered teacher portal binds week selection and routes full schedule',async()=>{const vm=await setup(true),w=wrappers.at(-1);expect(w.text()).toContain('教师');await w.get('select').setValue('8');await flushPromises();expect(String(vm.currentWeek)).toBe('8');await w.findAll('button').find(b=>b.text()==='进入完整课表').trigger('click');expect(router.push).toHaveBeenCalled();await w.findAll('button').find(b=>b.text()==='同步数据').trigger('click');await flushPromises();expect(listClassTimetables).toHaveBeenCalled()})
const records=[{id:1,courseName:'程序设计',weekday:'星期一',startSection:2,endSection:2,classroom:'A101',className:'软件一班'},{id:2,courseName:'数据库',weekday:'周三',sectionText:'3-4节',classroom:'B202',className:'软件二班'}]
beforeEach(()=>{vi.clearAllMocks();localStorage.clear();http.get.mockResolvedValue({data:{data:[{courseName:'程序设计',softwareRequirements:'IDEA',specialRequirements:'JDK 21',status:'待确认'}]}});listClassTimetables.mockResolvedValue(records);getClassTimetableSemesters.mockResolvedValue(['2026秋']);loadTeachingCalendar.mockResolvedValue({semester:'2026秋'})})
afterEach(()=>wrappers.splice(0).forEach(wrapper=>wrapper.unmount()))
async function setup(render=false){const wrapper=shallowMount(Page,{global:{directives:{loading:{}},stubs:render?panelStubs():Object.fromEntries(['button','icon','card','input','select','option','tag','empty','alert'].map(name=>[`el-${name}`,true]))}});wrappers.push(wrapper);await flushPromises();return wrapper.vm}
it('loads authenticated teacher calendar and joins course environment delivery details',async()=>{
  const vm=await setup();expect(vm.teacherName).toBe('教师甲');expect(vm.currentWeek).toBe(7);expect(vm.semester).toBe('2026秋');expect(vm.scheduleRecords[0]).toMatchObject({title:'程序设计',environment:'IDEA',note:'JDK 21',periodStart:1,day:'mon'});expect(vm.metrics.map(item=>item.value)).toEqual(['2','2','2','1']);expect(vm.visibleCourses).toHaveLength(2);expect(vm.weekDays[0]).toMatchObject({value:'mon',date:'10/19'});expect(vm.selectedCourse.id).toBe('teacher-course-1')
})
it('stored teacher binding overrides profile name and course selection follows visible week',async()=>{
  localStorage.setItem('teacher-schedule-binding:7','教师乙');const vm=await setup();expect(vm.teacherName).toBe('教师乙');vm.selectCourse(vm.scheduleRecords[1]);expect(vm.selectedCourse.title).toBe('数据库');vm.currentWeek=8;await flushPromises();expect(vm.selectedCourse).toBeNull();expect(vm.visibleCourses).toEqual([])
})
it('normalizes missing class and schedule fields and course section conventions',async()=>{
  const vm=await setup();expect(vm.resolvePeriodStart({sectionText:'4-5节'})).toBe(3);expect(vm.resolveSectionTime({})).toBe('待确认');expect(vm.resolveCourseTimeText({weekday:'星期一',startSection:1,endSection:2})).toBe('星期一 1-2节');expect(vm.normalizePortalCourse({})).toMatchObject({title:'未命名课程',lab:'待分配',environment:'按课程要求配置',className:'待同步班级',students:45,teacher:'教师甲'});expect(vm.normalizeListResponse([1])).toEqual([1]);expect(vm.normalizeListResponse({})).toEqual([])
})
it('empty backend schedules clear stale courses and report non-silent sync status',async()=>{
  const vm=await setup();listClassTimetables.mockResolvedValueOnce([]);await vm.syncTeacherData();expect(vm.scheduleRecords).toEqual([]);expect(vm.selectedCourse).toBeNull();expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('未匹配到'));expect(ElMessage.success).toHaveBeenCalledWith('教师周课表已同步');expect(vm.isSyncing).toBe(false)
})
it('failed synchronization retains current data and can stay silent on initial load',async()=>{
  const vm=await setup();listClassTimetables.mockRejectedValueOnce(new Error('offline'));await vm.syncTeacherData();expect(vm.scheduleRecords).toHaveLength(2);expect(ElMessage.warning).toHaveBeenCalledWith('offline');ElMessage.warning.mockClear();listClassTimetables.mockRejectedValueOnce(new Error('offline'));await vm.syncTeacherData({silent:true});expect(ElMessage.warning).not.toHaveBeenCalled();expect(vm.isSyncing).toBe(false);getClassTimetableSemesters.mockRejectedValueOnce(new Error('offline'));await vm.loadSemesters();expect(vm.semesters).toEqual([])
})
it('calendar failure prevents schedule loading and navigation opens teacher routes',async()=>{
  loadTeachingCalendar.mockRejectedValueOnce(new Error('calendar offline'));const vm=await setup();expect(listClassTimetables).not.toHaveBeenCalled();expect(ElMessage.error).toHaveBeenCalledWith('calendar offline');vm.goToSchedule();expect(router.push).toHaveBeenLastCalledWith('/teacher/schedule');vm.goToTimetableData();expect(router.push).toHaveBeenLastCalledWith('/teacher/class-timetable-data')
})

