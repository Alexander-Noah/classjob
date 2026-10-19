import { expect, it } from 'vitest'
import { parseScheduleTime, parseScheduleTimeRange, getMinutesUntilStart, getScheduleStatus, isUsingNow, isNeedOpenDoor, buildDashboardScheduleView, getDoorReminderStatus, buildDoorReminderList } from '../../src/views/dashboard/dashboardSchedule'
const now=new Date(2026,9,18,10,0)
it.each(['10:00-11:00',{startTime:'10:00',endTime:'11:00'},{timeRange:'10:00-11:00'},{desc:'课程 10：00 至 11：00'}])('parses supported schedule ranges %j',item=>{
  const {start,end}=parseScheduleTime(item,now)
  expect(start).toEqual(now);expect(end).toEqual(new Date(2026,9,18,11,0))
})
it('handles explicit dates, overnight ranges and invalid clock values',()=>{
  const overnight=parseScheduleTime({date:'2026/10/17',time:'23:30-01:00'},now)
  expect(overnight.end).toEqual(new Date(2026,9,18,1));expect(overnight.start).toEqual(new Date(2026,9,17,23,30))
  expect(parseScheduleTimeRange('25:00-10:99',now)).toEqual({start:null,end:null})
  expect(getMinutesUntilStart({},now)).toBe(Infinity)
  expect(getMinutesUntilStart('10:15-11:00',now)).toBe(15)
})
it.each([['09:00-09:59','ended'],['09:00-10:00','ongoing'],['10:00-11:00','ongoing'],['10:15-11:00','soon'],['10:16-11:00','later'],['unknown','later']])('classifies range boundaries %s', (range,group)=>expect(getScheduleStatus(range,now).group).toBe(group))
it('requires a current-day valid future interval for opening reminders',()=>{
  expect(isUsingNow('10:00-11:00',now)).toBe(true)
  expect(isNeedOpenDoor('10:01-11:00',now)).toBe(true)
  for(const item of [{date:'2026-10-19',time:'10:01-11:00'},'10:00-11:00','10:16-11:00','invalid','08:00-09:00']) expect(isNeedOpenDoor(item,now)).toBe(false)
  expect(getDoorReminderStatus('10:00-11:00',now)).toEqual({status:'使用中',statusType:'success'})
  expect(getDoorReminderStatus('10:01-11:00',now)?.status).toBe('待开门')
  expect(getDoorReminderStatus('11:00-12:00',now)).toBeNull()
})
it('groups and orders current-day schedules, removes ended items and limits visible rows',()=>{
  const result=buildDashboardScheduleView([{id:1,time:'12:00-13:00'},{id:2,time:'10:15-11:00'},{id:3,time:'09:30-10:30',roomNo:'A101',title:'实验',studentClass:'一班',teacherName:'张老师'},{id:4,time:'08:00-09:00'},{date:'2026-10-19',time:'10:00-11:00'}],now,{limit:2})
  expect(result.visibleItems.map(item=>item.id)).toEqual([3,2]);expect(result.sections.map(item=>item.key)).toEqual(['ongoing','soon'])
  expect(result).toMatchObject({hasMore:true,totalActive:3,totalToday:4})
  expect(result.visibleItems[0]).toMatchObject({lab:'A101',course:'实验',className:'一班',teacher:'张老师',status:'进行中'})
  const fallback=buildDashboardScheduleView([{}],now).visibleItems[0]
  expect(fallback).toMatchObject({time:'待确认',lab:'-',course:'未命名安排',className:'-',teacher:'-'})
  expect(buildDashboardScheduleView(null,now).visibleItems).toEqual([])
})
it('merges door reminders by start time with source-specific navigation and applicant extraction',()=>{
  const result=buildDoorReminderList([{time:'10:10-11:00',classroom:'实验楼 A101',description:'程序设计 10:10-11:00 申请人：李老师',className:'软件一班'}],[{time:'09:30-10:30',roomNo:'B202',reason:'预约实验',applicantName:'王老师'}],now,{limit:1})
  expect(result).toMatchObject({totalActive:2,hasMore:true})
  expect(result.visibleItems[0]).toMatchObject({lab:'B202',title:'预约实验',person:'王老师',audience:'王老师',path:'/lab-admin/reservations',status:'使用中'})
  const schedule=buildDoorReminderList([{time:'10:10-11:00',description:'程序设计 10:10-11:00 预约人：李老师',lab:'A101'}],[],now).visibleItems[0]
  expect(schedule).toMatchObject({title:'程序设计',person:'李老师',path:'/lab-admin/class-timetable-data'})
  expect(buildDoorReminderList(null,null,now).totalActive).toBe(0)
})
