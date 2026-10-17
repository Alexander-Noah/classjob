/*
 * 工作台统计的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const source = readFileSync(new URL('../src/views/dashboard/DashboardHome.vue', import.meta.url), 'utf8')

const cssBlock = (selector) => {
  const match = source.match(new RegExp(`${selector.replace('.', '\\.')}\\s*\\{([\\s\\S]*?)\\n\\}`))
  return match?.[1] || ''
}

test('door reminder list keeps reminder rows pinned to the top of the card', () => {
  const alertListStyles = cssBlock('.alert-list')
  const alertItemStyles = cssBlock('.alert-item')
  const alertEmptyStyles = cssBlock('.alert-empty')

  assert.match(alertListStyles, /display:\s*flex;/)
  assert.match(alertListStyles, /flex-direction:\s*column;/)
  assert.match(alertListStyles, /justify-content:\s*flex-start;/)
  assert.doesNotMatch(alertListStyles, /display:\s*grid;/)
  assert.match(alertItemStyles, /flex:\s*0\s+0\s+auto;/)
  assert.match(alertEmptyStyles, /flex:\s*1\s+1\s+auto;/)
})
