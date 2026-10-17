/*
 * 调课申请的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listScheduleAdjustments、createScheduleAdjustment、updateScheduleAdjustment、approveScheduleAdjustment、completeScheduleAdjustment。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

/**
 * 归一化列表响应。
 * 入参：data（数据）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

/**
 * 归一化记录响应。
 * 入参：data（数据）；fallbackRecord（fallback记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeRecordResponse = (data, fallbackRecord) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackRecord
}

/**
 * 列出课表Adjustments。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listScheduleAdjustments() {
  const { data } = await http.get(API_ENDPOINTS.scheduleAdjustments.list)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '获取调课申请失败')
  }
  return normalizeListResponse(data)
}

/**
 * 创建课表Adjustment。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function createScheduleAdjustment(payload) {
  const { data } = await http.post(API_ENDPOINTS.scheduleAdjustments.list, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '提交调课申请失败')
  }
  return normalizeRecordResponse(data, payload)
}

/**
 * 更新课表Adjustment。
 * 入参：id（标识）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function updateScheduleAdjustment(id, payload) {
  const { data } = await http.put(`${API_ENDPOINTS.scheduleAdjustments.detail}/${id}`, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '更新调课申请失败')
  }
  return normalizeRecordResponse(data, payload)
}

/**
 * 批准课表Adjustment。
 * 入参：id（标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function approveScheduleAdjustment(id) {
  const { data } = await http.post(`${API_ENDPOINTS.scheduleAdjustments.detail}/${id}/approve`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '通过调课申请失败')
  }
  return normalizeRecordResponse(data, { id })
}

/**
 * 完成课表Adjustment。
 * 入参：id（标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function completeScheduleAdjustment(id) {
  const { data } = await http.post(`${API_ENDPOINTS.scheduleAdjustments.detail}/${id}/complete`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '完成调课同步失败')
  }
  return normalizeRecordResponse(data, { id })
}
