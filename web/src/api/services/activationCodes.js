import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data?.records)) return data.data.records
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

const normalizePageResponse = (data) => {
  if (data?.data?.records) return data.data
  if (data?.records) return data
  return {
    records: normalizeListResponse(data),
    total: normalizeListResponse(data).length,
    statistics: {},
  }
}

const normalizeRecordResponse = (data, fallbackRecord = {}) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackRecord
}

export async function listActivationCodes(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.list, { params })
  return params?.asPage ? normalizePageResponse(data) : normalizeListResponse(data)
}

export async function listLabActivationBindings(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.labBindings, { params })
  return normalizeListResponse(data)
}

export async function getActivationCodePage(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.list, { params })
  return normalizePageResponse(data)
}

export async function createActivationCodes(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.create, payload)
  return normalizeRecordResponse(data, payload)
}

export async function generateActivationCode(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.generate, payload)
  return normalizeRecordResponse(data, payload)
}

export async function updateActivationCodeStatus(id, payload) {
  const { data } = await http.put(API_ENDPOINTS.activationCodes.status(id), payload)
  return normalizeRecordResponse(data, { id, ...payload })
}

export async function updateActivationCodeRemark(id, payload) {
  const { data } = await http.put(API_ENDPOINTS.activationCodes.remark(id), payload)
  return normalizeRecordResponse(data, { id, ...payload })
}

export async function listActivationCodeBindings(id) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.bindings(id))
  return normalizeListResponse(data)
}

export async function remoteBindActivationDevice(id, payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.remoteBind(id), payload)
  return normalizeRecordResponse(data, payload)
}

export async function remoteUnbindActivationDevice(id, bindingId, payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.remoteUnbind(id, bindingId), payload)
  return normalizeRecordResponse(data, payload)
}

export async function batchOperateActivationCodes(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.batch, payload)
  return normalizeRecordResponse(data, payload)
}

export async function listActivationAuditLogs(params = {}) {
  const { data } = await http.get(API_ENDPOINTS.activationCodes.auditLogs, { params })
  return normalizeListResponse(data)
}

export async function bindActivationCodeToLab(payload) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.bindLab, payload)
  return normalizeRecordResponse(data, payload)
}

export async function unbindActivationCodeFromLab(code) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.unbindLab(code))
  return normalizeRecordResponse(data, { code })
}

export async function disableActivationCode(code) {
  const { data } = await http.post(API_ENDPOINTS.activationCodes.disable(code))
  return normalizeRecordResponse(data, { code, status: 'disabled' })
}
