import http from '../http'
import { API_USE_MOCK } from '../config'
import { requireAuthoritativeRecord } from './authoritativeRecord'

const cloneRecords = (records) => JSON.parse(JSON.stringify(records || []))

const readLocalRecords = (storageKey) => {
  if (typeof window === 'undefined') return null

  try {
    const rawValue = window.localStorage.getItem(storageKey)
    const parsedValue = rawValue ? JSON.parse(rawValue) : null
    return Array.isArray(parsedValue) ? parsedValue : null
  } catch {
    return null
  }
}

const writeLocalRecords = (storageKey, records) => {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(storageKey, JSON.stringify(records))
}

const removeLocalRecords = (storageKey) => {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(storageKey)
}

const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data?.records)) return data.data.records
  if (Array.isArray(data?.data?.items)) return data.data.items
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.items)) return data.items
  return []
}

const normalizeRecordResponse = (data, fallbackRecord) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackRecord
}

export function createCollectionService({ endpoints, resourceName, storageKey, serverAuthoritativeRecords = false }) {
  const list = async ({ fallbackRecords = [] } = {}) => {
    if (API_USE_MOCK) {
      return cloneRecords(readLocalRecords(storageKey) || fallbackRecords)
    }

    const { data } = await http.get(endpoints.list)
    return normalizeListResponse(data)
  }

  const replace = async (records) => {
    const nextRecords = cloneRecords(records)

    if (API_USE_MOCK) {
      writeLocalRecords(storageKey, nextRecords)
      return nextRecords
    }

    const { data } = await http.put(endpoints.batch, {
      resource: resourceName,
      records: nextRecords,
    })

    const normalizedRecords = normalizeListResponse(data)
    const responseContainsList =
      Array.isArray(data?.data?.records) ||
      Array.isArray(data?.data?.items) ||
      Array.isArray(data?.data) ||
      Array.isArray(data) ||
      Array.isArray(data?.records) ||
      Array.isArray(data?.items)

    return responseContainsList ? normalizedRecords : nextRecords
  }

  const create = async (record) => {
    const nextRecord = cloneRecords([record])[0]

    if (API_USE_MOCK) {
      const records = readLocalRecords(storageKey) || []
      const savedRecord = {
        ...nextRecord,
        id: nextRecord.id || `${resourceName}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      }
      const nextRecords = [savedRecord, ...records]
      writeLocalRecords(storageKey, nextRecords)
      return savedRecord
    }

    const { data } = await http.post(endpoints.list, nextRecord)
    return serverAuthoritativeRecords ? requireAuthoritativeRecord(data, resourceName, 'create') : normalizeRecordResponse(data, nextRecord)
  }

  const update = async (id, record) => {
    const nextRecord = cloneRecords([record])[0]

    if (API_USE_MOCK) {
      const records = readLocalRecords(storageKey) || []
      const nextRecords = records.map((item) => (String(item.id) === String(id) ? { ...item, ...nextRecord, id } : item))
      writeLocalRecords(storageKey, nextRecords)
      return nextRecords.find((item) => String(item.id) === String(id)) || nextRecord
    }

    const { data } = await http.put(`${endpoints.list}/${id}`, nextRecord)
    return serverAuthoritativeRecords ? requireAuthoritativeRecord(data, resourceName, 'update') : normalizeRecordResponse(data, nextRecord)
  }

  const reset = async ({ fallbackRecords = [] } = {}) => {
    if (API_USE_MOCK) {
      removeLocalRecords(storageKey)
      return cloneRecords(fallbackRecords)
    }

    if (endpoints.reset) {
      await http.post(endpoints.reset)
    }

    return list()
  }

  const remove = async (id) => {
    if (API_USE_MOCK) {
      const records = readLocalRecords(storageKey) || []
      const nextRecords = records.filter((record) => String(record.id) !== String(id))
      writeLocalRecords(storageKey, nextRecords)
      return nextRecords
    }

    await http.delete(`${endpoints.list}/${id}`)
    return list()
  }

  const approve = async (id, payload = {}) => {
    if (API_USE_MOCK) {
      const records = readLocalRecords(storageKey) || []
      const nextRecords = records.map((item) =>
        String(item.id) === String(id)
          ? {
              ...item,
              ...payload,
              status: '已通过',
              tagType: 'success',
              conflict: false,
              reviewer: payload.reviewer || payload.reviewerName || item.reviewer,
              note: payload.note || payload.reviewRemark || item.note,
            }
          : item,
      )
      writeLocalRecords(storageKey, nextRecords)
      return nextRecords.find((item) => String(item.id) === String(id)) || payload
    }

    const { data } = await http.post(`${endpoints.list}/${id}/approve`, payload)
    return serverAuthoritativeRecords ? requireAuthoritativeRecord(data, resourceName, 'approve') : normalizeRecordResponse(data, payload)
  }

  const reject = async (id, payload = {}) => {
    if (API_USE_MOCK) {
      const records = readLocalRecords(storageKey) || []
      const nextRecords = records.map((item) =>
        String(item.id) === String(id)
          ? {
              ...item,
              ...payload,
              status: '已驳回',
              tagType: 'info',
              conflict: false,
              reviewer: payload.reviewer || payload.reviewerName || item.reviewer,
              note: payload.note || payload.reviewRemark || item.note,
            }
          : item,
      )
      writeLocalRecords(storageKey, nextRecords)
      return nextRecords.find((item) => String(item.id) === String(id)) || payload
    }

    const { data } = await http.post(`${endpoints.list}/${id}/reject`, payload)
    return serverAuthoritativeRecords ? requireAuthoritativeRecord(data, resourceName, 'reject') : normalizeRecordResponse(data, payload)
  }

  const action = async (id, actionName, payload = {}) => {
    if (API_USE_MOCK) {
      const records = readLocalRecords(storageKey) || []
      const nextRecords = records.map((item) =>
        String(item.id) === String(id)
          ? {
              ...item,
              ...payload,
            }
          : item,
      )
      writeLocalRecords(storageKey, nextRecords)
      return nextRecords.find((item) => String(item.id) === String(id)) || payload
    }

    const { data } = await http.post(`${endpoints.list}/${id}/${actionName}`, payload)
    return serverAuthoritativeRecords ? requireAuthoritativeRecord(data, resourceName, 'action') : normalizeRecordResponse(data, payload)
  }

  return {
    action,
    approve,
    create,
    remove,
    list,
    replace,
    reject,
    reset,
    update,
  }
}
