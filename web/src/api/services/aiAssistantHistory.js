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
    throw new Error(payload?.message || '聊天记录保存失败')
  }

  return payload?.data || payload
}

export const listAiConversationHistories = async () => {
  // 如果未登录，直接返回空数组，避免401错误
  if (!isAuthenticated()) {
    return []
  }
  
  const data = await requestJson(API_ENDPOINTS.aiAssistant.conversations)
  return Array.isArray(data) ? data : []
}

export const saveAiConversationHistories = async (conversations = []) => {
  // 如果未登录，直接返回空数组，避免401错误
  if (!isAuthenticated()) {
    return []
  }
  
  const data = await requestJson(API_ENDPOINTS.aiAssistant.conversations, {
    method: 'PUT',
    body: JSON.stringify({ conversations }),
  })
  return Array.isArray(data) ? data : []
}
