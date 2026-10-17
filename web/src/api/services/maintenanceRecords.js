/*
 * 维护记录的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listMaintenanceRecords、getMaintenanceRecord、createMaintenanceRecord、updateMaintenanceRecord、deleteMaintenanceRecord。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'
import { normalizeMaintenanceRecord } from '../../modules/maintenance-records/maintenanceRecordModel'

/**
 * unwrap响应的处理逻辑。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const unwrapResponse = (response) => {
  const payload = response?.data ?? response
  if (payload?.code !== undefined && Number(payload.code) !== 200) {
    throw new Error(payload?.message || payload?.msg || '维护记录接口调用失败')
  }
  return payload?.data ?? payload
}

/**
 * 归一化列表响应。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeListResponse = (response) => {
  const payload = unwrapResponse(response)
  const records = Array.isArray(payload)
    ? payload
    : Array.isArray(payload?.records)
      ? payload.records
      : Array.isArray(payload?.items)
        ? payload.items
        : []
  return records.map(normalizeMaintenanceRecord)
}

/**
 * 列出维护记录集合。
 * 入参：filters（筛选条件）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listMaintenanceRecords(filters = {}) {
  return normalizeListResponse(await http.get(API_ENDPOINTS.maintenanceRecords.list, { params: filters }))
}

/**
 * 获取维护记录。
 * 入参：id（标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getMaintenanceRecord(id) {
  return normalizeMaintenanceRecord(unwrapResponse(await http.get(API_ENDPOINTS.maintenanceRecords.detail(id))))
}

/**
 * 创建维护记录。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function createMaintenanceRecord(payload) {
  return normalizeMaintenanceRecord(
    unwrapResponse(await http.post(API_ENDPOINTS.maintenanceRecords.list, payload)),
  )
}

/**
 * 更新维护记录。
 * 入参：id（标识）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function updateMaintenanceRecord(id, payload) {
  return normalizeMaintenanceRecord(
    unwrapResponse(await http.put(API_ENDPOINTS.maintenanceRecords.detail(id), payload)),
  )
}

/**
 * 删除维护记录。
 * 入参：id（标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function deleteMaintenanceRecord(id) {
  return unwrapResponse(await http.delete(API_ENDPOINTS.maintenanceRecords.detail(id)))
}
