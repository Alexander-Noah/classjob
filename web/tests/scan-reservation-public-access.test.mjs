/*
 * 实验室预约的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const scanReservationUrl = new URL('../src/views/reservations/ScanReservation.vue', import.meta.url)
const routerUrl = new URL('../src/router/index.js', import.meta.url)

test('anonymous scan reservation visits skip the protected teaching calendar request', () => {
  const source = readFileSync(scanReservationUrl, 'utf8')

  assert.match(source, /import\s*\{[^}]*isAuthenticated[^}]*\}\s*from\s*['"]\.\.\/\.\.\/api\/services\/auth['"]/s)
  assert.match(source, /if\s*\(isAuthenticated\(\)\)\s*\{\s*try\s*\{\s*teachingCalendar\.value\s*=\s*await loadTeachingCalendar\(\)/s)
  assert.match(source, /await loadLabProfile\(\)/)
  assert.doesNotMatch(source, /catch\s*\([^)]*\)\s*\{[^}]*return[^}]*\}\s*await loadLabProfile\(\)/s)
})

test('scan reservation route remains public', () => {
  const source = readFileSync(routerUrl, 'utf8')
  const route = source.match(/\{\s*path:\s*['"]\/scan\/reserve['"][\s\S]*?\n\s*\},/)?.[0] || ''

  assert.match(route, /public:\s*true/)
})
