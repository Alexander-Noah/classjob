// @vitest-environment happy-dom
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import Page from '../../src/views/users/RolePageAccess.vue'
import * as api from '../../src/api/services/rolePageAccess'

vi.mock('../../src/api/services/auth', () => ({ getStoredRole: () => 'systemAdmin' }))
vi.mock('../../src/api/services/rolePageAccess', () => ({
  listAccessRoles: vi.fn(), getRolePageAccess: vi.fn(), saveRolePageAccess: vi.fn(), refreshPageAccess: vi.fn(),
}))
const pages = [
  { path: '/lab-admin', title: '工作台', menu: true, locked: true },
  { path: '/lab-admin/devices', title: '设备资产', menu: true, locked: false },
]
let wrapper
beforeEach(() => {
  vi.clearAllMocks()
  api.listAccessRoles.mockResolvedValue([{ code: 'labAdmin', name: '实验室管理员' }])
  api.getRolePageAccess.mockResolvedValue({ pages, policy: { accessiblePaths: pages.map(p => p.path), visiblePaths: pages.map(p => p.path) } })
  api.saveRolePageAccess.mockResolvedValue({})
})
afterEach(() => wrapper?.unmount())
async function setup() {
  wrapper = mount(Page, { global: { plugins: [ElementPlus] } })
  await flushPromises()
}
it('visible controls separate menu hiding from page authorization and save the exact role', async () => {
  await setup()
  const visible = wrapper.get('[aria-label="设备资产显示菜单"] input')
  await visible.setValue(false)
  expect(wrapper.get('[aria-label="设备资产允许访问"] input').element.checked).toBe(true)
  await wrapper.findAll('button').find(b=>b.text()==='保存角色权限').trigger('click')
  await flushPromises()
  expect(api.saveRolePageAccess).toHaveBeenCalledWith('labAdmin', {
    accessiblePaths: ['/lab-admin','/lab-admin/devices'], visiblePaths: ['/lab-admin'],
  })
})
it('revoking page access removes its menu and locked home cannot be disabled', async () => {
  await setup()
  await wrapper.get('[aria-label="设备资产允许访问"] input').setValue(false)
  expect(wrapper.get('[aria-label="设备资产显示菜单"] input').element.checked).toBe(false)
  expect(wrapper.get('[aria-label="设备资产显示菜单"] input').element.disabled).toBe(true)
  expect(wrapper.get('[aria-label="工作台允许访问"] input').element.disabled).toBe(true)
})
it('failed loading never enables saving an accidental empty policy', async () => {
  api.getRolePageAccess.mockRejectedValue(new Error('offline'))
  await setup()
  expect(wrapper.findAll('button').find(b=>b.text()==='保存角色权限').element.disabled).toBe(true)
  expect(api.saveRolePageAccess).not.toHaveBeenCalled()
})
