// @vitest-environment happy-dom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { useRoomControl } from '../../src/composables/useRoomControl'
import { iotService } from '../../src/api/services/iot'
import { labResourceService } from '../../src/api/services/basicResources'
import { ElMessage, ElMessageBox } from 'element-plus'

vi.mock('../../src/api/services/iot', () => ({ iotService: Object.fromEntries(['listIotDevices', 'listIotCommandLogs', 'listLabIotDevices', 'controlIotDevice', 'toBackendUrl'].map(key => [key, vi.fn()])) }))
vi.mock('../../src/api/services/basicResources', () => ({ labResourceService: { list: vi.fn() } }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() }, ElMessageBox: { prompt: vi.fn() } }))
const wrappers = []
it('device and event wrapper variants normalize consistently and optional device refresh errors stay visible',async()=>{const state=await setup();const rows=devices();for(const payload of [{records:rows},{data:{devices:rows}},{data:{records:rows}},rows]){iotService.listIotDevices.mockResolvedValueOnce(payload);await state.refreshAll();expect(state.selectedLab.value.deviceCount).toBe(5)}for(const payload of [{logs:[{event:'检测'}]},{data:{records:[{event:'检测'}]}},{data:{logs:[{event:'检测'}]}},[{event:'检测'}],{}]){iotService.listIotCommandLogs.mockResolvedValueOnce(payload);await state.refreshAll();expect(Array.isArray(state.filteredRecords.value)).toBe(true)}iotService.listLabIotDevices.mockRejectedValueOnce(Error('offline'));await state.selectLab(state.labs.value[0]);await flushPromises();expect(state.selectedLabDeviceLoading.value).toBe(false)})
it('lab status normalization covers maintenance, occupied, reserved and closed rooms',async()=>{labResourceService.list.mockResolvedValue(['维护中','使用中','预约中','已关闭'].map((status,index)=>({id:index+1,labName:`机房${index}`,status})));iotService.listIotDevices.mockResolvedValue({devices:[1,2,3,4].map(id=>({...devices()[0],id,labId:id}))});const state=await setup();expect(state.labs.value.map(lab=>lab.status)).toEqual(['维护中','使用中','预约中','暂停开放']);expect(state.gatewayModeInfo.value).toHaveProperty('label')})
const devices = () => ['access_control', 'camera', 'sensor', 'light', 'air_conditioner'].map((type, index) => ({
  id: index + 1, iotCode: `IOT-${index + 1}`, iotName: ['门禁', '摄像头', '温湿度', '灯光', '空调'][index], deviceType: type,
  labId: 7, lab: '机房', status: '正常', online: true, configured: true, protocol: 'mock', streamUrl: type === 'camera' ? '/camera/stream' : '',
}))
beforeEach(() => {
  vi.resetAllMocks()
  labResourceService.list.mockResolvedValue([{ id: 7, labName: '机房', building: 'A', roomNo: '101', capacity: 40 }])
  iotService.listIotDevices.mockResolvedValue({ devices: devices() })
  iotService.listLabIotDevices.mockResolvedValue({ devices: devices() })
  iotService.listIotCommandLogs.mockResolvedValue({ records: [{ commandKey: '开门', labName: '机房', resultStatus: 'failed' }] })
  iotService.controlIotDevice.mockResolvedValue({ success: true })
  iotService.toBackendUrl.mockImplementation(url => url)
  ElMessageBox.prompt.mockResolvedValue({ value: '  学号001  ' })
  vi.spyOn(window, 'open').mockImplementation(() => null)
})
afterEach(() => { for (const wrapper of wrappers.splice(0)) wrapper.unmount(); vi.restoreAllMocks() })
async function setup() {
  let state
  const wrapper = mount(defineComponent({ setup() { state = useRoomControl(); return () => null } }))
  wrappers.push(wrapper)
  await flushPromises()
  return state
}

