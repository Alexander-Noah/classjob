// @vitest-environment happy-dom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { deviceAssetService as service, normalizeDeviceAsset, resolveCategory, createEmptyDeviceAsset, toDeviceAssetPayload } from '../../src/api/services/deviceAssets'
import { matchDeviceLab, applyDeviceLabBinding, formatLabLabel } from '../../src/api/services/deviceLabBinding'
import { calculateDeviceAssetStats } from '../../src/api/services/deviceAssetStats'
import http from '../../src/api/http'
import { readSpreadsheetRows } from '../../src/utils/importExcel'
import { exportTableToExcel } from '../../src/utils/exportExcel'

const mode = vi.hoisted(() => ({ mock: false }))
vi.mock('../../src/api/config', () => ({ get API_USE_MOCK() { return mode.mock } }))
vi.mock('../../src/api/http', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
vi.mock('../../src/utils/importExcel', () => ({ readSpreadsheetRows: vi.fn() }))
vi.mock('../../src/utils/exportExcel', () => ({ exportTableToExcel: vi.fn(() => 1) }))
const labs = [{ id: 7, labName: '软件机房', roomNo: 'A101' }, { id: 8, labName: '网络机房', roomNo: 'B102' }]
const device = () => ({ id: 1, assetNo: 'DEV-1', deviceName: '台式电脑', labId: 7, labName: '软件机房', roomNo: 'A101', quantity: 2, status: '正常' })
beforeEach(() => {
  mode.mock = false; localStorage.clear(); vi.resetAllMocks()
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test')
  vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
})
afterEach(() => vi.restoreAllMocks())

it('device normalization accepts legacy aliases and payloads carry canonical lab metadata', () => {
  const value = normalizeDeviceAsset({ asset_no: 'OLD-1', device_name: '交换机', room_no: 'A101', lab_id: 7, lab_name: '机房', specs: '千兆', standard_requirement: '标准', quantity: 0 })
  expect(value).toMatchObject({ assetNo: 'OLD-1', deviceName: '交换机', labId: 7, labName: '机房', roomNo: 'A101', quantity: 1, category: '网络设备', specification: '千兆', standardRequirement: '标准', status: '正常', unit: '台' })
  expect(toDeviceAssetPayload(value)).toMatchObject({ deviceCode: 'OLD-1', labId: 7, labName: '机房' })
  expect(createEmptyDeviceAsset()).toMatchObject({ id: '', quantity: 1, status: '正常' })
  expect(['服务器', '路由器', '投影仪', '空调', '桌子'].map(resolveCategory)).toEqual(['计算终端', '网络设备', '显示设备', '环境设备', '其他设备'])
})

it('lab matching prioritizes IDs, rooms, names and fallback descriptions and returns independent bindings', () => {
  for (const partial of [{ labId: 7 }, { lab_id: '7' }, { location: 'a101' }, { lab: '软件机房' }, { labName: '软件机房北区' }, { installLocation: '信息楼 A101 室' }, { remark: '放在软件机房' }]) expect(matchDeviceLab(partial, labs)).toBe(labs[0])
  expect(matchDeviceLab({ labId: 8, labName: '软件机房' }, labs)).toBe(labs[1])
  expect(matchDeviceLab({}, labs)).toBeNull(); expect(matchDeviceLab({}, null)).toBeNull()
  expect(applyDeviceLabBinding({ id: 1 }, labs[0])).toMatchObject({ labId: 7, labName: '软件机房', roomNo: 'A101', location: 'A101', installLocation: 'A101' })
  const original = { id: 1 }; expect(applyDeviceLabBinding(original)).not.toBe(original)
  expect(formatLabLabel(labs[0])).toBe('A101 软件机房')
  expect(formatLabLabel({ id: 7 })).toBe('7'); expect(formatLabLabel()).toBe('未命名实验室')
  expect(calculateDeviceAssetStats([{ status: '正常' }, { status: ' 维修中 ' }, { status: '闲置' }, { status: '已报废' }, {}])).toEqual({ total: 5, normal: 2, repairing: 1, idle: 1, scrapped: 1 })
  expect(calculateDeviceAssetStats(null).total).toBe(0)
})

it.each([[[]], [{ records: [device()], total: '9' }], [{ items: [device()], total: 9 }], [[device()]]])('server lists and pages normalize supported envelopes %j', async data => {
  http.get.mockResolvedValue({ data: { data } })
  const records = await service.list({ keyword: '电脑', fallbackRecords: [] })
  expect(Array.isArray(records)).toBe(true)
  expect(http.get).toHaveBeenCalledWith('/devices', { params: { keyword: '电脑' } })
  const page = await service.page({ pageNum: 1, pageSize: 10 })
  expect(Array.isArray(page.records)).toBe(true)
  if (data.total) expect(page.total).toBe(9)
})

it('server CRUD posts normalized payloads and returns authoritative objects', async () => {
  http.get.mockResolvedValue({ data: { data: device() } })
  expect(await service.detail(1)).toMatchObject({ id: 1 })
  http.post.mockResolvedValue({ data: { data: device() } })
  http.put.mockResolvedValue({ data: { data: device() } })
  expect(await service.create(device(), labs)).toMatchObject({ id: 1, labId: 7 })
  expect(http.post).toHaveBeenCalledWith('/devices', expect.objectContaining({ labId: 7, deviceCode: 'DEV-1' }))
  await service.update(1, device(), labs)
  expect(http.put).toHaveBeenCalledWith('/devices/1', expect.objectContaining({ id: 1 }))
  await service.remove(1); expect(http.delete).toHaveBeenCalledWith('/devices/1')
  http.get.mockResolvedValueOnce({ data: { data: { total: 4 } } })
  expect(await service.stats()).toEqual({ total: 4 })
})

it('list outage only uses an explicitly supplied fallback and otherwise rejects', async () => {
  http.get.mockRejectedValue(new Error('offline'))
  expect(await service.list({ fallbackRecords: [device()] })).toMatchObject([{ id: 1 }])
  await expect(service.list()).rejects.toThrow('offline')
})

it('server inventory and transfer strip local-only fields and use the server record', async () => {
  const record = { id: 8, deviceId: 1 }
  http.post.mockResolvedValue({ data: { data: { device: device(), record, records: [record] } } })
  expect(await service.createInventoryRecord(1, { id: 'local', result: '已盘点' })).toEqual({ device: device(), record, records: [record] })
  expect(http.post).toHaveBeenCalledWith('/devices/1/inventory', { deviceId: 1, result: '已盘点' })
  await service.transfer(1, { id: 'local', currentDevice: device(), toLabId: 8 })
  expect(http.post).toHaveBeenCalledWith('/devices/1/transfer', { deviceId: 1, toLabId: 8 })
  http.get.mockResolvedValue({ data: { data: [record] } })
  expect(await service.listInventoryRecords({ deviceId: 1 })).toEqual([record])
  expect(await service.listTransferRecords()).toEqual([record])
})

it('server import sends multipart file and export revokes its download URL', async () => {
  expect(await service.importFile(null)).toEqual({ count: 0 })
  const file = new File(['sheet'], 'devices.xlsx')
  http.post.mockResolvedValue({ data: { data: { count: 2 } } })
  expect(await service.importFile(file)).toEqual({ count: 2 })
  expect(http.post.mock.calls[0][1].get('file').name).toBe('devices.xlsx')
  http.get.mockResolvedValue({ data: 'sheet', headers: { 'content-type': 'application/test' } })
  await service.export({ labId: 7 })
  expect(http.get).toHaveBeenCalledWith('/devices/export', { params: { labId: 7 }, responseType: 'blob' })
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test')
  service.downloadTemplate()
  expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({ filename: '设备资产导入模板', rows: expect.any(Array) }))
})

