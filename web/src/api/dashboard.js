/*
 * 工作台统计公共逻辑模块，供相关视图或服务复用。
 * 主要导出：fetchDashboardOverview。
 */
import http from './http'
import { API_ENDPOINTS } from './endpoints'
import { API_USE_MOCK } from './config'
import { DASHBOARD_OVERVIEW_MOCK } from '../constants/dashboard'
import { normalizeBusinessLoopOverview } from './businessLoop'

const arrayWithFallback = (value, fallback = []) => (Array.isArray(value) ? value : fallback)
const LAB_MANAGEMENT_SHORTCUT_TITLES = new Set(['实验室资源', '实验室管理'])

const normalizeShortcuts = (value) =>
  arrayWithFallback(value).filter(
    (shortcut) => !LAB_MANAGEMENT_SHORTCUT_TITLES.has(shortcut?.title),
  )

const normalizeDashboardOverview = (data = {}) => ({
  stats: arrayWithFallback(data?.stats),
  shortcuts: normalizeShortcuts(data?.shortcuts),
  labUsage: arrayWithFallback(data?.labUsage),
  deviceStatus: arrayWithFallback(data?.deviceStatus),
  notices: arrayWithFallback(data?.notices),
  doorAlerts: arrayWithFallback(data?.doorAlerts),
  todaySchedules: arrayWithFallback(data?.todaySchedules),
  todoItems: arrayWithFallback(data?.todoItems),
  recordReminders: arrayWithFallback(data?.recordReminders),
  recentActivities: arrayWithFallback(data?.recentActivities),
  businessLoop: normalizeBusinessLoopOverview(data?.businessLoop),
})

export async function fetchDashboardOverview() {
  if (API_USE_MOCK) {
    return normalizeDashboardOverview(DASHBOARD_OVERVIEW_MOCK)
  }

  const { data } = await http.get(API_ENDPOINTS.dashboard.overview)
  return normalizeDashboardOverview(data?.data || data)
}
