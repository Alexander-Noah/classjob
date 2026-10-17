/*
 * 设备资产的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：normalizeDeviceAsset、resolveCategory、createEmptyDeviceAsset、toDeviceAssetPayload、deviceAssetService。
 */
import { API_USE_MOCK } from '../config'
import { API_ENDPOINTS } from '../endpoints'
import http from '../http'
import { exportTableToExcel } from '../../utils/exportExcel'
import { readSpreadsheetRows } from '../../utils/importExcel'
import { calculateDeviceAssetStats } from './deviceAssetStats'
import { applyDeviceLabBinding, matchDeviceLab } from './deviceLabBinding'

const DEVICE_STORAGE_KEY = 'smart-lab:device-assets:records:v1'
const INVENTORY_RECORD_STORAGE_KEY = 'smart-lab:device-inventory-records'
const DEVICE_TRANSFER_RECORD_STORAGE_KEY = 'smart-lab:device-transfer-records'

const STATUS_OPTIONS = ['正常', '维修中', '闲置', '已报废']
const CATEGORY_OPTIONS = ['计算终端', '网络设备', '显示设备', '环境设备', '其他设备']

const TABLE_COLUMNS = [
  { label: '资产编号', prop: 'assetNo' },
  { label: '设备名称', prop: 'deviceName' },
  { label: '设备分类', prop: 'category' },
  { label: '所属实验室', prop: 'labName' },
  { label: '安装位置', prop: 'roomNo' },
  { label: '数量', prop: 'quantity' },
  { label: '单位', prop: 'unit' },
  { label: '状态', prop: 'status' },
  { label: '规格参数', prop: 'specification' },
  { label: '执行标准', prop: 'standardRequirement' },
  { label: '备注', prop: 'remark' },
]

const TEMPLATE_ROWS = [
  {
    labName: '软件开发实训室',
    roomNo: '2-204',
    deviceName: '联想启天M410',
    category: '计算终端',
    quantity: 51,
    unit: '套',
    status: '正常',
    specification: 'Intel i5 / 8G 内存 / 1T 硬盘',
    standardRequirement: '参照 GB/T 9813.1-2016',
    remark: '',
  },
]

const MOCK_RECORDS = [
  {
    id: 1,
    assetNo: 'DEV-204-001',
    deviceName: '联想启天M410',
    category: '计算终端',
    labName: '软件开发实训室',
    roomNo: '2-204',
    quantity: 51,
    unit: '套',
    status: '正常',
    specification: 'Intel i5 / 8G 内存 / 1T 硬盘',
    standardRequirement: '台式计算机参照 GB/T 9813.1-2016',
    remark: '',
    createdAt: '2026-06-01 09:00:00',
  },
  {
    id: 2,
    assetNo: 'DEV-204-002',
    deviceName: 'TP-LINK 24口交换机',
    category: '网络设备',
    labName: '软件开发实训室',
    roomNo: '2-204',
    quantity: 1,
    unit: '台',
    status: '维修中',
    specification: '24 口千兆交换机',
    standardRequirement: '',
    remark: '端口巡检中',
    createdAt: '2026-06-01 09:10:00',
  },
]

/**
 * 复制当前操作。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const clone = (value) => JSON.parse(JSON.stringify(value))
/**
 * 裁剪文本。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const trimText = (value) => String(value ?? '').trim()
/**
 * pick的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const pick = (...values) => values.find(/** 判断当前条目是否为查找目标。 入参：value。 */ (value) => trimText(value)) ?? ''

/**
 * 读取本地记录集合。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readLocalRecords = () => {
  if (typeof window === 'undefined') return clone(MOCK_RECORDS)

  try {
    const records = JSON.parse(window.localStorage.getItem(DEVICE_STORAGE_KEY) || 'null')
    return Array.isArray(records) ? records : clone(MOCK_RECORDS)
  } catch {
    return clone(MOCK_RECORDS)
  }
}

/**
 * 写入本地记录集合。
 * 入参：records（记录集合）。
 */
const writeLocalRecords = (records) => {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(DEVICE_STORAGE_KEY, JSON.stringify(records))
}

