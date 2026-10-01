import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data?.records)) return data.data.records
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

const normalizeRecordResponse = (data, fallbackRecord = {}) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackRecord
}

const withMessage = (error, fallbackMessage) => {
  error.message = error.userMessage || error.message || fallbackMessage
  throw error
}

export async function listLabActivationBindings(params = {}) {
  try {
    const { data } = await http.get(API_ENDPOINTS.activationCodes.labBindings, { params })
    return normalizeListResponse(data)
  } catch (error) {
    withMessage(error, '实验室接入授权状态读取失败')
  }
}

export async function bindActivationCodeToLab(payload) {
  try {
    const { data } = await http.post(API_ENDPOINTS.activationCodes.bindLab, payload)
    return normalizeRecordResponse(data, payload)
  } catch (error) {
    withMessage(error, '实验室接入授权绑定失败')
  }
}

export async function unbindActivationCodeFromLab(code) {
  try {
    const { data } = await http.post(API_ENDPOINTS.activationCodes.unbindLab(code))
    return normalizeRecordResponse(data, { code })
  } catch (error) {
    withMessage(error, '解除实验室接入授权失败')
  }
}

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
