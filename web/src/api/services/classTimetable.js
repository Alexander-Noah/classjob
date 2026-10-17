/*
 * 全校课表采集的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：listClassTimetables、getClassTimetableSummary、getClassTimetableSemesters、triggerClassTimetableCrawler、getClassTimetableCrawlerConfig 等。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

const unwrapData = (response) => {
  const payload = response?.data ?? response
  if (payload?.code && payload.code !== 200) {
    throw new Error(payload?.message || payload?.msg || '课表接口调用失败')
  }
  return payload?.data ?? payload
}

export async function listClassTimetables(params = {}) {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.list, { params }))
}

export async function getClassTimetableSummary() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.summary))
}

export async function getClassTimetableSemesters() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.semesters))
}

export async function triggerClassTimetableCrawler() {
  return unwrapData(await http.post(API_ENDPOINTS.classTimetables.crawl, undefined, { timeout: 10 * 60 * 1000 }))
}

export async function getClassTimetableCrawlerConfig() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.crawlerConfig))
}

export async function saveClassTimetableCrawlerConfig(payload) {
  return unwrapData(await http.post(API_ENDPOINTS.classTimetables.crawlerConfig, payload))
}

export async function listClassTimetableCrawlerTasks(params = {}) {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.crawlerTasks, { params }))
}

export async function clearClassTimetableCrawlerTasks() {
  return unwrapData(await http.delete(API_ENDPOINTS.classTimetables.crawlerTasks))
}

export async function getClassTimetableCrawlerCredential() {
  return unwrapData(await http.get(API_ENDPOINTS.classTimetables.credential, { skipErrorMessage: true }))
}

export async function saveClassTimetableCrawlerCredential(payload) {
  return unwrapData(await http.post(API_ENDPOINTS.classTimetables.credential, payload))
}

export async function deleteClassTimetableCrawlerCredential() {
  return unwrapData(await http.delete(API_ENDPOINTS.classTimetables.credential))
}
