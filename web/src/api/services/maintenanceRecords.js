import http from '../http'
import { API_ENDPOINTS } from '../endpoints'
import { normalizeMaintenanceRecord } from '../../modules/maintenance-records/maintenanceRecordModel'

const unwrapResponse = (response) => {
  const payload = response?.data ?? response
  if (payload?.code !== undefined && Number(payload.code) !== 200) {
    throw new Error(payload?.message || payload?.msg || '维护记录接口调用失败')
  }
  return payload?.data ?? payload
}

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

export async function listMaintenanceRecords(filters = {}) {
  return normalizeListResponse(await http.get(API_ENDPOINTS.maintenanceRecords.list, { params: filters }))
}

export async function getMaintenanceRecord(id) {
  return normalizeMaintenanceRecord(unwrapResponse(await http.get(API_ENDPOINTS.maintenanceRecords.detail(id))))
}

export async function createMaintenanceRecord(payload) {
  return normalizeMaintenanceRecord(
    unwrapResponse(await http.post(API_ENDPOINTS.maintenanceRecords.list, payload)),
  )
}

export async function updateMaintenanceRecord(id, payload) {
  return normalizeMaintenanceRecord(
    unwrapResponse(await http.put(API_ENDPOINTS.maintenanceRecords.detail(id), payload)),
  )
}

export async function deleteMaintenanceRecord(id) {
  return unwrapResponse(await http.delete(API_ENDPOINTS.maintenanceRecords.detail(id)))
}
