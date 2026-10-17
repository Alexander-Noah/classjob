/*
 * 物联网的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listIotDevices、listLabIotDevices、getLabIotOverview、createIotDevice、updateIotDevice 等。
 */
import { API_BASE_URL } from '../config'
import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

/**
 * unwrap载荷的处理逻辑。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const unwrapPayload = (response) => response?.data?.data ?? response?.data ?? response
/**
 * 构建请求选项。
 * 入参：requestOptions（请求选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const buildRequestOptions = (requestOptions = {}) => ({
  ...requestOptions,
  skipErrorMessage: Boolean(requestOptions.skipErrorMessage),
})

/**
 * 附加消息。
 * 入参：error（错误）；fallbackMessage（fallback消息）。
 */
const withMessage = (error, fallbackMessage) => {
  error.message = error.userMessage || error.message || fallbackMessage
  throw error
}

/**
 * 列出物联设备。
 * 入参：params（参数）；requestOptions（请求选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listIotDevices(params = {}, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.devices, { ...buildRequestOptions(requestOptions), params }))
  } catch (error) {
    withMessage(error, '物联设备列表读取失败')
  }
}

/**
 * 列出实验室物联设备。
 * 入参：labId（实验室标识）；requestOptions（请求选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listLabIotDevices(labId, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.labDevices(labId), buildRequestOptions(requestOptions)))
  } catch (error) {
    withMessage(error, '实验室物联设备读取失败')
  }
}

/**
 * 获取实验室物联概况。
 * 入参：labId（实验室标识）；requestOptions（请求选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getLabIotOverview(labId, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.labOverview(labId), buildRequestOptions(requestOptions)))
  } catch (error) {
    withMessage(error, '实验室物联概览读取失败')
  }
}

/**
 * 创建物联设备。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function createIotDevice(payload) {
  try {
    return unwrapPayload(await http.post(API_ENDPOINTS.iot.devices, payload))
  } catch (error) {
    withMessage(error, '新增物联设备失败')
  }
}

/**
 * 更新物联设备。
 * 入参：id（标识）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function updateIotDevice(id, payload) {
  try {
    return unwrapPayload(await http.put(`${API_ENDPOINTS.iot.devices}/${encodeURIComponent(id)}`, payload))
  } catch (error) {
    withMessage(error, '更新物联设备失败')
  }
}

/**
 * 删除物联设备。
 * 入参：id（标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function deleteIotDevice(id) {
  try {
    return unwrapPayload(await http.delete(`${API_ENDPOINTS.iot.devices}/${encodeURIComponent(id)}`))
  } catch (error) {
    withMessage(error, '删除物联设备失败')
  }
}

/**
 * control物联设备的处理逻辑。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function controlIotDevice(payload) {
  try {
    return unwrapPayload(await http.post(API_ENDPOINTS.iot.control, payload))
  } catch (error) {
    withMessage(error, '物联控制命令下发失败')
  }
}

/**
 * 列出物联命令Logs。
 * 入参：params（参数）；requestOptions（请求选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listIotCommandLogs(params = {}, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.commandLogs, { ...buildRequestOptions(requestOptions), params }))
  } catch (error) {
    withMessage(error, '物联控制日志读取失败')
  }
}

/**
 * 获取实验室环境。
 * 入参：labId（实验室标识）；requestOptions（请求选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getLabEnvironment(labId, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.environment(labId), buildRequestOptions(requestOptions)))
  } catch (error) {
    withMessage(error, '实验室环境感知数据读取失败')
  }
}

/**
 * 转换为BackendURL。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function toBackendUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//.test(path)) return path
  const baseUrl = API_BASE_URL.replace(/\/$/, '')
  const nextPath = String(path).startsWith('/') ? path : `/${path}`
  return `${baseUrl}${nextPath}`
}

export const iotService = {
  listIotDevices,
  listLabIotDevices,
  getLabIotOverview,
  createIotDevice,
  updateIotDevice,
  deleteIotDevice,
  controlIotDevice,
  listIotCommandLogs,
  getLabEnvironment,
  toBackendUrl,
}
