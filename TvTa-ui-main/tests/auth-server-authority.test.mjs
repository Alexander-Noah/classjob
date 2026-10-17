/*
 * 身份认证的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'

const authStore = readFileSync(new URL('../src/stores/auth.ts', import.meta.url), 'utf8')
const loginDialog = readFileSync(new URL('../src/components/LoginDialog.vue', import.meta.url), 'utf8')
const requestClient = readFileSync(new URL('../src/utils/request.ts', import.meta.url), 'utf8')
const activationStore = readFileSync(new URL('../src/stores/activation.ts', import.meta.url), 'utf8')

test('teacher desktop delegates authentication to the backend', /** 验证当前测试用例的预期行为。 */ () => {
  assert.match(authStore, /request\.post/)
  assert.match(authStore, /\/auth\/login/)
  assert.match(requestClient, /Authorization/)
})

test('production desktop sources do not publish demo credentials', /** 验证当前测试用例的预期行为。 */ () => {
  for (const [name, source] of [
    ['auth store', authStore],
    ['login dialog', loginDialog],
  ]) {
    assert.doesNotMatch(source, /admin123|teacher\s*\/\s*123456/, name)
  }
})

test('local trial is isolated to test builds and obsolete local-account adapters are removed', /** 验证当前测试用例的预期行为。 */ () => {
  assert.match(activationStore, /import\.meta\.env\.MODE === 'test'/)
  assert.match(activationStore, /crypto\.randomUUID\(\)/)
  assert.doesNotMatch(activationStore, /TRIAL-LOCAL-30-DAYS|quota:\s*999/)

  for (const obsoleteSource of [
    new URL('../src/stores/auth_20260523_132417_456.ts', import.meta.url),
    new URL('../src/views/RegisterView.vue', import.meta.url),
    new URL('../src/views/BatchRegisterView.vue', import.meta.url),
  ]) {
    assert.equal(existsSync(obsoleteSource), false, obsoleteSource.pathname)
  }
})
