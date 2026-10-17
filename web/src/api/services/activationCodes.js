/*
 * 激活码的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listActivationCodes、listLabActivationBindings、getActivationCodePage、createActivationCodes、generateActivationCode 等。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

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
 * 归一化页码响应。
 * 入参：data（数据）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizePageResponse = (data) => {
  if (data?.data?.records) return data.data
  if (data?.records) return data
  return {
    records: normalizeListResponse(data),
    total: normalizeListResponse(data).length,
    statistics: {},
  }
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
 * 列出Activation编码集合。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listActivationCodes(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.list, { params })
  return params?.asPage ? normalizePageResponse(data) : normalizeListResponse(data)
}

/**
 * 列出实验室ActivationBindings。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listLabActivationBindings(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.labBindings, { params })
  return normalizeListResponse(data)
}

/**
 * 获取Activation编码页码。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getActivationCodePage(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.list, { params })
  return normalizePageResponse(data)
}

/**
 * 创建Activation编码集合。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function createActivationCodes(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.create, payload)
  return normalizeRecordResponse(data, payload)
}

/**
 * generateActivation编码的处理逻辑。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function generateActivationCode(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.generate, payload)
  return normalizeRecordResponse(data, payload)
}

/**
 * 更新Activation编码状态。
 * 入参：id（标识）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function updateActivationCodeStatus(id, payload) {
  const { data } = await http.put(API_ENDPOINTS.activationCodes.status(id), payload)
  return normalizeRecordResponse(data, { id, ...payload })
}

/**
 * 更新Activation编码Remark。
 * 入参：id（标识）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function updateActivationCodeRemark(id, payload) {
  const { data } = await http.put(API_ENDPOINTS.activationCodes.remark(id), payload)
  return normalizeRecordResponse(data, { id, ...payload })
}

/**
 * 列出Activation编码Bindings。
 * 入参：id（标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listActivationCodeBindings(id) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.bindings(id))
  return normalizeListResponse(data)
}

/**
 * 远程BindActivation设备的处理逻辑。
 * 入参：id（标识）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function remoteBindActivationDevice(id, payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.remoteBind(id), payload)
  return normalizeRecordResponse(data, payload)
}

/**
 * 远程UnbindActivation设备的处理逻辑。
 * 入参：id（标识）；bindingId（binding标识）；payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function remoteUnbindActivationDevice(id, bindingId, payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.remoteUnbind(id, bindingId), payload)
  return normalizeRecordResponse(data, payload)
}

/**
 * batchOperateActivation编码集合的处理逻辑。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function batchOperateActivationCodes(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.batch, payload)
  return normalizeRecordResponse(data, payload)
}

/**
 * 列出ActivationAuditLogs。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listActivationAuditLogs(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.auditLogs, { params })
  return normalizeListResponse(data)
}

/**
 * bindActivation编码目标实验室的处理逻辑。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function bindActivationCodeToLab(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.bindLab, payload)
  return normalizeRecordResponse(data, payload)
}

/**
 * unbindActivation编码来源实验室的处理逻辑。
 * 入参：code（编码）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function unbindActivationCodeFromLab(code) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.unbindLab(code))
  return normalizeRecordResponse(data, { code })
}

/**
 * 禁用Activation编码。
 * 入参：code（编码）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function disableActivationCode(code) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.disable(code))
  return normalizeRecordResponse(data, { code, status: 'disabled' })
}
