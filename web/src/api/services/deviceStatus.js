/*
 * 设备运行状态的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listLabRuntimeStatus、getDeviceRuntimeStatus、listDeviceStatusEvents、reportDeviceRuntimeStatus、getDeviceStatusSummary 等。
 */
import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

const unwrapPayload = (response) => response?.data?.data ?? response?.data ?? response

const withMessage = (error, fallbackMessage) => {
  error.message = error.userMessage || error.message || fallbackMessage
  throw error
}

export async function listLabRuntimeStatus(labId, params = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.deviceStatus.lab(labId), { params }))
  } catch (error) {
    withMessage(error, '设备运行状态读取失败')
  }
}

export async function getDeviceRuntimeStatus(deviceId) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.deviceStatus.device(deviceId)))
  } catch (error) {
    withMessage(error, '设备运行状态详情读取失败')
  }
}

export async function listDeviceStatusEvents(params = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.deviceStatus.events, { params }))
  } catch (error) {
    withMessage(error, '设备状态事件读取失败')
  }
}

export async function reportDeviceRuntimeStatus(payload) {
  try {
    return unwrapPayload(await http.post(API_ENDPOINTS.deviceStatus.report, payload))
  } catch (error) {
    withMessage(error, '设备运行状态上报失败')
  }
}

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
