// 前端请求后端的基础路径。
// 开发环境通常是 /api，由 Vite 代理到后端；生产环境可改成真实 API 域名。
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// API 请求超时时间，单位毫秒。
export const API_TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 10000)

// API 运行模式：
// - server：请求真实后端。
// - mock：使用前端模拟数据。
export const API_MODE = import.meta.env.VITE_API_MODE || 'server'

// 是否启用 mock 数据。只要不是 server，都视为 mock 模式。
export const API_USE_MOCK = API_MODE !== 'server'
