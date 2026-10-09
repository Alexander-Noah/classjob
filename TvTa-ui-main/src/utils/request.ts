import axios from 'axios'
import { createDiscreteApi } from 'naive-ui'

const { message } = createDiscreteApi(['message'])

const DEFAULT_API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function normalizeApiBaseUrl(value: string) {
  const trimmed = value.trim()
  if (!trimmed) return DEFAULT_API_BASE_URL
  return trimmed.replace(/\/+$/, '')
}

export function getApiBaseUrl() {
  return normalizeApiBaseUrl(DEFAULT_API_BASE_URL)
}

const service = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 8000
})

service.interceptors.request.use(
  config => {
    config.baseURL = getApiBaseUrl()
    try {
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
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    return response.data
  },
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
