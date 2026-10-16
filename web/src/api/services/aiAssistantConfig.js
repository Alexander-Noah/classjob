import { API_BASE_URL } from '../config'
import { API_ENDPOINTS } from '../endpoints'
import { getAuthToken, isAuthenticated } from './auth'

const buildApiUrl = (path) => {
  const baseUrl = String(API_BASE_URL || '').replace(/\/$/, '')
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${baseUrl}${normalizedPath}`
}

const requestJson = async (path, options = {}) => {
  const token = getAuthToken()
  const response = await fetch(buildApiUrl(path), {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  })

  const payload = await response.json().catch(() => ({}))
  if (!response.ok || (payload?.code && Number(payload.code) !== 200)) {
    throw new Error(payload?.message || 'AI 接口请求失败')
  }

  return payload?.data || payload
}

export const getAiAssistantUserConfig = async () => {
  // 如果未登录，返回空配置
  if (!isAuthenticated()) {
    return {}
  }
  
  return requestJson(API_ENDPOINTS.aiAssistant.userConfig)
}

export const saveAiAssistantUserConfig = async (config) => {
  // 如果未登录，不执行保存操作
  if (!isAuthenticated()) {
    return {}
  }
  
  return requestJson(API_ENDPOINTS.aiAssistant.userConfig, {
    method: 'PUT',
    body: JSON.stringify(config || {}),
  })
}

export const testAiAssistantUserConfig = async (config) => {
  // 如果未登录，抛出友好错误
  if (!isAuthenticated()) {
    throw new Error('请先登录后再测试AI接口')
  }

  return requestJson(API_ENDPOINTS.aiAssistant.testUserConfig, {
    method: 'POST',
    body: config ? JSON.stringify(config) : undefined,
  })
}

export const requestAiAssistantAnswer = async ({ question, history = [] }) => {
  // 如果未登录，抛出友好错误
  if (!isAuthenticated()) {
    throw new Error('请先登录后再使用AI助手')
  }
  
  return requestJson(API_ENDPOINTS.aiAssistant.chat, {
    method: 'POST',
    body: JSON.stringify({ question, history }),
  })
}
