/*
 * http公共逻辑模块，供相关视图或服务复用。
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL, API_TIMEOUT } from './config'
import {
  AUTH_ROLE_STORAGE_KEY,
  AUTH_STORAGE_KEY,
  AUTH_TOKEN_STORAGE_KEY,
  AUTH_USER_STORAGE_KEY,
} from '../constants/auth'

// 公共传输层：服务模块负责接口参数和数据适配，这里统一处理令牌与错误提示。
const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
})

http.interceptors.request.use((config) => {
  // “记住我”使用 localStorage，临时登录使用 sessionStorage，两种会话均可携带令牌。
  const token =
    typeof window !== 'undefined'
      ? window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY) || window.sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
      : ''

  if (token) {
    config.headers = {
      ...config.headers,
      Authorization: `Bearer ${token}`,
    }
  }

  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response?.data
    // HTTP 200 不等于业务成功；只有响应显式包含 code 时才按统一业务协议判定。
    const hasBusinessCode = body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')

    if (hasBusinessCode && Number(body.code) !== 200) {
      const error = new Error(body.message || '接口请求失败')
      error.response = response
      error.__business = true
      return Promise.reject(error)
    }

    return response
  },
  (error) => {
    if (error?.response?.status === 401 && typeof window !== 'undefined') {
      // 同时清空两种存储，防止旧令牌被另一种存储恢复；回跳地址保留当前查询参数。
      const clearSessionStorage = (storage) => {
        storage?.removeItem(AUTH_STORAGE_KEY)
        storage?.removeItem(AUTH_ROLE_STORAGE_KEY)
        storage?.removeItem(AUTH_TOKEN_STORAGE_KEY)
        storage?.removeItem(AUTH_USER_STORAGE_KEY)
      }
      clearSessionStorage(window.localStorage)
      clearSessionStorage(window.sessionStorage)

      if (window.location.pathname !== '/login') {
        const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
        window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`)
      }
    }

    const message =
      error?.response?.data?.message ||
      (error?.code === 'ECONNABORTED'
        ? '接口请求超时，请检查后端服务或网络连接'
        : !error?.response
          ? '无法连接后端服务，请确认后端已启动且 VITE_API_PROXY_TARGET 配置正确'
          : error?.message) ||
      '接口请求失败'

    // 页面自行处理错误时可关闭公共提示；__handled 用于避免调用方重复弹出消息。
    if (!error?.config?.skipErrorMessage) {
      ElMessage.error(message)
    }
    error.userMessage = message
    error.message = message
    error.__handled = true
    return Promise.reject(error)
  },
)

export default http