it('loads rooms, normalizes five hardware types and derives metrics and controls', async () => {
  const state = await setup()
  expect(state.selectedLab.value).toMatchObject({ id: '7', name: '机房', deviceCount: 5, capacity: '40 人' })
  expect(state.selectedLabDevices.value.map(device => device.type)).toEqual(['access-control', 'camera', 'sensor', 'light', 'air-conditioner'])
  expect(state.controlMetrics.value).toEqual({ total: 2, onlineCount: 2, lightCount: 1, airCount: 1 })
  expect(state.deviceCapabilityMetrics.value).toEqual({ total: 5, onlineCount: 5, configuredCount: 5, categoryCount: 5 })
  expect(state.deviceManagementMetrics.value).toEqual({ total: 5, onlineCount: 5, faultCount: 0, maintenanceCount: 0 })
  expect(state.gatewayModeInfo.value.label).toBe('模拟网关演示')
  expect(state.roomMetrics.value[1].value).toBe('5')
  expect(state.roomOptions.value).toEqual([{ label: '机房', value: '机房' }])
  expect(state.labDeviceOptions.value).toEqual(state.roomOptions.value)
  expect(state.deviceCategoryOptions.value).toHaveLength(5)
  expect(state.categoryOptions.value).toContainEqual({ label: '开门', value: '开门' })
  expect(state.selectedRisk.value.label).toBe('运行正常')
  expect(state.getControlLabel(state.selectedLabControls.value[0])).toBe('亮度')
  expect(state.getControlLabel(state.selectedLabControls.value[1])).toBe('目标温度')
  expect(state.getControlValue(state.selectedLabControls.value[0])).toBe('80%')
  state.deviceKeyword.value = 'IOT-1'
  expect(state.filteredManagedDevices.value).toHaveLength(1)
  state.deviceLabFilter.value = 'other'
  expect(state.filteredManagedDevices.value).toEqual([])
  state.deviceLabFilter.value = 'all'
  state.deviceCategoryFilter.value = 'camera'
  expect(state.filteredManagedDevices.value).toEqual([])
  state.deviceCategoryFilter.value = 'all'
  state.deviceStatusFilter.value = '故障'
  expect(state.filteredManagedDevices.value).toEqual([])
  state.keyword.value = '开门'
  state.categoryFilter.value = '开门'
  state.roomFilter.value = '机房'
  expect(state.filteredRecords.value).toHaveLength(1)
  state.roomFilter.value = 'other'
  expect(state.filteredRecords.value).toEqual([])
})

it('access open and lock commands bind room and device; gateway failure leaves a failure event', async () => {
  const state = await setup()
  await state.handleAccessCommand('远程开门')
  expect(iotService.controlIotDevice).toHaveBeenLastCalledWith(expect.objectContaining({ iotDeviceId: 1, labId: 7, action: 'open', payload: { door: '前门' }, sourceType: 'mock' }))
  expect(state.selectedLab.value.door.doorState).toBe('短时开启')
  expect(state.selectedRisk.value.label).toBe('门禁关注')
  await state.handleAccessCommand('锁定门禁')
  expect(state.selectedLab.value.door.status).toBe('已锁定')
  iotService.controlIotDevice.mockResolvedValueOnce({ success: false, response: '网关离线' })
  await state.handleAccessCommand('远程开门')
  expect(ElMessage.error).toHaveBeenCalledWith('网关离线')
  expect(state.filteredRecords.value[0]).toMatchObject({ status: '失败', tagType: 'danger' })
})

it.each([['card-enroll', 'cardNo'], ['face-enroll', 'personNo'], ['fingerprint-enroll', 'personNo'], ['student-enroll', 'studentNo']])('credential %s sends trimmed identity and required flags', async (action, field) => {
  const state = await setup()
  await state.handleAccessCredential(action)
  expect(iotService.controlIotDevice).toHaveBeenCalledWith(expect.objectContaining({ action, payload: expect.objectContaining({ [field]: '学号001', credentialValue: '学号001' }) }))
  if (action === 'student-enroll') expect(iotService.controlIotDevice.mock.calls[0][0].payload).toMatchObject({ allowStudentCard: true, allowCampusCode: true })
  expect(state.selectedLab.value.door.status).toBe('录入中')
})

