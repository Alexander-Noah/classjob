/*
 * AI 助手的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：getAiAssistantUserConfig、saveAiAssistantUserConfig、testAiAssistantUserConfig、requestAiAssistantAnswer。
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
    throw new Error(payload?.message || 'AI 接口请求失败')
  }

  return payload?.data || payload
}

/**
 * 获取AIAssistant用户配置。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getAiAssistantUserConfig = async () => {
  // 如果未登录，返回空配置
  if (!isAuthenticated()) {
    return {}
  }
  
  return requestJson(API_ENDPOINTS.aiAssistant.userConfig)
}

/**
 * 保存AIAssistant用户配置。
 * 入参：config（配置）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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

/**
 * 验证AIAssistant用户配置。
 * 入参：config（配置）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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

/**
 * 请求AIAssistantAnswer的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const requestAiAssistantAnswer = async ({ question, history = [], conversationId }) => {
  // 如果未登录，抛出友好错误
  if (!isAuthenticated()) {
    throw new Error('请先登录后再使用AI助手')
  }
  
  return requestJson(API_ENDPOINTS.aiAssistant.chat, {
    method: 'POST',
    body: JSON.stringify({ question, history, conversationId }),
  })
}

export const decideAiAssistantAction = (id, conversationId, confirmed) =>
  requestJson(`/ai-assistant/actions/${encodeURIComponent(id)}/decision`, {
    method: 'POST', body: JSON.stringify({ conversationId, confirmed }),
  })
