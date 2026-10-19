// @vitest-environment happy-dom
import { beforeEach, expect, it, vi } from 'vitest'
import { isRetiredRole } from '../../src/constants/auth'
import { ACTIVE_ROUTE_MODULES, ROUTE_MODULES, getRouteModulesByRole } from '../../src/constants/routeModules'
import { getRoleMenus } from '../../src/constants/navigation'
import { assertActiveSessionRole, persistAuthSession, isAuthenticated, loginByPassword } from '../../src/api/services/auth'
import http from '../../src/api/http'
import router from '../../src/router'

vi.mock('../../src/api/http', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
vi.mock('../../src/api/config', () => ({ API_USE_MOCK: false }))
// 此处验证导航守卫；登录页静态图片的加载交由生产构建验证。
vi.mock('../../src/views/login/Login.vue', () => ({ default: { render: () => null } }))

beforeEach(/** 每例清理浏览器会话，保证旧缓存与新登录相互独立。 */ () => {
  localStorage.clear()
  sessionStorage.clear()
  vi.clearAllMocks()
})

it('教师角色识别兼容编码、中文名称和旧缓存中的空格', /** 验证别名不能绕过限制。 */ () => {
  for (const role of ['teacher', '教师', ' Teacher ', 'maintenance', '维修人员']) expect(isRetiredRole(role)).toBe(true)
  for (const role of ['labAdmin', 'systemAdmin', 'academic', null]) expect(isRetiredRole(role)).toBe(false)
})

it('教师模块定义保留，但运行路由和菜单不再开放', /** 验证暂停而非删除实现。 */ () => {
  expect(ROUTE_MODULES.filter(module => module.roleScope === 'teacher')).toHaveLength(4)
  expect(ACTIVE_ROUTE_MODULES.some(module => module.roleScope === 'teacher')).toBe(false)
  expect(getRouteModulesByRole('teacher')).toEqual([])
  expect(getRoleMenus('teacher')).toEqual([])
  expect(getRouteModulesByRole('labAdmin').length).toBeGreaterThan(0)
  expect(getRouteModulesByRole('systemAdmin').length).toBeGreaterThan(0)
})

it('会话任一角色字段为教师即拒绝，不能伪装管理员回写缓存', /** 验证缓存边界。 */ () => {
  for (const user of [{ roleCode: 'teacher' }, { role: '教师' }, { roleName: '教师' }]) {
    expect(() => assertActiveSessionRole('labAdmin', user)).toThrow('角色已停用')
    expect(() => persistAuthSession({ role: 'labAdmin', user, token: 'token' })).toThrow('角色已停用')
  }
  expect(isAuthenticated()).toBe(false)
  expect(localStorage.length + sessionStorage.length).toBe(0)
})

it('服务端返回教师登录信息也不能建立新会话', /** 验证真实登录适配链路。 */ async () => {
  http.post.mockResolvedValue({ data: { code: 200, data: { token: 'old-token', user: { roleCode: 'teacher', roleName: '教师' } } } })
  await expect(loginByPassword({ username: 'teacher01', password: 'test-pass' })).rejects.toThrow('角色已停用')
  expect(isAuthenticated()).toBe(false)
})

it('管理员仍可创建浏览器会话', /** 验证正常角色无回归。 */ () => {
  persistAuthSession({ role: 'labAdmin', user: { roleCode: 'labAdmin' }, token: 'admin-token' })
  expect(isAuthenticated()).toBe(true)
})

it('旧教师链接统一进入无权访问页，教师页面组件不注册', /** 验证实际路由表。 */ () => {
  expect(router.getRoutes().some(route => route.meta?.roleScope === 'teacher')).toBe(false)
  for (const path of ['/teacher', '/teacher/schedule', '/teacher/course-environments', '/teacher/class-timetable-data']) {
    expect(router.resolve(path).matched.at(-1)?.redirect).toBe('/forbidden')
  }
})

it('已有教师缓存访问登录页时被清理', /** 验证真实路由守卫处理历史会话。 */ async () => {
  localStorage.setItem('smart-lab-authenticated', 'true')
  localStorage.setItem('smart-lab-role', 'teacher')
  localStorage.setItem('smart-lab-token', 'old-teacher-token')
  localStorage.setItem('smart-lab-user', JSON.stringify({ roleCode: 'teacher' }))
  await router.push('/login')
  expect(isAuthenticated()).toBe(false)
  expect(router.currentRoute.value.path).toBe('/login')
  expect(router.currentRoute.value.query.error).toContain('角色已停用')
  expect(http.get).not.toHaveBeenCalled()
})
