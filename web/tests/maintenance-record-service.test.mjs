/*
 * 维护记录的自动化测试；夹具用于验证行为，不是运行时配置。
 * 主要导出：listMaintenanceRecords、getMaintenanceRecord、createMaintenanceRecord、updateMaintenanceRecord、deleteMaintenanceRecord。
 */
import { readFileSync } from 'node:fs'
import assert from 'node:assert/strict'
import { test } from 'node:test'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('maintenance record endpoints expose only CRUD paths', /** 验证当前测试用例的预期行为。 */ async () => {
  const { API_ENDPOINTS } = await import('../src/api/endpoints.js')
  const maintenanceEndpoints = API_ENDPOINTS.maintenanceRecords

  assert.deepEqual(Object.keys(maintenanceEndpoints).sort(), ['detail', 'list'])
  assert.equal(maintenanceEndpoints.list, '/maintenance-records')
  assert.equal(maintenanceEndpoints.detail('MR/7'), '/maintenance-records/MR%2F7')
  assert.doesNotMatch(JSON.stringify(maintenanceEndpoints), /assign|accept|complete|close|escalate/i)
})

test('maintenance record model maps legacy repair fields to the Task 1 DTO', /** 验证当前测试用例的预期行为。 */ async () => {
  const { normalizeMaintenanceRecord } = await import('../src/modules/maintenance-records/maintenanceRecordModel.js')

  assert.deepEqual(
    normalizeMaintenanceRecord({
      id: 7,
      ticket: 'R-007',
      labName: '软件开发实训室',
      deviceName: '联想启天M410',
      faultType: '设备故障',
      submittedAt: '2026-10-10 08:30:00',
      assignee: '张三',
      description: '更换故障硬盘',
      result: '已恢复',
      contact: '13800000000',
      lastUpdate: '2026-10-10 12:00:00',
      status: '已完成',
      progress: 100,
    }),
    {
      id: 7,
      recordNo: 'R-007',
      labId: '',
      deviceId: '',
      lab: '软件开发实训室',
      device: '联想启天M410',
      maintenanceType: '设备故障',
      maintenanceDate: '2026-10-10 08:30:00',
      maintainer: '张三',
      content: '更换故障硬盘',
      result: '已恢复',
      remark: '13800000000',
      updatedAt: '2026-10-10 12:00:00',
    },
  )
})

test('maintenance record service delegates CRUD to the server and normalizes responses', /** 验证当前测试用例的预期行为。 */ async () => {
  const serviceSource = readSource('../src/api/services/maintenanceRecords.js')
  assert.doesNotMatch(serviceSource, /useRepairManagement|localStorage|sessionStorage|API_USE_MOCK/)
  assert.doesNotMatch(serviceSource, /assign|accept|complete|close|escalate/i)
  assert.match(serviceSource, /export async function listMaintenanceRecords\(filters = \{\}\)/)
  assert.match(serviceSource, /http\.get\(API_ENDPOINTS\.maintenanceRecords\.list, \{ params: filters \}\)/)
  assert.match(serviceSource, /export async function getMaintenanceRecord\(id\)/)
  assert.match(serviceSource, /http\.get\(API_ENDPOINTS\.maintenanceRecords\.detail\(id\)\)/)
  assert.match(serviceSource, /export async function createMaintenanceRecord\(payload\)/)
  assert.match(serviceSource, /http\.post\(API_ENDPOINTS\.maintenanceRecords\.list, payload\)/)
  assert.match(serviceSource, /export async function updateMaintenanceRecord\(id, payload\)/)
  assert.match(serviceSource, /http\.put\(API_ENDPOINTS\.maintenanceRecords\.detail\(id\), payload\)/)
  assert.match(serviceSource, /export async function deleteMaintenanceRecord\(id\)/)
  assert.match(serviceSource, /http\.delete\(API_ENDPOINTS\.maintenanceRecords\.detail\(id\)\)/)
})
