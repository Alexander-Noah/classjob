/*
 * 调课申请的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listScheduleAdjustments、createScheduleAdjustment、updateScheduleAdjustment、approveScheduleAdjustment、completeScheduleAdjustment。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

const normalizeRecordResponse = (data, fallbackRecord) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackRecord
}

export async function listScheduleAdjustments() {
  const { data } = await http.get(API_ENDPOINTS.scheduleAdjustments.list)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '获取调课申请失败')
  }
  return normalizeListResponse(data)
}

export async function createScheduleAdjustment(payload) {
  const { data } = await http.post(API_ENDPOINTS.scheduleAdjustments.list, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '提交调课申请失败')
  }
  return normalizeRecordResponse(data, payload)
}

export async function updateScheduleAdjustment(id, payload) {
  const { data } = await http.put(`${API_ENDPOINTS.scheduleAdjustments.detail}/${id}`, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '更新调课申请失败')
  }
  return normalizeRecordResponse(data, payload)
}

export async function approveScheduleAdjustment(id) {
  const { data } = await http.post(`${API_ENDPOINTS.scheduleAdjustments.detail}/${id}/approve`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '通过调课申请失败')
  }
  return normalizeRecordResponse(data, { id })
}

export async function completeScheduleAdjustment(id) {
  const { data } = await http.post(`${API_ENDPOINTS.scheduleAdjustments.detail}/${id}/complete`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '完成调课同步失败')
  }
  return normalizeRecordResponse(data, { id })
}
