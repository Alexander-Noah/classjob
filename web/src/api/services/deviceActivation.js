/*
 * 设备激活的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listLabActivationBindings、bindActivationCodeToLab、unbindActivationCodeFromLab、generateLabActivationCode、deviceActivationService。
 */
import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

/**
 * 归一化列表响应。
 * 入参：data（数据）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data?.records)) return data.data.records
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

/**
 * 归一化记录响应。
 * 入参：data（数据）；fallbackRecord（fallback记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeRecordResponse = (data, fallbackRecord = {}) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackRecord
}

/**
 * 附加消息。
 * 入参：error（错误）；fallbackMessage（fallback消息）。
 */
const withMessage = (error, fallbackMessage) => {
  error.message = error.userMessage || error.message || fallbackMessage
  throw error
}

/**
 * 列出实验室ActivationBindings。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listLabActivationBindings(params = {}) {
  try {
    const { data } = await http.get(API_ENDPOINTS.activationCodes.labBindings, { params })
    return normalizeListResponse(data)
  } catch (error) {
    withMessage(error, '实验室接入授权状态读取失败')
  }
}

/**
 * bindActivation编码目标实验室的处理逻辑。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function bindActivationCodeToLab(payload) {
  try {
    const { data } = await http.post(API_ENDPOINTS.activationCodes.bindLab, payload)
    return normalizeRecordResponse(data, payload)
  } catch (error) {
    withMessage(error, '实验室接入授权绑定失败')
  }
}

/**
 * unbindActivation编码来源实验室的处理逻辑。
 * 入参：code（编码）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function unbindActivationCodeFromLab(code) {
  try {
    const { data } = await http.post(API_ENDPOINTS.activationCodes.unbindLab(code))
    return normalizeRecordResponse(data, { code })
  } catch (error) {
    withMessage(error, '解除实验室接入授权失败')
  }
}

/**
 * generate实验室Activation编码的处理逻辑。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function generateLabActivationCode(payload) {
  try {
    const { data } = await http.post(API_ENDPOINTS.activationCodes.generate, payload)
    return normalizeRecordResponse(data, payload)
  } catch (error) {
    withMessage(error, '实验室接入码生成失败')
  }
}

export const deviceActivationService = {
  listLabActivationBindings,
  bindActivationCodeToLab,
  unbindActivationCodeFromLab,
  generateLabActivationCode,
}
