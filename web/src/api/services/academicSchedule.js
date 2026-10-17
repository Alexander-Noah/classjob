/*
 * 教务课表集成的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：importAcademicSchedule、getAcademicCredential、saveAcademicCredential、deleteAcademicCredential。
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
 * 导入教务课表。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function importAcademicSchedule(payload) {
  const { data } = await http.post(API_ENDPOINTS.academicSchedules.import, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '教务课表导入失败')
  }
  return normalizeListResponse(data)
}

/**
 * 获取教务凭据。
 * 入参：credentialKey（凭据键）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getAcademicCredential(credentialKey) {
  const { data } = await http.get(`${API_ENDPOINTS.academicSchedules.credentials}/${encodeURIComponent(credentialKey)}`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '获取教务账号配置失败')
  }
  return data?.data || data || {}
}

/**
 * 保存教务凭据。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function saveAcademicCredential(payload) {
  const { data } = await http.post(API_ENDPOINTS.academicSchedules.credentials, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '保存教务账号配置失败')
  }
  return data?.data || data || {}
}

/**
 * 删除教务凭据。
 * 入参：credentialKey（凭据键）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function deleteAcademicCredential(credentialKey) {
  const { data } = await http.delete(`${API_ENDPOINTS.academicSchedules.credentials}/${encodeURIComponent(credentialKey)}`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '删除教务账号配置失败')
  }
  return data?.data ?? true
}
