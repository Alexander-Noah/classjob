/*
 * systemSettings的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：getSystemSettings、saveSystemSettings、resetSystemSettings。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

const normalizeSettingsResponse = (data, fallbackSettings = {}) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackSettings
}

export async function getSystemSettings(fallbackSettings = {}) {
  const { data } = await http.get(API_ENDPOINTS.systemSettings.detail)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '获取系统配置失败')
  }
  return normalizeSettingsResponse(data, fallbackSettings)
}

export async function saveSystemSettings(settings) {
  const { data } = await http.put(API_ENDPOINTS.systemSettings.detail, settings)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '保存系统配置失败')
  }
  return normalizeSettingsResponse(data, settings)
}

export async function resetSystemSettings() {
  const { data } = await http.post(API_ENDPOINTS.systemSettings.reset)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '恢复系统配置失败')
  }
  return normalizeSettingsResponse(data, {})
}
