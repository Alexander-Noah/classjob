// @vitest-environment happy-dom
import { afterEach,it,expect,vi } from 'vitest'
import { defineComponent,h,ref } from 'vue'
import { mount,shallowMount,flushPromises } from '@vue/test-utils'
import { useCurrentTime } from '../../src/composables/useCurrentTime'
import { useECharts } from '../../src/composables/useECharts'
import Forbidden from '../../src/views/placeholder/Forbidden.vue'
import { panelStubs } from '../support/formStubs'
const chart=vi.hoisted(()=>({init:vi.fn(),setOption:vi.fn(),resize:vi.fn(),dispose:vi.fn()}))
const router=vi.hoisted(()=>({replace:vi.fn(),back:vi.fn()}))
vi.mock('../../src/utils/echartsCore',()=>({echarts:{init:chart.init}}))
vi.mock('vue-router',()=>({useRouter:()=>router}))
vi.mock('../../src/api/services/auth',()=>({getStoredRole:()=> 'labAdmin'}))
afterEach(()=>{vi.useRealTimers();vi.restoreAllMocks();vi.clearAllMocks()})
it('clock refreshes displayed seconds and releases its interval on unmount',async()=>{vi.useFakeTimers({toFake:['Date','setInterval','clearInterval']});vi.setSystemTime(new Date(2026,9,19,11,0,0));const w=mount(defineComponent({setup(){const {currentTime}=useCurrentTime();return()=>h('time',currentTime.value)}}));await flushPromises();const first=w.text();expect(first).toContain('2026');await vi.advanceTimersByTimeAsync(1000);expect(w.text()).not.toBe(first);expect(vi.getTimerCount()).toBe(1);w.unmount();expect(vi.getTimerCount()).toBe(0)})
it('chart initializes once, replaces options, resizes and disposes with its owner',async()=>{const instance={setOption:chart.setOption,resize:chart.resize,dispose:chart.dispose};chart.init.mockReturnValue(instance);let controls;const target=ref(null),data=ref(1);const w=mount(defineComponent({setup(){controls=useECharts(target,()=>({series:[{data:[data.value]}]}));return()=>h('div')}}));controls.resizeChart();await controls.renderChart();expect(chart.init).not.toHaveBeenCalled();target.value=w.element;await controls.renderChart();data.value=2;await controls.renderChart();expect(chart.init).toHaveBeenCalledTimes(1);expect(chart.setOption).toHaveBeenLastCalledWith({series:[{data:[2]}]},true);controls.resizeChart();expect(chart.resize).toHaveBeenCalledOnce();w.unmount();expect(chart.dispose).toHaveBeenCalledOnce()})
it('forbidden screen routes home or back according to available browser history',async()=>{const w=shallowMount(Forbidden,{global:{stubs:panelStubs()}});expect(w.text()).toContain('无权访问');await w.findAll('button')[0].trigger('click');expect(router.replace).toHaveBeenCalledWith('/lab-admin');vi.spyOn(window.history,'length','get').mockReturnValue(1);await w.findAll('button')[1].trigger('click');expect(router.replace).toHaveBeenCalledTimes(2);vi.spyOn(window.history,'length','get').mockReturnValue(2);await w.findAll('button')[1].trigger('click');expect(router.back).toHaveBeenCalled();w.unmount()})


