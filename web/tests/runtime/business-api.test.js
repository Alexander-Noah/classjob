import { beforeEach, expect, it, vi } from 'vitest'
import http from '../../src/api/http'
import * as maintenance from '../../src/api/services/maintenanceRecords'
import * as settings from '../../src/api/services/systemSettings'
import * as scan from '../../src/api/services/scanReservation'
import * as activation from '../../src/api/services/deviceActivation'
import { fetchPlatformSnapshot } from '../../src/api/services/platformOverview'

vi.mock('../../src/api/http', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
beforeEach(() => vi.resetAllMocks())

it.each([
  [[{ id: 1, order_no: 'M-001', lab_name: '机房' }]],
  [{ records: [{ id: 1, order_no: 'M-001', lab_name: '机房' }] }],
  [{ items: [{ id: 1, order_no: 'M-001', lab_name: '机房' }] }],
])('maintenance lists normalize legacy fields for supported response %j', async data => {
  http.get.mockResolvedValue({ data: { code: 200, data } })
  expect(await maintenance.listMaintenanceRecords({ labId: 7 })).toMatchObject([{ id: 1, recordNo: 'M-001', lab: '机房' }])
  expect(http.get).toHaveBeenCalledWith('/maintenance-records', { params: { labId: 7 } })
})

it('maintenance CRUD preserves normalized server records and routes', async () => {
  const record = { id: 3, description: '更换零件', result_desc: '完成' }
  for (const method of ['get', 'post', 'put']) http[method].mockResolvedValue({ data: { data: record } })
  for (const result of [await maintenance.getMaintenanceRecord(3), await maintenance.createMaintenanceRecord({ content: 'draft' }), await maintenance.updateMaintenanceRecord(3, { content: 'edit' })]) {
    expect(result).toMatchObject({ id: 3, content: '更换零件', result: '完成' })
  }
  expect(http.get).toHaveBeenCalledWith('/maintenance-records/3')
  expect(http.post).toHaveBeenCalledWith('/maintenance-records', { content: 'draft' })
  expect(http.put).toHaveBeenCalledWith('/maintenance-records/3', { content: 'edit' })
  http.delete.mockResolvedValue({ data: { code: 200, data: { deleted: true } } })
  expect(await maintenance.deleteMaintenanceRecord(3)).toEqual({ deleted: true })
  expect(http.delete).toHaveBeenCalledWith('/maintenance-records/3')
})

it('maintenance rejects business failures and treats malformed lists as empty', async () => {
  http.get.mockResolvedValueOnce({ data: { code: 400, msg: '维护记录无效' } })
  await expect(maintenance.listMaintenanceRecords()).rejects.toThrow('维护记录无效')
  http.get.mockResolvedValueOnce({ data: { code: 500 } })
  await expect(maintenance.getMaintenanceRecord(1)).rejects.toThrow('维护记录接口调用失败')
  http.get.mockResolvedValueOnce({ data: {} })
  expect(await maintenance.listMaintenanceRecords()).toEqual([])
})

it('system settings unwrap data and honor fallback only for absent settings', async () => {
  http.get.mockResolvedValueOnce({ data: { data: { title: '平台' } } }).mockResolvedValueOnce({ data: { title: '直接对象' } }).mockResolvedValueOnce({ data: { code: 200 } })
  expect(await settings.getSystemSettings()).toEqual({ title: '平台' })
  expect(await settings.getSystemSettings()).toEqual({ title: '直接对象' })
  expect(await settings.getSystemSettings({ title: '默认' })).toEqual({ title: '默认' })
  http.put.mockResolvedValue({ data: { code: 200 } })
  expect(await settings.saveSystemSettings({ title: '新名称' })).toEqual({ title: '新名称' })
  expect(http.put).toHaveBeenCalledWith('/system-settings', { title: '新名称' })
  http.post.mockResolvedValue({ data: { data: { title: '初始' } } })
  expect(await settings.resetSystemSettings()).toEqual({ title: '初始' })
  expect(http.post).toHaveBeenCalledWith('/system-settings/reset')
})

it.each([
  ['get', 'getSystemSettings', '获取系统配置失败'],
  ['put', 'saveSystemSettings', '保存系统配置失败'],
  ['post', 'resetSystemSettings', '恢复系统配置失败'],
])('settings %s propagates business failures', async (method, name, fallback) => {
  http[method].mockResolvedValueOnce({ data: { code: 500 } }).mockResolvedValueOnce({ data: { code: 400, message: '无权操作' } })
  await expect(settings[name]({})).rejects.toThrow(fallback)
  await expect(settings[name]({})).rejects.toThrow('无权操作')
})

it('scan reservations pass profile code and status contact as query parameters', async () => {
  http.get.mockResolvedValue({ data: { data: { id: 7 } } })
  expect(await scan.getScanReservationProfile({ labId: 7, code: 'code' })).toEqual({ id: 7 })
  expect(http.get).toHaveBeenCalledWith('/public/labs/7/reservation-profile', { params: { code: 'code' } })
  await scan.getScanReservationStatus({ id: 9, contact: '123' })
  expect(http.get).toHaveBeenCalledWith('/public/reservations/9/status', { params: { contact: '123' } })
  http.post.mockResolvedValue({ data: { code: 200, data: { id: 9, status: '待审批' } } })
  expect(await scan.submitScanReservation({ labId: 7 })).toEqual({ id: 9, status: '待审批' })
  expect(http.post).toHaveBeenCalledWith('/public/reservations/scan', { labId: 7 })
  http.post.mockResolvedValueOnce({ data: { code: 400, message: '预约冲突' } }).mockResolvedValueOnce({ data: { code: 400 } })
  await expect(scan.submitScanReservation({})).rejects.toThrow('预约冲突')
  await expect(scan.submitScanReservation({})).rejects.toThrow('扫码预约接口调用失败')
})

it.each([[[]], [{ records: [] }], [{ data: [] }], [{ data: { records: [] } }], [{}]])('activation lists handle response %j', async data => {
  http.get.mockResolvedValue({ data })
  expect(await activation.listLabActivationBindings()).toEqual([])
  expect(http.get).toHaveBeenCalledWith('/activation-codes/lab-bindings', { params: {} })
})

it('activation bindings encode codes and return server objects or documented fallbacks', async () => {
  http.post.mockResolvedValueOnce({ data: { data: { id: 8 } } })
  expect(await activation.bindActivationCodeToLab({ code: 'test', labId: 7 })).toEqual({ id: 8 })
  expect(http.post).toHaveBeenCalledWith('/activation-codes/bind-lab', { code: 'test', labId: 7 })
  http.post.mockResolvedValueOnce({ data: { code: 200 } })
  expect(await activation.unbindActivationCodeFromLab('a/b c')).toEqual({ code: 'a/b c' })
  expect(http.post).toHaveBeenCalledWith('/activation-codes/a%2Fb%20c/unbind-lab')
  http.post.mockResolvedValueOnce({ data: { id: 9 } })
  expect(await activation.generateLabActivationCode({ labId: 7 })).toEqual({ id: 9 })
  expect(http.post).toHaveBeenCalledWith('/admin/activation-codes/generate', { labId: 7 })
})

it.each([
  ['get', 'listLabActivationBindings', '实验室接入授权状态读取失败'],
  ['post', 'bindActivationCodeToLab', '实验室接入授权绑定失败'],
  ['post', 'unbindActivationCodeFromLab', '解除实验室接入授权失败'],
  ['post', 'generateLabActivationCode', '实验室接入码生成失败'],
])('activation %s failures preserve friendly messages', async (method, name, fallback) => {
  http[method].mockRejectedValueOnce({ userMessage: '友好提示', message: 'technical' }).mockRejectedValueOnce({})
  await expect(activation[name]({})).rejects.toMatchObject({ message: '友好提示' })
  await expect(activation[name]({})).rejects.toMatchObject({ message: fallback })
})

it('platform snapshot supports independent sources and complete paginated lists', async () => {
  http.get.mockImplementation(async path => path === '/admin/users'
    ? { data: { code: '200', data: { records: [{ id: 1 }], total: '1' } } }
    : { data: { data: [{ id: 2 }] } })
  expect(await fetchPlatformSnapshot()).toEqual({ users: [{ id: 1 }], notices: [{ id: 2 }], errors: [] })
})

it.each([
  [{ code: 500 }], [{ data: { records: [{ id: 1 }], total: 2 } }], [{ data: {} }],
])('platform snapshot rejects incomplete or invalid account data %j independently', async data => {
  http.get.mockImplementation(async path => ({ data: path === '/admin/users' ? data : { data: { items: [] } } }))
  expect(await fetchPlatformSnapshot()).toEqual({ users: null, notices: [], errors: ['账号数据加载失败，请重试'] })
})

it('platform snapshot marks both network failures without fabricating totals', async () => {
  http.get.mockRejectedValue(new Error('offline'))
  expect(await fetchPlatformSnapshot()).toEqual({ users: null, notices: null, errors: ['账号数据加载失败，请重试', '通知数据加载失败，请重试'] })
})
