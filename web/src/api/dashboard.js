import http from './http'
import { API_ENDPOINTS } from './endpoints'
import { API_USE_MOCK } from './config'
import { DASHBOARD_OVERVIEW_MOCK } from '../constants/dashboard'
import { normalizeBusinessLoopOverview } from './businessLoop'

const arrayWithFallback = (value, fallback = []) => (Array.isArray(value) ? value : fallback)
const LAB_MANAGEMENT_SHORTCUT_TITLES = new Set(['实验室资源', '实验室管理'])

const normalizeShortcuts = (value) =>
  arrayWithFallback(value, DASHBOARD_OVERVIEW_MOCK.shortcuts).filter(
    (shortcut) => !LAB_MANAGEMENT_SHORTCUT_TITLES.has(shortcut?.title),
  )

const normalizeDashboardOverview = (data = {}) => ({
  stats: arrayWithFallback(data?.stats, DASHBOARD_OVERVIEW_MOCK.stats),
  shortcuts: normalizeShortcuts(data?.shortcuts),
  labUsage: arrayWithFallback(data?.labUsage, DASHBOARD_OVERVIEW_MOCK.labUsage),
  deviceStatus: arrayWithFallback(data?.deviceStatus, DASHBOARD_OVERVIEW_MOCK.deviceStatus),
  notices: arrayWithFallback(data?.notices, DASHBOARD_OVERVIEW_MOCK.notices),
  doorAlerts: arrayWithFallback(data?.doorAlerts, DASHBOARD_OVERVIEW_MOCK.doorAlerts),
  todaySchedules: arrayWithFallback(data?.todaySchedules, DASHBOARD_OVERVIEW_MOCK.todaySchedules),
  todoItems: arrayWithFallback(data?.todoItems, DASHBOARD_OVERVIEW_MOCK.todoItems),
  recordReminders: arrayWithFallback(data?.recordReminders, DASHBOARD_OVERVIEW_MOCK.recordReminders),
  recentActivities: arrayWithFallback(data?.recentActivities, DASHBOARD_OVERVIEW_MOCK.recentActivities),
  businessLoop: normalizeBusinessLoopOverview(data?.businessLoop),
})

export async function fetchDashboardOverview() {
  if (API_USE_MOCK) {
    return normalizeDashboardOverview(DASHBOARD_OVERVIEW_MOCK)
  }

  const { data } = await http.get(API_ENDPOINTS.dashboard.overview)
  return normalizeDashboardOverview(data?.data || data)
}
