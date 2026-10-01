import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('profile dialog updates the current user through auth profile endpoint', () => {
  const layoutSource = readSource('../src/views/layout/AdminLayout.vue')
  const authServiceSource = readSource('../src/api/services/auth.js')

  assert.match(authServiceSource, /export\s+async\s+function\s+updateCurrentUserProfile/)
  assert.match(authServiceSource, /http\.put\(\s*API_ENDPOINTS\.auth\.profile/)
  assert.match(layoutSource, /updateCurrentUserProfile/)
  assert.doesNotMatch(layoutSource, /import\s+\{[^}]*updateUser[^}]*\}\s+from\s+['"]\.\.\/\.\.\/api\/services\/users['"]/)
  assert.doesNotMatch(layoutSource, /updateUser\(\s*currentUser\.value\.id/)
})
