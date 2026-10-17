/*
 * 设备运行状态的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('device status page is scoped to terminal runtime monitoring', () => {
  const source = readSource('../src/views/device-status/DeviceStatusPage.vue')

  assert.doesNotMatch(source, /getDeviceType(Meta)?/)
  assert.doesNotMatch(source, /label="类型"|label="安装位置"/)
  assert.doesNotMatch(source, /门禁|灯光|摄像头/)

  ;[
    '主机名',
    'IP 地址',
    'MAC / 终端',
    '在线状态',
    '运行状态',
    '健康度',
    'CPU 使用率',
    '内存使用率',
    '磁盘使用率',
    '来源',
    '激活时间',
    '最近上报时间',
  ].forEach((label) => assert.match(source, new RegExp(label)))
})

test('lab admin navigation groups device modules together', () => {
  const navigationSource = readSource('../src/constants/navigation.js')
  const routeSource = readSource('../src/constants/routeModules.js')

  assert.match(navigationSource, /title:\s*'设备管理'/)
  assert.match(navigationSource, /children:\s*\['\/lab-admin\/devices',\s*'\/lab-admin\/device-status',\s*'\/lab-admin\/room-control'\]/)
  assert.doesNotMatch(routeSource, /按实验室查看灯光、门禁、摄像头/)
  assert.match(routeSource, /教师端\/实验室客户端/)
})
