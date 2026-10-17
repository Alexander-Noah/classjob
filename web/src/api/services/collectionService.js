/*
 * collectionService的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：createCollectionService。
 */
import http from '../http'
import { API_USE_MOCK } from '../config'
import { requireAuthoritativeRecord } from './authoritativeRecord'

// 仅面向可 JSON 序列化的台账数据；Date 会转成字符串，不能传入循环对象或函数。
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

// 兼容列表、records/items 和统一 data 包装；未识别的响应保持空列表，不填造业务记录。
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

/**
 * 创建台账资源的请求适配器；真实模式走 HTTP，mock 模式在 localStorage 保存副本。
 * @param {object} options 资源配置，endpoints.list 用于列表与单条增删改
 * @param {boolean} options.serverAuthoritativeRecords 正式创建/更新是否必须返回有效服务端记录
 * @returns {object} list/replace/create/update/reset/remove 等异步操作
 * @description 严格记录校验只作用于 create/update，不等于所有批量接口都提供同等保证。
 */
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

    // 只有响应明确不含列表时才兼容回显提交值；显式空列表必须作为服务端结果保留。
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

    // 正式模式不生成本地 ID；严格资源必须接受服务端已保存对象，否则整次操作报错。
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
