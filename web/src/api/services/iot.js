import { API_BASE_URL } from '../config'
import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

const unwrapPayload = (response) => response?.data?.data ?? response?.data ?? response
const buildRequestOptions = (requestOptions = {}) => ({
  ...requestOptions,
  skipErrorMessage: Boolean(requestOptions.skipErrorMessage),
})

const withMessage = (error, fallbackMessage) => {
  error.message = error.userMessage || error.message || fallbackMessage
  throw error
}

export async function listIotDevices(params = {}, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.devices, { ...buildRequestOptions(requestOptions), params }))
  } catch (error) {
    withMessage(error, '物联设备列表读取失败')
  }
}

export async function listLabIotDevices(labId, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.labDevices(labId), buildRequestOptions(requestOptions)))
  } catch (error) {
    withMessage(error, '实验室物联设备读取失败')
  }
}

export async function getLabIotOverview(labId, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.labOverview(labId), buildRequestOptions(requestOptions)))
  } catch (error) {
    withMessage(error, '实验室物联概览读取失败')
  }
}

export async function createIotDevice(payload) {
  try {
    return unwrapPayload(await http.post(API_ENDPOINTS.iot.devices, payload))
  } catch (error) {
    withMessage(error, '新增物联设备失败')
  }
}

export async function updateIotDevice(id, payload) {
  try {
    return unwrapPayload(await http.put(`${API_ENDPOINTS.iot.devices}/${encodeURIComponent(id)}`, payload))
  } catch (error) {
    withMessage(error, '更新物联设备失败')
  }
}

export async function deleteIotDevice(id) {
  try {
    return unwrapPayload(await http.delete(`${API_ENDPOINTS.iot.devices}/${encodeURIComponent(id)}`))
  } catch (error) {
    withMessage(error, '删除物联设备失败')
  }
}

export async function controlIotDevice(payload) {
  try {
    return unwrapPayload(await http.post(API_ENDPOINTS.iot.control, payload))
  } catch (error) {
    withMessage(error, '物联控制命令下发失败')
  }
}

export async function listIotCommandLogs(params = {}, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.commandLogs, { ...buildRequestOptions(requestOptions), params }))
  } catch (error) {
    withMessage(error, '物联控制日志读取失败')
  }
}

export async function getLabEnvironment(labId, requestOptions = {}) {
  try {
    return unwrapPayload(await http.get(API_ENDPOINTS.iot.environment(labId), buildRequestOptions(requestOptions)))
  } catch (error) {
    withMessage(error, '实验室环境感知数据读取失败')
  }
}

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