/**
 * 读取Stored记录集合。
 * 入参：storageKey（storage键）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readStoredRecords = (storageKey) => {
  if (typeof window === 'undefined') return []

  try {
    const records = JSON.parse(window.localStorage.getItem(storageKey) || '[]')
    return Array.isArray(records) ? records : []
  } catch {
    return []
  }
}

/**
 * 写入Stored记录集合。
 * 入参：storageKey（storage键）；records（记录集合）。
 */
const writeStoredRecords = (storageKey, records) => {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(storageKey, JSON.stringify(records))
}

/**
 * unwrap载荷的处理逻辑。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const unwrapPayload = (response) => response?.data?.data ?? response?.data ?? response

/**
 * 归一化列表载荷。
 * 入参：payload（载荷）。
 * @returns 组装的键值对象。
 */
const normalizeListPayload = (payload) => {
  const value = unwrapPayload(payload)
  if (Array.isArray(value?.records)) return { records: value.records, total: Number(value.total ?? value.records.length) }
  if (Array.isArray(value?.items)) return { records: value.items, total: Number(value.total ?? value.items.length) }
  if (Array.isArray(value)) return { records: value, total: value.length }
  return { records: [], total: 0 }
}

/**
 * 归一化库存载荷。
 * 入参：payload（载荷）；fallbackRecord（fallback记录）。
 * @returns 组装的键值对象。
 */
const normalizeInventoryPayload = (payload, fallbackRecord) => {
  const value = unwrapPayload(payload) || {}
  return {
    device: value.device || null,
    record: value.record || fallbackRecord,
    records: Array.isArray(value.records) ? value.records : [],
  }
}

/**
 * 归一化Transfer载荷。
 * 入参：payload（载荷）；fallbackRecord（fallback记录）。
 * @returns 组装的键值对象。
 */
const normalizeTransferPayload = (payload, fallbackRecord) => {
  const value = unwrapPayload(payload) || {}
  return {
    device: value.device || null,
    record: value.record || fallbackRecord,
    records: Array.isArray(value.records) ? value.records : [],
  }
}

/**
 * 归一化设备资产。
 * 入参：device（设备）。
 * @returns 组装的键值对象。
 */
