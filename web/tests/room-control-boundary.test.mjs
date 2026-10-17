/*
 * 实验室控制的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { readFileSync } from 'node:fs'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('room control page uses iot endpoints instead of device assets', /** 验证当前测试用例的预期行为。 */ () => {
  const source = readSource('../src/composables/useRoomControl.js')

  assert.doesNotMatch(source, /deviceResourceService/)
  assert.doesNotMatch(source, /createIotControlFromDevice/)
  assert.doesNotMatch(source, /toServerDevicePayload/)
  assert.match(source, /iotService\.listIotDevices/)
  assert.match(source, /iotService\.listLabIotDevices/)
  assert.match(source, /iotService\.listIotCommandLogs/)
})

test('room control empty state names unconfigured iot devices clearly', /** 验证当前测试用例的预期行为。 */ () => {
  const source = readSource('../src/views/room-control/components/DeviceControlPanel.vue')

  assert.match(source, /该实验室暂未接入物联设备/)
  assert.doesNotMatch(source, /当前实验室暂无门禁、灯光、空调或温湿度设备/)
  assert.match(source, /模拟网关演示/)
  assert.match(source, /待配置真实网关/)
})

test('room control page exposes gateway mode without claiming real hardware access', /** 验证当前测试用例的预期行为。 */ () => {
  const composable = readSource('../src/composables/useRoomControl.js')
  const header = readSource('../src/views/room-control/components/RoomControlHeader.vue')
  const page = readSource('../src/views/room-control/RoomControlCenter.vue')

  assert.match(composable, /gatewayModeInfo/)
  assert.match(composable, /模拟网关演示/)
  assert.match(composable, /待配置真实网关/)
  assert.match(header, /gateway-mode/)
  assert.match(page, /:gateway-mode="gatewayModeInfo"/)
  assert.doesNotMatch(composable, /真实硬件状态/)
  assert.doesNotMatch(composable, /已同步 .*真实硬件/)
  assert.doesNotMatch(composable, /已真实接入硬件/)
  assert.doesNotMatch(header, /已真实接入硬件/)
})

test('room control does not show mocked hardware before real iot data loads', /** 验证当前测试用例的预期行为。 */ () => {
  const source = readSource('../src/composables/useRoomControl.js')
  const legacyOverview = new URL('../src/constants/roomControl.js', import.meta.url)

  assert.doesNotMatch(source, /ROOM_CONTROL_OVERVIEW/)
  assert.doesNotMatch(source, /cloneData\(ROOM_CONTROL_OVERVIEW\.labs\)/)
  assert.doesNotMatch(source, /ROOM_CONTROL_OVERVIEW\.labs\[0\]/)
  assert.equal(existsSync(legacyOverview), false)
  assert.match(source, /const selectedLabId = ref\(''\)/)
  assert.match(source, /const labs = ref\(\[\]\)/)
})
