/*
 * 通知公告的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：noticeService。
 */
import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

/**
 * 归一化载荷。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizePayload = (payload) => payload?.data ?? payload ?? null

/**
 * 归一化列表。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeList = (payload) => {
  const data = normalizePayload(payload)
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data)) return data
  return []
}

/**
 * unwrap响应的处理逻辑。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const unwrapResponse = (response) => normalizePayload(response?.data)

export const noticeService = {
  /**
   * 列出当前操作。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async list() {
    const response = await http.get(API_ENDPOINTS.notices.list)
    return normalizeList(response.data)
  },

  /**
   * 创建当前操作。
   * 入参：payload（载荷）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async create(payload) {
    const response = await http.post(API_ENDPOINTS.notices.list, payload)
    return unwrapResponse(response)
  },

  /**
   * 更新当前操作。
   * 入参：id（标识）；payload（载荷）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async update(id, payload) {
    const response = await http.put(`${API_ENDPOINTS.notices.list}/${id}`, payload)
    return unwrapResponse(response)
  },

  /**
   * 移除当前操作。
   * 入参：id（标识）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async remove(id) {
    const response = await http.delete(`${API_ENDPOINTS.notices.list}/${id}`)
    return normalizeList(response.data)
  },

  /**
   * publish的处理逻辑。
   * 入参：id（标识）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async publish(id) {
    const response = await http.post(API_ENDPOINTS.notices.publish(id))
    return unwrapResponse(response)
  },

  /**
   * withdraw的处理逻辑。
   * 入参：id（标识）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async withdraw(id) {
    const response = await http.post(API_ENDPOINTS.notices.withdraw(id))
    return unwrapResponse(response)
  },

  /**
   * 归档的处理逻辑。
   * 入参：id（标识）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async archive(id) {
    const response = await http.post(API_ENDPOINTS.notices.archive(id))
    return unwrapResponse(response)
  },

  /**
   * recipients的处理逻辑。
   * 入参：id（标识）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async recipients(id) {
    const response = await http.get(API_ENDPOINTS.notices.recipients(id))
    return normalizeList(response.data)
  },

  /**
   * 用户通知的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async userNotices() {
    const response = await http.get(API_ENDPOINTS.notices.user)
    return normalizeList(response.data)
  },

  /**
   * unread通知的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async unreadNotices() {
    const response = await http.get(API_ENDPOINTS.notices.unread)
    return normalizeList(response.data)
  },

  /**
   * 标记Read。
   * 入参：id（标识）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async markRead(id) {
    const response = await http.post(API_ENDPOINTS.notices.read(id))
    return unwrapResponse(response)
  },

  /**
   * stats的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async stats() {
    const response = await http.get(API_ENDPOINTS.notices.stats)
    return unwrapResponse(response) || {}
  },

  /**
   * 同步BusinessReminders。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async syncBusinessReminders() {
    const response = await http.post(API_ENDPOINTS.notices.syncBusinessReminders)
    return unwrapResponse(response) || {}
  },
}
