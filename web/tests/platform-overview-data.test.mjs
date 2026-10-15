import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import test from 'node:test'
import { buildPlatformSummary, buildPlatformAccountRows, buildPlatformStatistics } from '../src/modules/platform/platformOverview.js'

const accounts = [
  { id: 1, username: 'sysadmin', roleCode: 'systemAdmin', status: 1, createdAt: '2026-10-15 10:00:00' },
  { id: 2, username: 'labadmin', roleCode: 'labAdmin', status: 1, createdAt: '2026-10-15 11:00:00' },
]

test('only the two actual accounts appear in platform totals, roles and account rows', () => {
  const stats = buildPlatformStatistics(accounts, [])
  assert.deepEqual(stats.summary.map(item => item.value), ['2', '2', '2', '0'])
  assert.equal(stats.distribution.data.reduce((sum, item) => sum + item.value, 0), 2)
  assert.deepEqual(stats.trend.values, [2])
  assert.deepEqual(buildPlatformAccountRows(accounts).map(row => row[0]), ['sysadmin', 'labadmin'])
  assert.deepEqual(stats.ranking.categories, [])
})

test('empty data stays empty and missing responses remain unknown rather than zero', () => {
  assert.deepEqual(buildPlatformSummary([], []).map(item => item.value), ['0', '0', '0', '0'])
  assert.deepEqual(buildPlatformSummary(null, null).map(item => item.value), ['—', '—', '—', '—'])
  assert.deepEqual(buildPlatformStatistics([], []).distribution.data, [])
  assert.deepEqual(buildPlatformAccountRows([]), [])
})

test('disabled users and additional roles are counted from records rather than presets', () => {
  const users = [...accounts, { username: 'teacher', roleCode: 'teacher', status: 0 }]
  assert.deepEqual(buildPlatformSummary(users, [{publishStatus:'草稿'}]).map(item => item.value), ['3', '2', '2', '1'])
  assert.deepEqual(buildPlatformStatistics(users, []).compare.values, [2, 1])
})

function service(http) {
  const source = readFileSync(new URL('../src/api/services/platformOverview.js', import.meta.url), 'utf8')
    .replace(/^import .*$/gm, '').replace('export async function', 'async function')
  return vm.runInNewContext(`${source}\nfetchPlatformSnapshot`, {
    http, API_ENDPOINTS: {users:{list:'/users'},notices:{list:'/notices'}},
  })
}

test('API empty arrays are authoritative and a failed request is not disguised as an empty database', async () => {
  const load = service({get: async endpoint => {
    if (endpoint === '/notices') throw new Error('Unavailable')
    return {data:{code:200,data:[]}}
  }})
  const result = await load()
  assert.equal(result.users.length, 0)
  assert.equal(result.notices, null)
  assert.equal(result.errors.length, 1)
})

test('partial pages and business errors cannot produce misleading totals', async () => {
  const load = service({get: async endpoint => ({data: endpoint === '/users'
    ? {code:200,data:{records:accounts,total:127}}
    : {code:500,data:[]}})})
  const result = await load()
  assert.equal(result.users, null)
  assert.equal(result.notices, null)
  assert.equal(result.errors.length, 2)
})
