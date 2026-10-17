/*
 * 工作台统计公共逻辑模块，供相关视图或服务复用。
 * 主要导出：fetchDashboardOverview。
 */
import http from './http'
import { API_ENDPOINTS } from './endpoints'
import { API_USE_MOCK } from './config'
import { DASHBOARD_OVERVIEW_MOCK } from '../constants/dashboard'
import { normalizeBusinessLoopOverview } from './businessLoop'

/**
 * arrayWithFallback的处理逻辑。
 * 入参：value（值）；fallback（fallback）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const arrayWithFallback = (value, fallback = []) => (Array.isArray(value) ? value : fallback)
const LAB_MANAGEMENT_SHORTCUT_TITLES = new Set(['实验室资源', '实验室管理'])

/**
 * 归一化Shortcuts。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeShortcuts = (value) =>
  arrayWithFallback(value).filter(
    /**
     * 判断当前条目是否符合筛选条件。
     * 入参：shortcut。
     */
    (shortcut) => !LAB_MANAGEMENT_SHORTCUT_TITLES.has(shortcut?.title),
  )

/**
 * 归一化Dashboard概况。
 * 入参：data（数据）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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

/**
 * 获取Dashboard概况。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function fetchDashboardOverview() {
  if (API_USE_MOCK) {
    return normalizeDashboardOverview(DASHBOARD_OVERVIEW_MOCK)
  }

  const { data } = await http.get(API_ENDPOINTS.dashboard.overview)
  return normalizeDashboardOverview(data?.data || data)
}
