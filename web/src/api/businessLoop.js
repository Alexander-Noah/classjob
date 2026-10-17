/*
 * 业务闭环公共逻辑模块，供相关视图或服务复用。
 * 主要导出：normalizeBusinessLoopOverview、fetchBusinessLoopOverview。
 */
import http from './http'
import { API_USE_MOCK } from './config'

const BUSINESS_LOOP_OVERVIEW_URL = '/business-loop/overview'

const EMPTY_BUSINESS_LOOP = {
  cards: [],
  chains: [],
  actions: [],
  latestEvents: [],
  eventCategories: {},
  usageTrend: [],
  labUsageRanking: [],
  repairFaultRanking: [],
  totals: {},
}

/**
 * 归一化BusinessLoop概况。
 * 入参：data（数据）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const normalizeBusinessLoopOverview = (data) => ({
  ...EMPTY_BUSINESS_LOOP,
  ...(data && typeof data === 'object' ? data : {}),
  cards: Array.isArray(data?.cards) ? data.cards : [],
  chains: Array.isArray(data?.chains) ? data.chains : [],
  actions: Array.isArray(data?.actions) ? data.actions : [],
  latestEvents: Array.isArray(data?.latestEvents) ? data.latestEvents : [],
  eventCategories: data?.eventCategories && typeof data.eventCategories === 'object' ? data.eventCategories : {},
  usageTrend: Array.isArray(data?.usageTrend) ? data.usageTrend : [],
  labUsageRanking: Array.isArray(data?.labUsageRanking) ? data.labUsageRanking : [],
  repairFaultRanking: Array.isArray(data?.repairFaultRanking) ? data.repairFaultRanking : [],
  totals: data?.totals && typeof data.totals === 'object' ? data.totals : {},
})

/**
 * 获取BusinessLoop概况。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function fetchBusinessLoopOverview() {
  if (API_USE_MOCK) {
    return normalizeBusinessLoopOverview(EMPTY_BUSINESS_LOOP)
  }

  const { data } = await http.get(BUSINESS_LOOP_OVERVIEW_URL)
  return normalizeBusinessLoopOverview(data?.data || data)
}
