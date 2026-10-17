/*
 * request公共逻辑模块，供相关视图或服务复用。
 * 主要导出：getApiBaseUrl。
 */
import axios from 'axios'
import { createDiscreteApi } from 'naive-ui'

const { message } = createDiscreteApi(['message'])

// 桌面端直接访问后端，不走 Web 管理端的 Vite 代理；打包前配置实际服务地址。
const DEFAULT_API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * 归一化API基础URL。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function normalizeApiBaseUrl(value: string) {
  const trimmed = value.trim()
  if (!trimmed) return DEFAULT_API_BASE_URL
  return trimmed.replace(/\/+$/, '')
}

/**
 * 获取API基础URL。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function getApiBaseUrl() {
  return normalizeApiBaseUrl(DEFAULT_API_BASE_URL)
}

const service = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 8000
})

service.interceptors.request.use(
  /**
   * 处理 use 调用中的回调逻辑。
   * 入参：config。
   */
  config => {
    config.baseURL = getApiBaseUrl()
    try {
      // 登录会话与设备激活状态分开保存；激活成功不代表用户已获得业务接口权限。
      const rawSession = window.sessionStorage.getItem('tvta-ui-auth-session')
      const token = rawSession ? JSON.parse(rawSession)?.token : ''
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch {
      window.sessionStorage.removeItem('tvta-ui-auth-session')
    }
    return config
  },
  /**
   * 处理 use 调用中的回调逻辑。
   * 入参：error。
   */
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  /**
   * 处理 use 调用中的回调逻辑。
   * 入参：response。
   */
  response => {
    // 与 Web 管理端不同，此适配器直接返回响应体，调用方不再解包 AxiosResponse。
    return response.data
  },
  /**
   * 处理 use 调用中的回调逻辑。
   * 入参：error。
   */
  error => {
    if (!(error.config as any)?.silent) {
      const currentBaseUrl = getApiBaseUrl()
      const errorMessage = error.code === 'ERR_NETWORK'
        ? `无法连接后端服务：${currentBaseUrl}`
        : (error.response?.data?.message || error.message || 'Request Error')
      message.error(errorMessage)
    }
    return Promise.reject(error)
  }
)

export default service
