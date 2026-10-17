/*
 * 身份认证的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('server dashboard normalization never fills missing sections with mock rows', /** 验证当前测试用例的预期行为。 */ () => {
  const source = readSource('../src/api/dashboard.js')

  assert.match(source, /const arrayWithFallback = \(value, fallback = \[\]\)/)
  assert.doesNotMatch(source, /data\?\.stats, DASHBOARD_OVERVIEW_MOCK\.stats/)
  assert.doesNotMatch(source, /data\?\.todaySchedules, DASHBOARD_OVERVIEW_MOCK\.todaySchedules/)
  assert.doesNotMatch(source, /data\?\.recentActivities, DASHBOARD_OVERVIEW_MOCK\.recentActivities/)
})

test('dashboard cards preserve authoritative statistic values returned by the backend', /** 验证当前测试用例的预期行为。 */ () => {
  const source = readSource('../src/views/dashboard/DashboardHome.vue')
  const statsBlock = source.slice(source.indexOf('const stats = computed'), source.indexOf('const loadDashboardOverview'))

  assert.doesNotMatch(statsBlock, /nextStat\.value\s*=/)
  assert.doesNotMatch(statsBlock, /nextStat\.title\s*=/)
})
