/*
 * profile-editing-boundary.test的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('profile dialog updates the current user through auth profile endpoint', /** 验证当前测试用例的预期行为。 */ () => {
  const layoutSource = readSource('../src/views/layout/AdminLayout.vue')
  const authServiceSource = readSource('../src/api/services/auth.js')

  assert.match(authServiceSource, /export\s+async\s+function\s+updateCurrentUserProfile/)
  assert.match(authServiceSource, /http\.put\(\s*API_ENDPOINTS\.auth\.profile/)
  assert.match(layoutSource, /updateCurrentUserProfile/)
  assert.doesNotMatch(layoutSource, /import\s+\{[^}]*updateUser[^}]*\}\s+from\s+['"]\.\.\/\.\.\/api\/services\/users['"]/)
  assert.doesNotMatch(layoutSource, /updateUser\(\s*currentUser\.value\.id/)
})