it('mock CRUD, filters and pagination persist copies without any network call', async () => {
  mode.mock = true
  expect((await service.list()).length).toBeGreaterThan(0)
  const saved = await service.create(device(), labs)
  expect(saved.id).toMatch(/^device-/)
  await service.update(saved.id, { ...saved, status: '闲置' }, labs)
  expect(await service.detail(saved.id)).toMatchObject({ status: '闲置' })
  expect((await service.list({ keyword: '台式', labName: '软件机房', category: '计算终端', status: '闲置' }))).toHaveLength(1)
  expect((await service.page({ pageNum: 1, pageSize: 1 })).records).toHaveLength(1)
  expect((await service.stats()).idle).toBe(1)
  await service.remove(saved.id)
  expect((await service.list()).some(value => value.id === saved.id)).toBe(false)
  expect(http.get).not.toHaveBeenCalled()
})

it('mock inventory and transfer retain history and update the asset lab', async () => {
  mode.mock = true
  const inventory = await service.createInventoryRecord(1, { result: '已盘点' })
  expect(inventory.record.id).toMatch(/^inv-/)
  expect(await service.listInventoryRecords({ deviceId: '1' })).toHaveLength(1)
  const currentDevice = await service.detail(1)
  const transfer = await service.transfer(1, { currentDevice, toLabId: 8, toLabName: '网络机房', toLocation: 'B102' })
  expect(transfer.device).toMatchObject({ labId: 8, labName: '网络机房', roomNo: 'B102' })
  expect(transfer.record).not.toHaveProperty('currentDevice')
  expect(await service.detail(1)).toMatchObject({ labId: 8 })
  expect(await service.listTransferRecords({ deviceId: 1 })).toHaveLength(1)
  expect(await service.listTransferRecords({ deviceId: 999 })).toEqual([])
})

it('mock spreadsheet import maps Chinese columns and export receives the resulting records', async () => {
  mode.mock = true
  readSpreadsheetRows.mockResolvedValue([{ 资产编号: 'NEW', 设备名称: '交换机', 实验室名称: '软件机房', 数量: 3, 执行标准: '标准' }])
  expect(await service.importFile(new File(['x'], 'import.xlsx'), labs)).toEqual({ count: 1 })
  const imported = (await service.list({ keyword: 'NEW' }))[0]
  expect(imported).toMatchObject({ assetNo: 'NEW', labId: 7, quantity: 3, standardRequirement: '标准' })
  await service.export({ keyword: 'NEW' })
  expect(exportTableToExcel).toHaveBeenCalledWith(expect.objectContaining({ rows: [imported] }))
})
