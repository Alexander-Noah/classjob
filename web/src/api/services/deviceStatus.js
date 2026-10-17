/*
 * 设备运行状态的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listLabRuntimeStatus、getDeviceRuntimeStatus、listDeviceStatusEvents、reportDeviceRuntimeStatus、getDeviceStatusSummary 等。
 */
import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

/**
 * unwrap载荷的处理逻辑。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const unwrapPayload = (response) => response?.data?.data ?? response?.data ?? response

/**
 * 附加消息。
 * 入参：error（错误）；fallbackMessage（fallback消息）。
 */
const withMessage = (error, fallbackMessage) => {
  error.message = error.userMessage || error.message || fallbackMessage
  throw error
}

/**
 * 列出实验室运行时状态。
 * 入参：labId（实验室标识）；params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listLabRuntimeStatus(labId, params = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.deviceStatus.lab(labId), { params }))
  } catch (error) {
    withMessage(error, '设备运行状态读取失败')
  }
}

/**
 * 获取设备运行时状态。
 * 入参：deviceId（设备标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getDeviceRuntimeStatus(deviceId) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.deviceStatus.device(deviceId)))
  } catch (error) {
    withMessage(error, '设备运行状态详情读取失败')
  }
}

/**
 * 列出设备状态事件。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listDeviceStatusEvents(params = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.deviceStatus.events, { params }))
  } catch (error) {
    withMessage(error, '设备状态事件读取失败')
  }
}

/**
 * report设备运行时状态的处理逻辑。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function reportDeviceRuntimeStatus(payload) {
  try {
    return unwrapPayload(await http.post(API_ENDPOINTS.deviceStatus.report, payload))
  } catch (error) {
    withMessage(error, '设备运行状态上报失败')
  }
}

/**
 * 获取设备状态摘要。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getDeviceStatusSummary(params = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.deviceStatus.summary, { params }))
  } catch (error) {
    withMessage(error, '设备运行状态汇总读取失败')
  }
}

export const deviceStatusService = {
  listLabRuntimeStatus,
  getDeviceRuntimeStatus,
  listDeviceStatusEvents,
  reportDeviceRuntimeStatus,
  getDeviceStatusSummary,
}
