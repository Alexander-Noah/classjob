import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

export async function importAcademicSchedule(payload) {
  const { data } = await http.post(API_ENDPOINTS.academicSchedules.import, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '教务课表导入失败')
  }
  return normalizeListResponse(data)
}

export async function getAcademicCredential(credentialKey) {
  const { data } = await http.get(`${API_ENDPOINTS.academicSchedules.credentials}/${encodeURIComponent(credentialKey)}`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '获取教务账号配置失败')
  }
  return data?.data || data || {}
}

export async function saveAcademicCredential(payload) {
  const { data } = await http.post(API_ENDPOINTS.academicSchedules.credentials, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '保存教务账号配置失败')
  }
  return data?.data || data || {}
}

export async function deleteAcademicCredential(credentialKey) {
  const { data } = await http.delete(`${API_ENDPOINTS.academicSchedules.credentials}/${encodeURIComponent(credentialKey)}`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '删除教务账号配置失败')
  }
  return data?.data ?? true
}
