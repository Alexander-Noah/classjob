/*
 * 平台概览的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：fetchPlatformSnapshot。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

/**
 * 加载Collection。
 * 入参：endpoint（端点）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
async function loadCollection(endpoint) {
  const response = await http.get(endpoint)
  const payload = response.data
  if (payload?.code !== undefined && Number(payload.code) !== 200) {
    throw new Error('数据接口返回错误')
  }
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  const records = data?.records ?? data?.items
  if (!Array.isArray(records)) throw new Error('数据接口未返回记录列表')
  if (data.total !== undefined && Number(data.total) !== records.length) {
    throw new Error('接口未返回完整记录，暂不能展示汇总')
  }
  return records
}

/**
 * 获取Platform快照。
 * @returns 组装的键值对象。
 */
export async function fetchPlatformSnapshot() {
  const results = await Promise.allSettled([
    loadCollection(API_ENDPOINTS.users.list),
    loadCollection(API_ENDPOINTS.notices.list),
  ])
  return {
    users: results[0].status === 'fulfilled' ? results[0].value : null,
    notices: results[1].status === 'fulfilled' ? results[1].value : null,
    errors: results.flatMap(/** 转换并展开当前条目。 入参：result、index。 */ (result, index) => result.status === 'rejected' ? [`${index === 0 ? '账号' : '通知'}数据加载失败，请重试`] : []),
  }
}
