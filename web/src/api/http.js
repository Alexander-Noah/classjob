import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL, API_TIMEOUT } from './config'
import {
  AUTH_ROLE_STORAGE_KEY,
  AUTH_STORAGE_KEY,
  AUTH_TOKEN_STORAGE_KEY,
  AUTH_USER_STORAGE_KEY,
} from '../constants/auth'

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
})

http.interceptors.request.use((config) => {
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
