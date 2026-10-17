/*
 * 平台概览的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import test from 'node:test'
import { buildPlatformSummary, buildPlatformAccountRows, buildPlatformStatistics } from '../src/modules/platform/platformOverview.js'

const accounts = [
  { id: 1, username: 'sysadmin', roleCode: 'systemAdmin', status: 1, createdAt: '2026-10-15 10:00:00' },
  { id: 2, username: 'labadmin', roleCode: 'labAdmin', status: 1, createdAt: '2026-10-15 11:00:00' },
]

test('only the two actual accounts appear in platform totals, roles and account rows', /** 验证当前测试用例的预期行为。 */ () => {
  const stats = buildPlatformStatistics(accounts, [])
  assert.deepEqual(stats.summary.map(/** 将当前条目转换为目标结果。 入参：item。 */ item => item.value), ['2', '2', '2', '0'])
  assert.equal(stats.distribution.data.reduce(/** 合并当前条目与累计结果。 入参：sum、item。 */ (sum, item) => sum + item.value, 0), 2)
  assert.deepEqual(stats.trend.values, [2])
  assert.deepEqual(buildPlatformAccountRows(accounts).map(/** 将当前条目转换为目标结果。 入参：row。 */ row => row[0]), ['sysadmin', 'labadmin'])
  assert.deepEqual(stats.ranking.categories, [])
})

test('empty data stays empty and missing responses remain unknown rather than zero', /** 验证当前测试用例的预期行为。 */ () => {
  assert.deepEqual(buildPlatformSummary([], []).map(/** 将当前条目转换为目标结果。 入参：item。 */ item => item.value), ['0', '0', '0', '0'])
  assert.deepEqual(buildPlatformSummary(null, null).map(/** 将当前条目转换为目标结果。 入参：item。 */ item => item.value), ['—', '—', '—', '—'])
  assert.deepEqual(buildPlatformStatistics([], []).distribution.data, [])
  assert.deepEqual(buildPlatformAccountRows([]), [])
})

test('disabled users and additional roles are counted from records rather than presets', /** 验证当前测试用例的预期行为。 */ () => {
  const users = [...accounts, { username: 'teacher', roleCode: 'teacher', status: 0 }]
  assert.deepEqual(buildPlatformSummary(users, [{publishStatus:'草稿'}]).map(/** 将当前条目转换为目标结果。 入参：item。 */ item => item.value), ['3', '2', '2', '1'])
  assert.deepEqual(buildPlatformStatistics(users, []).compare.values, [2, 1])
})

/**
 * 服务的处理逻辑。
 * 入参：http（HTTP）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function service(http) {
  const source = readFileSync(new URL('../src/api/services/platformOverview.js', import.meta.url), 'utf8')
    .replace(/^import .*$/gm, '').replace('export async function', 'async function')
  return vm.runInNewContext(`${source}\nfetchPlatformSnapshot`, {
    http, API_ENDPOINTS: {users:{list:'/users'},notices:{list:'/notices'}},
  })
}

test('API empty arrays are authoritative and a failed request is not disguised as an empty database', /** 验证当前测试用例的预期行为。 */ async () => {
  const load = service({/** 获取当前操作。 入参：endpoint（端点）。 @returns 组装的键值对象。 */ get: async endpoint => {
    if (endpoint === '/notices') throw new Error('Unavailable')
    return {data:{code:200,data:[]}}
  }})
  const result = await load()
  assert.equal(result.users.length, 0)
  assert.equal(result.notices, null)
  assert.equal(result.errors.length, 1)
})

test('partial pages and business errors cannot produce misleading totals', /** 验证当前测试用例的预期行为。 */ async () => {
  const load = service({/** 获取当前操作。 入参：endpoint（端点）。 @returns 由当前表达式或分支生成的处理结果。 */ get: async endpoint => ({data: endpoint === '/users'
    ? {code:200,data:{records:accounts,total:127}}
    : {code:500,data:[]}})})
  const result = await load()
  assert.equal(result.users, null)
  assert.equal(result.notices, null)
  assert.equal(result.errors.length, 2)
})
