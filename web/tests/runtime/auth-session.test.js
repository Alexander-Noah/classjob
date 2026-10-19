// @vitest-environment happy-dom
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import * as auth from '../../src/api/services/auth'
import * as keys from '../../src/constants/auth'
import http from '../../src/api/http'

vi.mock('../../src/api/http', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }))
vi.mock('../../src/api/config', () => ({ API_USE_MOCK: false }))

const user = { id: 7, username: 'admin', roleCode: 'labAdmin', realName: '管理员' }
beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  vi.resetAllMocks()
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-10-18T00:00:00Z'))
})
afterEach(() => vi.useRealTimers())

it('empty and malformed cached identities have safe defaults', () => {
  expect(auth.getStoredUser()).toBeNull()
  expect(auth.getStoredRole()).toBe('labAdmin')
  expect(auth.getAuthToken()).toBe('')
  expect(auth.isAuthenticated()).toBe(false)
  localStorage.setItem(keys.AUTH_USER_STORAGE_KEY, '{broken')
  expect(auth.getStoredUser()).toBeNull()
})

it('switching remember mode removes the old session from the other storage', () => {
  auth.persistAuthSession({ role: 'labAdmin', token: 'first', user })
  auth.persistAuthSession({ role: 'systemAdmin', token: 'second', user, remember: false })
  expect(localStorage.length).toBe(0)
  expect(auth.getStoredRole()).toBe('systemAdmin')
  expect(auth.getAuthToken()).toBe('second')
  expect(auth.getStoredUser()).toEqual(user)
  auth.persistAuthSession({ role: 'labAdmin', token: 'third', user })
  expect(sessionStorage.length).toBe(0)
  expect(auth.getAuthToken()).toBe('third')
  auth.clearAuthSession()
  expect(localStorage.length + sessionStorage.length).toBe(0)
})

it('remembered username is trimmed and cleared when disabled', () => {
  expect(auth.getRememberedLogin()).toEqual({ remember: true, username: '' })
  auth.persistRememberedLogin({ username: '  admin  ', remember: true })
  expect(auth.getRememberedLogin()).toEqual({ remember: true, username: 'admin' })
  auth.persistRememberedLogin({ username: 'ignored', remember: false })
  expect(auth.getRememberedLogin()).toEqual({ remember: false, username: '' })
  expect(localStorage.getItem(keys.AUTH_REMEMBER_USERNAME_STORAGE_KEY)).toBeNull()
})

it('profile edits merge cached fields without moving a temporary session to local storage', () => {
  auth.persistAuthSession({ role: 'labAdmin', token: 'token', user, remember: false })
  const updated = auth.updateStoredUser({ realName: '新名称', phone: '123' })
  expect(updated).toMatchObject({ id: 7, username: 'admin', name: '新名称', phone: '123', roleCode: 'labAdmin' })
  expect(auth.getStoredUser()).toEqual(updated)
  expect(localStorage.length).toBe(0)
  expect(auth.getAuthToken()).toBe('token')
  expect(() => auth.updateStoredUser({ roleCode: 'teacher' })).toThrow('角色已停用')
  expect(auth.getStoredUser()).toEqual(updated)
})

it('profile fetch and update unwrap responses and surface business failures', async () => {
  http.get.mockResolvedValueOnce({ data: { code: 200, data: user } })
  expect(await auth.getCurrentUser('admin')).toEqual(user)
  expect(http.get).toHaveBeenCalledWith('/auth/profile', { params: { username: 'admin' } })
  http.get.mockResolvedValueOnce({ data: { code: 403, message: '无权访问' } })
  await expect(auth.getCurrentUser()).rejects.toThrow('无权访问')
  http.put.mockResolvedValueOnce({ data: { data: user } })
  expect(await auth.updateCurrentUserProfile({ realName: '名称' })).toEqual(user)
  expect(http.put).toHaveBeenCalledWith('/auth/profile', { realName: '名称' })
  http.put.mockResolvedValueOnce({ data: { code: 400 } })
  await expect(auth.updateCurrentUserProfile()).rejects.toThrow('更新个人资料失败')
})

it('refresh requires authentication, throttles normal calls and supports forced refresh', async () => {
  expect(await auth.refreshCurrentUserSession()).toBeNull()
  expect(http.get).not.toHaveBeenCalled()
  auth.persistAuthSession({ role: 'labAdmin', token: 'token', user })
  http.get.mockResolvedValue({ data: { data: { ...user, realName: '服务端名称' } } })
  expect(await auth.refreshCurrentUserSession({ force: true })).toMatchObject({ name: '服务端名称' })
  await auth.refreshCurrentUserSession()
  expect(http.get).toHaveBeenCalledTimes(1)
  vi.advanceTimersByTime(8001)
  await auth.refreshCurrentUserSession()
  expect(http.get).toHaveBeenCalledTimes(2)
  http.get.mockRejectedValueOnce(new Error('offline'))
  expect(await auth.refreshCurrentUserSession({ force: true })).toEqual(auth.getStoredUser())
  http.get.mockResolvedValueOnce({ data: null })
  expect(await auth.refreshCurrentUserSession({ force: true })).toEqual(auth.getStoredUser())
})

it('refresh propagates retired-role failures instead of silently retaining access', async () => {
  auth.persistAuthSession({ role: 'labAdmin', token: 'token', user })
  http.get.mockResolvedValueOnce({ data: { data: { ...user, roleCode: 'teacher' } } })
  await expect(auth.refreshCurrentUserSession({ force: true })).rejects.toThrow('角色已停用')
})

it('login strips the remember flag and stores the server profile in session storage', async () => {
  http.post.mockResolvedValue({ data: { code: 200, data: { token: 'server-token', user } } })
  http.get.mockResolvedValue({ data: { data: { ...user, realName: '服务端名称' } } })
  const result = await auth.loginByPassword({ username: 'admin', password: 'secret', remember: false })
  expect(http.post).toHaveBeenCalledWith('/auth/login', { username: 'admin', password: 'secret' })
  expect(result).toMatchObject({ token: 'server-token', role: 'labAdmin', user: { name: '服务端名称' } })
  expect(auth.getStoredUser()).toEqual(result.user)
  expect(localStorage.length).toBe(0)
  expect(sessionStorage.getItem(keys.AUTH_TOKEN_STORAGE_KEY)).toBe('server-token')
})

it('failed login leaves no session, and profile outages preserve a valid login response', async () => {
  http.post.mockResolvedValueOnce({ data: { code: 401, message: '账号或密码错误' } })
  await expect(auth.loginByPassword({ username: 'admin' })).rejects.toThrow('账号或密码错误')
  expect(auth.isAuthenticated()).toBe(false)
  http.post.mockResolvedValueOnce({ data: { token: 'token', user } })
  http.get.mockRejectedValueOnce(new Error('offline'))
  expect(await auth.loginByPassword({ username: 'admin' })).toMatchObject({ token: 'token', user: { id: 7 } })
})

it('logout clears both storage modes even if the server is unavailable', async () => {
  auth.persistAuthSession({ role: 'labAdmin', token: 'token', user })
  sessionStorage.setItem(keys.AUTH_TOKEN_STORAGE_KEY, 'stale')
  auth.persistRememberedLogin({ username: 'admin', remember: true })
  http.post.mockRejectedValueOnce(new Error('offline'))
  await auth.logout()
  expect(auth.isAuthenticated()).toBe(false)
  expect(auth.getAuthToken()).toBe('')
  expect(auth.getStoredUser()).toBeNull()
  expect(auth.getRememberedLogin().username).toBe('admin')
})
