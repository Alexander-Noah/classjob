/*
 * 全校课表采集的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listClassTimetables、getClassTimetableSummary、getClassTimetableSemesters、triggerClassTimetableCrawler、getClassTimetableCrawlerConfig 等。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

/**
 * unwrap数据的处理逻辑。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const unwrapData = (response) => {
  const payload = response?.data ?? response
  if (payload?.code && payload.code !== 200) {
    throw new Error(payload?.message || payload?.msg || '课表接口调用失败')
  }
  return payload?.data ?? payload
}

/**
 * 列出Class课表。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listClassTimetables(params = {}) {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.list, { params }))
}

/**
 * 获取Class课表摘要。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getClassTimetableSummary() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.summary))
}

/**
 * 获取Class课表学期。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getClassTimetableSemesters() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.semesters))
}

/**
 * triggerClass课表Crawler的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function triggerClassTimetableCrawler() {
  return unwrapData(await http.post(API_ENDPOINTS.classTimetables.crawl, undefined, { timeout: 10 * 60 * 1000 }))
}

/**
 * 获取Class课表Crawler配置。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getClassTimetableCrawlerConfig() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.crawlerConfig))
}

/**
 * 保存Class课表Crawler配置。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function saveClassTimetableCrawlerConfig(payload) {
  return unwrapData(await http.post(API_ENDPOINTS.classTimetables.crawlerConfig, payload))
}

/**
 * 列出Class课表CrawlerTasks。
 * 入参：params（参数）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listClassTimetableCrawlerTasks(params = {}) {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.crawlerTasks, { params }))
}

/**
 * 清理Class课表CrawlerTasks。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function clearClassTimetableCrawlerTasks() {
  return unwrapData(await http.delete(API_ENDPOINTS.classTimetables.crawlerTasks))
}

/**
 * 获取Class课表Crawler凭据。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getClassTimetableCrawlerCredential() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.credential, { skipErrorMessage: true }))
}

/**
 * 保存Class课表Crawler凭据。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function saveClassTimetableCrawlerCredential(payload) {
  return unwrapData(await http.post(API_ENDPOINTS.classTimetables.credential, payload))
}

/**
 * 删除Class课表Crawler凭据。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function deleteClassTimetableCrawlerCredential() {
  return unwrapData(await http.delete(API_ENDPOINTS.classTimetables.credential))
}