export function normalizeDeviceAsset(device = {}) {
  const assetNo = pick(device.assetNo, device.asset_no, device.deviceCode, device.code)
  const deviceName = pick(device.deviceName, device.device_name, device.name)
  const roomNo = pick(device.roomNo, device.room_no, device.location, device.installLocation)
  const specification = pick(device.specification, device.specs)
  const standardRequirement = pick(device.standardRequirement, device.standard_requirement)

  return {
    id: device.id ?? assetNo ?? `device-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    assetNo,
    deviceName,
    category: pick(device.category) || resolveCategory(deviceName),
    labId: device.labId ?? device.lab_id ?? '',
    labName: pick(device.labName, device.lab_name, device.lab, device.laboratoryName),
    roomNo,
    quantity: Number(device.quantity) > 0 ? Number(device.quantity) : 1,
    unit: pick(device.unit) || '台',
    status: pick(device.status) || '正常',
    specification,
    standardRequirement,
    remark: pick(device.remark),
    sourceType: pick(device.sourceType, device.source_type) || '手动录入',
    createdAt: pick(device.createdAt, device.createTime, device.created_at),
  }
}

/**
 * 解析分类。
 * 入参：deviceName（设备名称）。
 * @returns 文本结果。
 */
export function resolveCategory(deviceName = '') {
  const text = trimText(deviceName)
  if (/电脑|主机|工作站|计算机|服务器/i.test(text)) return '计算终端'
  if (/交换机|路由器|网络/i.test(text)) return '网络设备'
  if (/投影|显示器|屏幕/i.test(text)) return '显示设备'
  if (/空调|温控/i.test(text)) return '环境设备'
  return '其他设备'
}

/**
 * 创建空值设备资产。
 * @returns 组装的键值对象。
 */
export function createEmptyDeviceAsset() {
  return {
    id: '',
    assetNo: '',
    deviceName: '',
    category: '计算终端',
    labId: '',
    labName: '',
    roomNo: '',
    quantity: 1,
    unit: '台',
    status: '正常',
    specification: '',
    standardRequirement: '',
    remark: '',
  }
}

/**
 * 转换为设备资产载荷。
 * 入参：device（设备）。
 * @returns 组装的键值对象。
 */
export function toDeviceAssetPayload(device = {}) {
  const normalized = normalizeDeviceAsset(device)
  return {
    id: normalized.id,
    assetNo: normalized.assetNo,
    deviceCode: normalized.assetNo,
    deviceName: normalized.deviceName,
    category: normalized.category,
    labId: normalized.labId || null,
    labName: normalized.labName,
    laboratoryName: normalized.labName,
    roomNo: normalized.roomNo,
    location: normalized.roomNo,
    installLocation: normalized.roomNo,
    quantity: normalized.quantity,
    unit: normalized.unit,
    status: normalized.status,
    specification: normalized.specification,
    specs: normalized.specification,
    standardRequirement: normalized.standardRequirement,
    remark: normalized.remark,
    sourceType: normalized.sourceType,
  }
}

/**
 * 筛选记录集合。
 * 入参：records（记录集合）；params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const filterRecords = (records, params = {}) => {
  const keyword = trimText(params.keyword).toLowerCase()
  return records.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => {
    const device = normalizeDeviceAsset(record)
    const keywordMatched =
      !keyword ||
      [device.assetNo, device.deviceName].some(/** 判断当前条目是否满足任一匹配条件。 入参：value。 */ (value) => trimText(value).toLowerCase().includes(keyword))
    const labMatched =
      (!params.labId && !params.labName) ||
      (params.labId && String(device.labId) === String(params.labId)) ||
      (params.labName && device.labName === params.labName)
    const categoryMatched = !params.category || device.category === params.category
    const statusMatched = !params.status || device.status === params.status
    return keywordMatched && labMatched && categoryMatched && statusMatched
  })
}

export const deviceAssetService = {
  statusOptions: STATUS_OPTIONS,
  categoryOptions: CATEGORY_OPTIONS,
  tableColumns: TABLE_COLUMNS,

  /**
   * 列出当前操作。
   * 入参：params（参数）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async list(params = {}) {
    const { fallbackRecords = [], ...queryParams } = params || {}

    if (API_USE_MOCK) {
      return filterRecords(readLocalRecords(), queryParams).map(normalizeDeviceAsset)
    }

    try {
      const response = await http.get(API_ENDPOINTS.devices.list, { params: queryParams })
      return normalizeListPayload(response).records.map(normalizeDeviceAsset)
    } catch (error) {
      if (fallbackRecords.length) return fallbackRecords.map(normalizeDeviceAsset)
      throw error
    }
  },

  /**
   * 页码的处理逻辑。
   * 入参：params（参数）。
   * @returns 组装的键值对象。
   */
  async page(params = {}) {
    if (API_USE_MOCK) {
      const pageNum = Number(params.pageNum || 1)
      const pageSize = Number(params.pageSize || 10)
      const filtered = filterRecords(readLocalRecords(), params).map(normalizeDeviceAsset)
      const start = (pageNum - 1) * pageSize
      return {
        records: filtered.slice(start, start + pageSize),
        total: filtered.length,
      }
    }

    const response = await http.get(API_ENDPOINTS.devices.page, { params })
    const payload = normalizeListPayload(response)
    return {
      ...payload,
      records: payload.records.map(normalizeDeviceAsset),
    }
  },

  /**
   * stats的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async stats() {
    if (API_USE_MOCK) {
      return calculateDeviceAssetStats(readLocalRecords().map(normalizeDeviceAsset))
    }

    return unwrapPayload(await http.get(API_ENDPOINTS.devices.stats))
  },

  /**
   * 详情的处理逻辑。
   * 入参：id（标识）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async detail(id) {
    if (API_USE_MOCK) {
      return normalizeDeviceAsset(readLocalRecords().find(/** 判断当前条目是否为查找目标。 入参：record。 */ (record) => String(record.id) === String(id)))
    }

    return normalizeDeviceAsset(unwrapPayload(await http.get(API_ENDPOINTS.devices.detail(id))))
  },

  /**
   * 创建当前操作。
   * 入参：payload（载荷）；labOptions（实验室选项）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async create(payload, labOptions = []) {
    const matchedLab = matchDeviceLab(payload, labOptions)
    const nextDevice = toDeviceAssetPayload(applyDeviceLabBinding(payload, matchedLab))

    if (API_USE_MOCK) {
      const records = readLocalRecords()
      const savedDevice = normalizeDeviceAsset({
        ...nextDevice,
        id: `device-${Date.now()}`,
        assetNo: nextDevice.assetNo || `DEV-${Date.now().toString().slice(-6)}`,
      })
      writeLocalRecords([savedDevice, ...records])
      return savedDevice
    }

    return normalizeDeviceAsset(unwrapPayload(await http.post(API_ENDPOINTS.devices.list, nextDevice)))
  },

  /**
   * 更新当前操作。
   * 入参：id（标识）；payload（载荷）；labOptions（实验室选项）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async update(id, payload, labOptions = []) {
    const matchedLab = matchDeviceLab(payload, labOptions)
    const nextDevice = toDeviceAssetPayload(applyDeviceLabBinding({ ...payload, id }, matchedLab))

    if (API_USE_MOCK) {
      const records = readLocalRecords().map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) =>
        String(record.id) === String(id) ? normalizeDeviceAsset({ ...record, ...nextDevice, id }) : record,
      )
      writeLocalRecords(records)
      return normalizeDeviceAsset(records.find(/** 判断当前条目是否为查找目标。 入参：record。 */ (record) => String(record.id) === String(id)))
    }

    return normalizeDeviceAsset(unwrapPayload(await http.put(API_ENDPOINTS.devices.detail(id), nextDevice)))
  },

  /**
   * 移除当前操作。
   * 入参：id（标识）。
   */
  async remove(id) {
    if (API_USE_MOCK) {
      writeLocalRecords(readLocalRecords().filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => String(record.id) !== String(id)))
      return
    }

    await http.delete(API_ENDPOINTS.devices.detail(id))
  },

  /**
   * 导入文件。
   * 入参：file（文件）；labOptions（实验室选项）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async importFile(file, labOptions = []) {
    if (!file) return { count: 0 }

    if (API_USE_MOCK) {
      const rows = await readSpreadsheetRows(file)
      const imported = rows.map(/** 将当前条目转换为目标结果。 入参：row。 */ (row) => {
        const device = normalizeDeviceAsset({
          assetNo: row.资产编号 || row.assetNo,
          deviceName: row.设备名称 || row.deviceName || row.主要设备名称,
          category: row.设备分类 || row.category,
          labName: row.实验室名称 || row.所属实验室 || row.labName,
          roomNo: row.地点 || row.安装位置 || row.roomNo,
          quantity: row.数量 || row.quantity,
          unit: row.单位 || row.unit,
          status: row.状态 || row.status,
          specification: row.主要功能和技术要求 || row.规格参数 || row.specification,
          standardRequirement: row.执行标准和数量要求 || row.执行标准 || row.standardRequirement,
          remark: row.备注 || row.remark,
        })
        return normalizeDeviceAsset(applyDeviceLabBinding(device, matchDeviceLab(device, labOptions)))
      })
      writeLocalRecords([...imported, ...readLocalRecords()])
      return { count: imported.length }
    }

    const formData = new FormData()
    formData.append('file', file)
    return unwrapPayload(await http.post(API_ENDPOINTS.devices.import, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }))
  },

  /**
   * 导出当前操作。
   * 涉及：分配对象 URL，须配套释放。
   * 入参：params（参数）。
   * @returns 组装的键值对象。
   */
  async export(params = {}) {
    if (API_USE_MOCK) {
      const rows = filterRecords(readLocalRecords(), params).map(normalizeDeviceAsset)
      const count = exportTableToExcel({
        filename: '设备资产台账',
        sheetName: '设备资产台账',
        rows,
        columns: TABLE_COLUMNS,
      })
      return { count }
    }

    const response = await http.get(API_ENDPOINTS.devices.export, { params, responseType: 'blob' })
    const blob = new Blob([response.data], {
      type: response.headers?.['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `设备资产台账-${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    URL.revokeObjectURL(objectUrl)
    return { count: 0 }
  },

  /**
   * 列出库存记录集合。
   * 入参：params（参数）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async listInventoryRecords(params = {}) {
    if (API_USE_MOCK) {
      const records = readStoredRecords(INVENTORY_RECORD_STORAGE_KEY)
      return params.deviceId ? records.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => String(record.deviceId) === String(params.deviceId)) : records
    }

    return unwrapPayload(await http.get(API_ENDPOINTS.deviceInventoryRecords.list, { params }))
  },

  /**
   * 创建库存记录。
   * 入参：deviceId（设备标识）；payload（载荷）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async createInventoryRecord(deviceId, payload = {}) {
    const nextRecord = clone({ ...payload, deviceId })

    if (API_USE_MOCK) {
      const savedRecord = {
        ...nextRecord,
        id: nextRecord.id || `inv-${Date.now()}`,
      }
      const records = [savedRecord, ...readStoredRecords(INVENTORY_RECORD_STORAGE_KEY)]
      writeStoredRecords(INVENTORY_RECORD_STORAGE_KEY, records)
      return {
        device: null,
        record: savedRecord,
        records: records.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => String(record.deviceId) === String(deviceId)),
      }
    }

    const serverRecord = clone(nextRecord)
    delete serverRecord.id
    return normalizeInventoryPayload(await http.post(API_ENDPOINTS.devices.inventory(deviceId), serverRecord), nextRecord)
  },

  /**
   * 列出Transfer记录集合。
   * 入参：params（参数）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async listTransferRecords(params = {}) {
    if (API_USE_MOCK) {
      const records = readStoredRecords(DEVICE_TRANSFER_RECORD_STORAGE_KEY)
      return params.deviceId ? records.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => String(record.deviceId) === String(params.deviceId)) : records
    }

    return unwrapPayload(await http.get(API_ENDPOINTS.deviceTransferRecords.list, { params }))
  },

  /**
   * 调拨当前操作。
   * 入参：deviceId（设备标识）；payload（载荷）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async transfer(deviceId, payload = {}) {
    const nextRecord = clone({ ...payload, deviceId })

    if (API_USE_MOCK) {
      const currentDevice = nextRecord.currentDevice || {}
      const savedDevice = normalizeDeviceAsset({
        ...currentDevice,
        id: deviceId,
        labId: nextRecord.toLabId ?? currentDevice.labId ?? '',
        labName: nextRecord.toLabName ?? currentDevice.labName ?? currentDevice.lab ?? '',
        roomNo: nextRecord.toLocation ?? currentDevice.roomNo ?? currentDevice.location ?? '',
      })
      const savedRecord = {
        ...nextRecord,
        id: nextRecord.id || `transfer-${Date.now()}`,
        deviceId,
        deviceCode: nextRecord.deviceCode || currentDevice.assetNo || currentDevice.code || currentDevice.deviceCode || '',
        deviceName: nextRecord.deviceName || currentDevice.deviceName || currentDevice.name || '',
        transferAt: nextRecord.transferAt || new Date().toISOString().slice(0, 19).replace('T', ' '),
      }
      delete savedRecord.currentDevice

      const records = [savedRecord, ...readStoredRecords(DEVICE_TRANSFER_RECORD_STORAGE_KEY)]
      writeStoredRecords(DEVICE_TRANSFER_RECORD_STORAGE_KEY, records)
      writeLocalRecords(readLocalRecords().map(/** 将当前条目转换为目标结果。 入参：device。 */ (device) => (String(device.id) === String(deviceId) ? savedDevice : device)))
      return {
        device: savedDevice,
        record: savedRecord,
        records: records.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => String(record.deviceId) === String(deviceId)),
      }
    }

    const serverRecord = clone(nextRecord)
    delete serverRecord.id
    delete serverRecord.currentDevice
    return normalizeTransferPayload(await http.post(API_ENDPOINTS.devices.transfer(deviceId), serverRecord), nextRecord)
  },

  /**
   * 下载模板。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  downloadTemplate() {
    return exportTableToExcel({
      filename: '设备资产导入模板',
      sheetName: '设备资产导入模板',
      rows: TEMPLATE_ROWS,
      columns: [
        { label: '实验室名称', prop: 'labName' },
        { label: '地点', prop: 'roomNo' },
        { label: '设备名称', prop: 'deviceName' },
        { label: '设备分类', prop: 'category' },
        { label: '数量', prop: 'quantity' },
        { label: '单位', prop: 'unit' },
        { label: '状态', prop: 'status' },
        { label: '主要功能和技术要求', prop: 'specification' },
        { label: '执行标准和数量要求', prop: 'standardRequirement' },
        { label: '备注', prop: 'remark' },
      ],
    })
  },
}
