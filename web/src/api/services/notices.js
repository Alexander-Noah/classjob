import { API_ENDPOINTS } from '../endpoints'
import http from '../http'

const normalizePayload = (payload) => payload?.data ?? payload ?? null

const normalizeList = (payload) => {
  const data = normalizePayload(payload)
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data)) return data
  return []
}

const unwrapResponse = (response) => normalizePayload(response?.data)

export const noticeService = {
  async list() {
    const response = await http.get(API_ENDPOINTS.notices.list)
    return normalizeList(response.data)
  },

  async create(payload) {
    const response = await http.post(API_ENDPOINTS.notices.list, payload)
    return unwrapResponse(response)
  },

  async update(id, payload) {
    const response = await http.put(`${API_ENDPOINTS.notices.list}/${id}`, payload)
    return unwrapResponse(response)
  },

  async remove(id) {
    const response = await http.delete(`${API_ENDPOINTS.notices.list}/${id}`)
    return normalizeList(response.data)
  },

  async publish(id) {
    const response = await http.post(API_ENDPOINTS.notices.publish(id))
    return unwrapResponse(response)
  },

  async withdraw(id) {
    const response = await http.post(API_ENDPOINTS.notices.withdraw(id))
    return unwrapResponse(response)
  },

  async archive(id) {
    const response = await http.post(API_ENDPOINTS.notices.archive(id))
    return unwrapResponse(response)
  },

  async recipients(id) {
    const response = await http.get(API_ENDPOINTS.notices.recipients(id))
    return normalizeList(response.data)
  },

  async userNotices() {
    const response = await http.get(API_ENDPOINTS.notices.user)
    return normalizeList(response.data)
  },

  async unreadNotices() {
    const response = await http.get(API_ENDPOINTS.notices.unread)
    return normalizeList(response.data)
  },

  async markRead(id) {
    const response = await http.post(API_ENDPOINTS.notices.read(id))
    return unwrapResponse(response)
  },

  async stats() {
    const response = await http.get(API_ENDPOINTS.notices.stats)
    return unwrapResponse(response) || {}
  },

  async syncBusinessReminders() {
    const response = await http.post(API_ENDPOINTS.notices.syncBusinessReminders)
    return unwrapResponse(response) || {}
  },
}