it('credential cancel, prompt failure and gateway failure do not report success', async () => {
  const state = await setup()
  await state.handleAccessCredential('unknown')
  ElMessageBox.prompt.mockRejectedValueOnce('cancel').mockRejectedValueOnce(new Error('prompt error'))
  await state.handleAccessCredential('card-enroll')
  await state.handleAccessCredential('card-enroll')
  expect(iotService.controlIotDevice).not.toHaveBeenCalled()
  expect(ElMessage.error).toHaveBeenCalledWith('门禁录入信息读取失败')
  iotService.controlIotDevice.mockResolvedValueOnce({ success: false, status: '拒绝' })
  await state.handleAccessCredential('card-enroll')
  expect(ElMessage.error).toHaveBeenCalledWith('拒绝')
})

it('camera opens its backend stream and handles absent stream or browser failure', async () => {
  const state = await setup()
  await state.handleCameraView()
  expect(window.open).toHaveBeenCalledWith('/camera/stream', '_blank', 'noopener')
  expect(state.selectedLab.value.camera.status).toBe('已连接')
  state.selectedLabDevices.value.find(device => device.type === 'camera').streamUrl = ''
  await state.handleCameraView()
  expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('未提供浏览器'))
  iotService.toBackendUrl.mockImplementationOnce(() => { throw new Error('视频读取失败') })
  await state.handleCameraView()
  expect(state.selectedLab.value.camera.status).toBe('连接失败')
})

it('environment refresh updates hardware status and handles gateway outages', async () => {
  const state = await setup()
  iotService.listLabIotDevices.mockResolvedValueOnce({ devices: devices().map(device => ({ ...device, online: false })) })
  await state.handleSensorCheck()
  expect(state.selectedLab.value.status).toBe('硬件关注')
  expect(state.selectedLab.value.door.doorState).toBe('不可用')
  iotService.listLabIotDevices.mockRejectedValueOnce(new Error('离线'))
  await state.handleSensorCheck()
  expect(ElMessage.error).toHaveBeenCalledWith('离线')
  await state.refreshAll()
  expect(ElMessage.success).toHaveBeenCalledWith('已同步 1 间实验室网关状态')
})

it.each([['class', 88, 24], ['energy', 45, 26], ['security', 20, 25]])('scene %s sends both controls with scene parameters', async (preset, light, temperature) => {
  const state = await setup()
  await state.applyScenePreset(preset)
  const commands = iotService.controlIotDevice.mock.calls.map(call => call[0])
  expect(commands[0]).toMatchObject({ iotDeviceId: 4, action: 'set', payload: { value: light, enabled: true } })
  expect(commands[1]).toMatchObject({ iotDeviceId: 5, action: 'set', payload: { value: temperature, enabled: true } })
  if (preset === 'security') expect(commands[2]).toMatchObject({ iotDeviceId: 1, action: 'lock' })
  expect(state.selectedLab.value.door.status).toBe(preset === 'security' ? '已布防' : '已锁定')
})

it('scene failure restores control state and direct control failure resets to hardware values', async () => {
  const state = await setup()
  const before = JSON.parse(JSON.stringify(state.controlState))
  iotService.controlIotDevice.mockResolvedValueOnce({ success: false, response: '联动失败' })
  await state.applyScenePreset('energy')
  expect(state.controlState).toEqual(before)
  expect(ElMessage.error).toHaveBeenCalledWith('联动失败')
  const control = state.selectedLabControls.value[0]
  state.controlState[control.id].enabled = false
  await state.handleControlChange(control)
  expect(iotService.controlIotDevice).toHaveBeenLastCalledWith(expect.objectContaining({ action: 'off' }))
  state.controlState[control.id].value = 33
  iotService.controlIotDevice.mockRejectedValueOnce(new Error('控制失败'))
  await state.handleControlChange(control)
  expect(state.controlState[control.id].value).toBe(80)
  expect(ElMessage.error).toHaveBeenCalledWith('控制失败')
  await state.handleControlChange({ id: 'missing' })
})

