// @vitest-environment happy-dom
import { afterEach,expect,it } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import Detail from '../../src/views/reservations/components/ReservationDetailPanel.vue'
import { panelStubs } from '../support/formStubs'
const wrappers=[]
afterEach(()=>wrappers.splice(0).forEach(w=>w.unmount()))
const valid={id:1,applicant:'教师甲',department:'学院',status:'待审核',date:'2099-01-04',timeRange:'08:00-10:00',attendees:20,lab:'机房'}
function setup(reservation){const w=shallowMount(Detail,{props:{reservation},global:{stubs:panelStubs()}});wrappers.push(w);return w}
it.each(['已通过','已驳回','冲突'])('reservation detail renders %s feedback, absent dates and invalid counts safely',async status=>{
 const w=setup({...valid,status,date:'',timeRange:'',department:'',attendees:'invalid',conflict:status==='冲突'})
 expect(w.text()).toContain('未填写组织');expect(w.vm.toStatisticNumber('invalid')).toBe(0)
 expect(w.findAll('.kpi-value').map(n=>n.text())).toEqual(['-','-'])
 if(status==='冲突')expect(w.text()).toContain('存在时间冲突')
 const note='审批反馈';await w.setProps({reservation:{...valid,status,note,conflict:status==='冲突'}})
 expect(w.text()).toContain(note)
 const deleteButton=w.findAll('button').find(b=>b.text()==='删除');await deleteButton.trigger('click')
 expect(w.emitted('delete')[0][0]).toMatchObject({id:1})
})
it('absent reservation hides all actions and pending records emit only the chosen action',async()=>{
 const w=setup(null);expect(w.findAll('button')).toEqual([])
 expect(w.vm.actions).toMatchObject({approve:false,reject:false,edit:false,cancel:false})
 expect(w.vm.getTooltipContent('approve')).toBeTruthy()
 await w.setProps({reservation:valid})
 for(const [label,event] of [['通过','approve'],['驳回','reject'],['编辑信息','edit'],['取消预约','cancel']]){
  const b=w.findAll('button').find(b=>b.text()===label);await b.trigger('click');expect(w.emitted(event)[0][0]).toMatchObject({id:1})
 }
})
