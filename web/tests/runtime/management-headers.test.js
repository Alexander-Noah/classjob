// @vitest-environment happy-dom
import { it,expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { defineComponent,h } from 'vue'
import { panelStubs } from '../support/formStubs'
import Device from '../../src/views/devices/components/DeviceManagementHeader.vue'
import Reservation from '../../src/views/reservations/components/ReservationManagementHeader.vue'
import Usage from '../../src/views/records/components/UsageRecordsHeader.vue'
it.each([[Device,{},[['新增设备','create'],['下载模板','download-template'],['导出 Excel','export']]],[Reservation,{statusSummary:[{label:'待审核',count:3}]},[['新建预约','create'],['冲突检测','detect'],['导出审核表','export']]],[Usage,{statusSummary:[{label:'异常',count:2}]},[['筛选异常','filter-abnormal'],['导出记录','export']]]])('management header exposes its independent business commands',async(component,props,actions)=>{const stubs=panelStubs();stubs['el-upload']=defineComponent({name:'TestUpload',props:['onChange'],setup(_,{slots}){return()=>h('section',slots.default?.())}});const w=shallowMount(component,{props,global:{stubs}});try{for(const [label,event] of actions){await w.findAll('button').find(b=>b.text()===label).trigger('click');expect(w.emitted(event)).toHaveLength(1)}if(component===Device){const file=new File(['a'],'assets.csv');w.findComponent({name:'TestUpload'}).props('onChange')({raw:file});expect(w.emitted('import-file')[0]).toEqual([file]);w.vm.handleImportChange({});expect(w.emitted('import-file')).toHaveLength(1)}else expect(w.text()).toContain(props.statusSummary[0].label)}finally{w.unmount()}})
