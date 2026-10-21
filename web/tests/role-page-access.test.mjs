import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import { canAccessRolePage, isRoleMenuVisible, filterRoleMenus, createRecoveryPolicy, isRecoveryPage } from '../src/constants/rolePageAccess.js'
import { API_ENDPOINTS } from '../src/api/endpoints.js'

test('hidden menu keeps direct page access and empty grants deny every optional page', () => {
  const policy = { accessiblePaths: ['/lab-admin', '/lab-admin/devices'], visiblePaths: ['/lab-admin'] }
  assert.equal(canAccessRolePage(policy, '/lab-admin/devices'), true)
  assert.equal(isRoleMenuVisible(policy, '/lab-admin/devices'), false)
  assert.equal(canAccessRolePage(policy, '/lab-admin/devices/edit'), false)
  assert.equal(canAccessRolePage({ accessiblePaths: [] }, '/lab-admin/devices'), false)
  assert.equal(canAccessRolePage(null, '/lab-admin'), false)
})
test('empty groups disappear while original menu input remains intact', () => {
  const menus = [{ index: '/lab-admin' }, { index: 'assets', children: [{ index: '/lab-admin/devices' }] }]
  const policy = { accessiblePaths: ['/lab-admin'], visiblePaths: ['/lab-admin'] }
  assert.deepEqual(filterRoleMenus(menus, policy), [{ index: '/lab-admin' }])
  assert.equal(menus[1].children.length, 1)
})
test('service outage preserves only necessary recovery entries without granting business pages', () => {
  const admin = createRecoveryPolicy('systemAdmin')
  assert.deepEqual(admin.visiblePaths, ['/system-admin','/system-admin/role-access'])
  assert.equal(canAccessRolePage(admin, '/system-admin/users'), false)
  assert.equal(isRecoveryPage('labAdmin','/system-admin/role-access'), false)
  assert.equal(isRecoveryPage('labAdmin','/lab-admin'), true)
})
test('backend catalog covers every frontend page and respects role boundaries', () => {
  const catalog = JSON.parse(readFileSync(new URL('../../Backend/src/main/resources/security/role-pages.json', import.meta.url)))
  const source = readFileSync(new URL('../src/constants/routeModules.js', import.meta.url), 'utf8')
  const paths = [...source.matchAll(/^    path: '([^']+)'/gm)].map(match => match[1])
  assert.deepEqual(new Set(catalog.map(page => page.path)), new Set(paths))
  assert.ok(catalog.filter(page => page.locked).some(page => page.path === '/system-admin/role-access'))
  assert.ok(catalog.find(page=>page.path==='/system-admin/users').api.includes(`* ${API_ENDPOINTS.users.list}/**`))
  assert.ok(catalog.find(page=>page.path==='/system-admin/activation-codes').api.includes(`* ${API_ENDPOINTS.activationCodes.list}/**`))
})