it('capability dispatch handles switches, sliders, media, forms, sensors and failures', async () => {
  const state = await setup()
  const [access, camera, sensor, light, air] = state.selectedLabDevices.value
  await state.handleDeviceCapability(null, null)
  await state.handleDeviceCapability(sensor, sensor.capabilities[0])
  expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('尚未配置网关点位'))
  await state.handleDeviceCapability(camera, camera.capabilities[0])
  expect(window.open).toHaveBeenCalled()
  camera.streamUrl = ''
  await state.handleDeviceCapability(camera, camera.capabilities[0])
  expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('未配置可打开的视频流'))
  await state.handleDeviceCapability(light, light.capabilities[0])
  expect(iotService.controlIotDevice).toHaveBeenLastCalledWith(expect.objectContaining({ action: 'set', payload: expect.objectContaining({ enabled: true }) }))
  state.capabilityState[`${light.code}::power`].enabled = false
  await state.handleDeviceCapability(light, light.capabilities[0])
  expect(iotService.controlIotDevice).toHaveBeenLastCalledWith(expect.objectContaining({ action: 'off' }))
  await state.handleDeviceCapability(air, air.capabilities[1])
  expect(iotService.controlIotDevice).toHaveBeenLastCalledWith(expect.objectContaining({ payload: expect.objectContaining({ value: 24, unit: '°C' }) }))
  await state.handleDeviceCapability(air, air.capabilities[2])
  expect(iotService.controlIotDevice).toHaveBeenLastCalledWith(expect.objectContaining({ payload: expect.objectContaining({ mode: '制冷' }) }))
  await state.handleDeviceCapability(access, { key: 'card-enroll', type: 'form', enabled: true })
  await state.handleDeviceCapability(sensor, sensor.capabilities[2])
  iotService.controlIotDevice.mockRejectedValueOnce(new Error('命令失败'))
  await state.handleDeviceCapability(access, { key: 'status', label: '状态', enabled: true })
  expect(ElMessage.error).toHaveBeenCalledWith('命令失败')
  await state.handleDeviceCapability(access, { key: 'status', label: '状态', enabled: true })
})

it('room selection loads devices while asset mutations remain directed to the asset page', async () => {
  const state = await setup()
  state.selectLab(state.labs.value[0])
  await flushPromises()
  expect(state.roomFilter.value).toBe('机房')
  state.selectManagedDeviceLab({ labId: 7 })
  state.selectManagedDeviceLab({ labId: 999 })
  await flushPromises()
  state.handleManagedDeviceStatus({}, '故障')
  state.handleManagedDeviceOnline({}, false)
  state.inventoryManagedDevice({})
  expect(ElMessage.warning).toHaveBeenCalledTimes(3)
})

it('missing rooms and hardware are handled without sending commands', async () => {
  labResourceService.list.mockResolvedValue([])
  iotService.listIotDevices.mockRejectedValue(new Error('not configured'))
  iotService.listIotCommandLogs.mockRejectedValue(new Error('not configured'))
  const state = await setup()
  expect(state.selectedRisk.value.label).toBe('未选择')
  expect(state.selectedLabControls.value).toEqual([])
  expect(state.gatewayModeInfo.value.label).toBe('待配置真实网关')
  await state.handleAccessCommand('远程开门')
  await state.handleAccessCredential('card-enroll')
  await state.handleCameraView()
  await state.handleSensorCheck()
  await state.applyScenePreset('energy')
  await state.refreshAll()
  expect(iotService.controlIotDevice).not.toHaveBeenCalled()
  expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('物联设备已刷新'))
})

it('room without devices warns for unsupported operations and retains current view on load failure', async () => {
  iotService.listIotDevices.mockResolvedValue({})
  iotService.listLabIotDevices.mockResolvedValue({ devices: [] })
  const state = await setup()
  expect(state.selectedLab.value.status).toBe('未接入')
  await state.handleAccessCommand('远程开门')
  await state.handleAccessCredential('card-enroll')
  await state.handleCameraView()
  await state.applyScenePreset('class')
  await state.applyScenePreset('unknown')
  expect(iotService.controlIotDevice).not.toHaveBeenCalled()
  labResourceService.list.mockRejectedValueOnce(new Error('offline'))
  await state.refreshAll()
  expect(ElMessage.warning).toHaveBeenCalledWith('实验室资源加载失败，已保留当前物联控制页面')
  expect(state.selectedLab.value.name).toBe('机房')
})

