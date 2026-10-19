import { beforeEach, expect, it, vi } from 'vitest'
import http from '../../src/api/http'
import { ElMessage } from 'element-plus'
import * as keys from '../../src/constants/auth'

vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))
const request = http.interceptors.request.handlers[0].fulfilled
const success = http.interceptors.response.handlers[0].fulfilled
const failure = http.interceptors.response.handlers[0].rejected
const storage = () => {
  const values = new Map()
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) }
}
it('invalid form data never reaches the adapter and preserves its validation error', async () => {
  const adapter = vi.fn()
  await expect(http.post('/users', { username: 'x'.repeat(101) }, { adapter })).rejects.toMatchObject({ __inputValidation: true, __handled: true })
  expect(adapter).not.toHaveBeenCalled()
  expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('100'))
  ElMessage.error.mockClear()
  await expect(http.post('/users', { password: 'x'.repeat(129) }, { adapter, skipErrorMessage: true })).rejects.toMatchObject({ __inputValidation: true })
  expect(adapter).not.toHaveBeenCalled()
  expect(ElMessage.error).not.toHaveBeenCalled()
})
beforeEach(() => { vi.unstubAllGlobals(); vi.clearAllMocks() })

it('requests outside a browser preserve existing headers', () => {
  expect(request({ headers: { Accept: 'application/json' } })).toEqual({ headers: { Accept: 'application/json' } })
})

it('both persistent and temporary browser sessions supply authorization', () => {
  const browser = { localStorage: storage(), sessionStorage: storage() }
  vi.stubGlobal('window', browser)
  expect(request({ headers: {} })).toEqual({ headers: {} })
  browser.sessionStorage.setItem(keys.AUTH_TOKEN_STORAGE_KEY, 'temporary')
  expect(request({ headers: { Accept: 'application/json' } }).headers).toEqual({ Accept: 'application/json', Authorization: 'Bearer temporary' })
  browser.localStorage.setItem(keys.AUTH_TOKEN_STORAGE_KEY, 'persistent')
  expect(request({}).headers.Authorization).toBe('Bearer persistent')
})

it('successful responses pass through and explicit business failures reject', async () => {
  for (const data of [null, [], { value: 3 }, { code: 200 }, { code: '200' }]) {
    const response = { data }
    expect(success(response)).toBe(response)
  }
  await expect(success({ data: { code: 400, message: '参数错误' } })).rejects.toMatchObject({ message: '参数错误', __business: true })
  await expect(success({ data: { code: 500 } })).rejects.toThrow('接口请求失败')
})

it.each([
  [{ code: 'ECONNABORTED' }, '接口请求超时，请检查后端服务或网络连接'],
  [{}, '无法连接后端服务，请确认后端已启动且 VITE_API_PROXY_TARGET 配置正确'],
  [{ response: { status: 500, data: { message: '服务异常' } } }, '服务异常'],
  [{ response: { status: 500 }, message: 'server error' }, 'server error'],
  [{ response: { status: 500 } }, '接口请求失败'],
])('transport failure %j reports the expected message', async (error, message) => {
  await expect(failure(error)).rejects.toMatchObject({ message, userMessage: message, __handled: true })
  expect(ElMessage.error).toHaveBeenCalledWith(message)
})

it('callers can suppress the common error toast', async () => {
  await expect(failure({ config: { skipErrorMessage: true } })).rejects.toMatchObject({ __handled: true })
  expect(ElMessage.error).not.toHaveBeenCalled()
})

it.each(['/devices', '/login'])('401 clears both session stores and redirects from %s when needed', async path => {
  const browser = {
    localStorage: storage(), sessionStorage: storage(),
    location: { pathname: path, search: '?lab=7', hash: '#asset', replace: vi.fn() },
  }
  vi.stubGlobal('window', browser)
  for (const store of [browser.localStorage, browser.sessionStorage]) {
    for (const key of [keys.AUTH_STORAGE_KEY, keys.AUTH_ROLE_STORAGE_KEY, keys.AUTH_TOKEN_STORAGE_KEY, keys.AUTH_USER_STORAGE_KEY]) store.setItem(key, 'stale')
    store.setItem(keys.AUTH_REMEMBER_USERNAME_STORAGE_KEY, 'admin')
  }
  await expect(failure({ response: { status: 401 } })).rejects.toBeDefined()
  for (const store of [browser.localStorage, browser.sessionStorage]) {
    for (const key of [keys.AUTH_STORAGE_KEY, keys.AUTH_ROLE_STORAGE_KEY, keys.AUTH_TOKEN_STORAGE_KEY, keys.AUTH_USER_STORAGE_KEY]) expect(store.getItem(key)).toBeNull()
    expect(store.getItem(keys.AUTH_REMEMBER_USERNAME_STORAGE_KEY)).toBe('admin')
  }
  if (path === '/login') expect(browser.location.replace).not.toHaveBeenCalled()
  else expect(browser.location.replace).toHaveBeenCalledWith('/login?redirect=%2Fdevices%3Flab%3D7%23asset')
})
