/*
 * AI 助手的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：deleteAiConversationHistory、listAiConversationHistories、saveAiConversationHistories。
 */
import { API_BASE_URL } from '../config'
import { API_ENDPOINTS } from '../endpoints'
import { getAuthToken, isAuthenticated } from './auth'

/**
 * 构建APIURL。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const buildApiUrl = (path) => {
  const baseUrl = String(API_BASE_URL || '').replace(/\/$/, '')
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${baseUrl}${normalizedPath}`
}

/**
 * 请求JSON的处理逻辑。
 * 涉及：发起 HTTP 请求。
 * 入参：path（路径）；options（选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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

  const payload = await response.json().catch(/** 处理异步失败结果。 */ () => ({}))
  if (!response.ok || (payload?.code && Number(payload.code) !== 200)) {
    throw new Error(payload?.message || '聊天记录保存失败')
  }

  return payload?.data || payload
}

/**
 * 删除AI会话历史。
 * 入参：conversationId（会话标识）。
 */
export const deleteAiConversationHistory = async (conversationId) => {
  if (!isAuthenticated()) return
  await requestJson(`${API_ENDPOINTS.aiAssistant.conversations}/${encodeURIComponent(conversationId)}`, {
    method: 'DELETE',
  })
}

/**
 * 列出AI会话历史。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const listAiConversationHistories = async () => {
  // 如果未登录，直接返回空数组，避免401错误
  if (!isAuthenticated()) {
    return []
  }
  
  const data = await requestJson(API_ENDPOINTS.aiAssistant.conversations)
  return Array.isArray(data) ? data : []
}

/**
 * 保存AI会话历史。
 * 入参：conversations（会话）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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
