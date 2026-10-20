// @vitest-environment happy-dom
import { expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import Board from '../../src/views/teacher/components/TeacherScheduleBoard.vue'

it('weekly board places courses by day and period and emits the selected original record', async () => {
  const courses = [
    { id: 'first', day: 'mon', title: '程序设计', time: '08:30', lab: 'A101', className: '一班' },
    { id: 'second', day: 'mon', periodStart: 3, title: '数据库', lab: 'A102' },
    { id: 'other', day: 'tue', periodStart: 1, title: '网络' },
  ]
  const w = mount(Board, { props: { activeId: 'second', courses, weekDays: [{ value: 'mon', label: '周一', date: '10/19' }, { value: 'tue', label: '周二', date: '10/20' }] }, global: { stubs: { 'el-empty': true } } })
  try {
    expect(w.findAll('.course-card')).toHaveLength(3)
    expect(w.find('.course-card.active').text()).toContain('数据库')
    expect(w.findAll('.course-cell')[0].text()).toContain('程序设计')
    expect(w.findAll('.course-cell')[1].text()).toContain('网络')
    await w.find('.course-card.active').trigger('click')
    expect(w.emitted('select')).toEqual([[courses[1]]])
    await w.setProps({ courses: [] })
    expect(w.findComponent({ name: 'el-empty' }).exists()).toBe(true)
  } finally { w.unmount() }
})

it('weekly board renders an empty schedule with default props', () => {
  const w = mount(Board, { global: { stubs: { 'el-empty': true } } })
  try { expect(w.findAll('.course-card')).toHaveLength(0); expect(w.text()).toContain('01-02节') } finally { w.unmount() }
})
