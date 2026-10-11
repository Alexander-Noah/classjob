import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'

import {
  getAllowedPaths,
  isPathAllowed,
  parseUserPermissions,
} from '../src/constants/permissions.js'

const readSource = (relativePath) =>
  readFileSync(new URL(relativePath, import.meta.url), 'utf8')

test('maintenance record entry and viewing are independent lab-admin route modules', () => {
  const routeModules = readSource('../src/constants/routeModules.js')
  const router = readSource('../src/router/index.js')

  const viewingIndex = routeModules.indexOf("path: '/lab-admin/maintenance-records'")
  const entryIndex = routeModules.indexOf("path: '/lab-admin/maintenance-records/new'")

  assert.notEqual(viewingIndex, -1)
  assert.notEqual(entryIndex, -1)
  assert.ok(viewingIndex < entryIndex, 'viewing route should be declared before the entry route')
  assert.match(routeModules, /name: 'LabAdminMaintenanceRecordList'[\s\S]*?menuTitle: '维护记录查看'[\s\S]*?roleScope: 'labAdmin'/)
  assert.match(routeModules, /name: 'LabAdminMaintenanceRecordEntry'[\s\S]*?menuTitle: '维护记录填写'[\s\S]*?roleScope: 'labAdmin'/)
  assert.match(router, /LabAdminMaintenanceRecordList:\s*\(\)\s*=>\s*import\('\.\.\/views\/maintenance-records\/MaintenanceRecordList\.vue'\)/)
  assert.match(router, /LabAdminMaintenanceRecordEntry:\s*\(\)\s*=>\s*import\('\.\.\/views\/maintenance-records\/MaintenanceRecordEntry\.vue'\)/)
})

test('entry and viewing permissions stay separate while legacy repair access expands to both', () => {
  assert.deepEqual(getAllowedPaths(['维护记录填写']), ['/lab-admin/maintenance-records/new'])
  assert.deepEqual(getAllowedPaths(['维护记录查看']), ['/lab-admin/maintenance-records'])
  assert.equal(isPathAllowed('/lab-admin/maintenance-records/new', ['维护记录查看']), false)
  assert.equal(isPathAllowed('/lab-admin/maintenance-records', ['维护记录填写']), false)

  const parsed = parseUserPermissions({
    permissions: JSON.stringify([{ group: '环境运维', items: ['故障报修'] }]),
  })

  assert.deepEqual(parsed, ['维护记录填写', '维护记录查看'])
  assert.equal(isPathAllowed('/lab-admin/maintenance-records/new', parsed), true)
  assert.equal(isPathAllowed('/lab-admin/maintenance-records', parsed), true)
})

test('lab-admin navigation and dashboards point to maintenance-record viewing', () => {
  const navigation = readSource('../src/constants/navigation.js')
  const frontendDashboard = readSource('../src/constants/dashboard.js')
  const backendDashboard = readSource('../../Backend/legacy-app/src/main/java/org/example/backend/service/impl/DashboardServiceImpl.java')

  assert.match(navigation, /title: '设备维护管理'[\s\S]*?children:\s*\[\s*'\/lab-admin\/maintenance-records',\s*'\/lab-admin\/maintenance-records\/new',\s*'\/lab-admin\/notices'\s*\]/)
  for (const source of [frontendDashboard, backendDashboard]) {
    assert.match(source, /维护记录查看/)
    assert.match(source, /\/lab-admin\/maintenance-records/)
    assert.doesNotMatch(source, /\/lab-admin\/repairs/)
  }
})

test('legacy repair URLs redirect to maintenance-record viewing', () => {
  const router = readSource('../src/router/index.js')

  assert.match(router, /path: 'lab-admin\/repairs',[\s\S]*?redirect: '\/lab-admin\/maintenance-records'/)
  assert.match(router, /path: '\/repairs',[\s\S]*?redirect: '\/lab-admin\/maintenance-records'/)
})

test('maintenance role routes, workbench, presets, and selectors are removed', () => {
  const authConstants = readSource('../src/constants/auth.js')
  const authService = readSource('../src/api/services/auth.js')
  const navigation = readSource('../src/constants/navigation.js')
  const routeModules = readSource('../src/constants/routeModules.js')
  const router = readSource('../src/router/index.js')
  const rolePortals = readSource('../src/constants/rolePortals.js')
  const roleModules = readSource('../src/constants/roleModules.js')
  const userPermissions = readSource('../src/views/users/UserPermissions.vue')
  const usersService = readSource('../src/api/services/users.js')
  const noticeCenter = readSource('../src/views/notices/NoticeCenter.vue')

  assert.doesNotMatch(authConstants, /maintenance:\s*'\/maintenance'/)
  assert.doesNotMatch(navigation, /\bmaintenance:\s*\{/)
  assert.doesNotMatch(routeModules, /roleScope:\s*'maintenance'/)
  assert.doesNotMatch(router, /MaintenancePortal|MaintenanceWorkOrders/)
  assert.doesNotMatch(rolePortals, /export const MAINTENANCE_PORTAL/)
  assert.doesNotMatch(roleModules, /export const MAINTENANCE_WORK_ORDER_MODULE/)
  assert.doesNotMatch(userPermissions.match(/const ROLE_OPTIONS = \[[^\]]*\]/)?.[0] || '', /维修人员/)
  assert.doesNotMatch(usersService.match(/const PERMISSIONS_BY_ROLE = \{[\s\S]*?\n\}/)?.[0] || '', /维修人员|维修工单/)
  assert.doesNotMatch(usersService, /roleCode:\s*'maintenance'|roleName:\s*'维修人员'/)
  assert.doesNotMatch(authService.match(/const DEFAULT_PERMISSION_GROUPS = \{[\s\S]*?\n\}/)?.[0] || '', /maintenance|维修工单/)
  assert.doesNotMatch(noticeCenter, /value:\s*'maintenance'/)

  assert.match(router, /path: '\/maintenance\/:pathMatch\(\.\*\)\*'[\s\S]*?redirect: '\/forbidden'/)
  assert.match(authService, /角色已停用，请联系系统管理员重新分配角色/)
  assert.equal(existsSync(new URL('../src/views/roles/MaintenancePortal.vue', import.meta.url)), false)
  assert.equal(existsSync(new URL('../src/views/maintenance/MaintenanceWorkOrders.vue', import.meta.url)), false)
  assert.equal(existsSync(new URL('../src/views/maintenance/components/MaintenanceWorkOrderDrawer.vue', import.meta.url)), false)
  assert.equal(existsSync(new URL('../src/views/maintenance/components/MaintenanceProcessDialog.vue', import.meta.url)), false)
})
