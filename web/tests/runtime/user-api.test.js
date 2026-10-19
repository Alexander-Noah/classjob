// @vitest-environment happy-dom
import { beforeEach, expect, it, vi } from 'vitest'
import * as api from '../../src/api/services/users'
import { noticeService } from '../../src/api/services/notices'
import http from '../../src/api/http'
import { ADMIN_USERS_STORAGE_KEY } from '../../src/constants/auth'
const mode = vi.hoisted(() => ({ mock: false }))
vi.mock('../../src/api/config', () => ({ get API_USE_MOCK() { return mode.mock } }))
vi.mock('../../src/api/http', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
beforeEach(() => { mode.mock = false; localStorage.clear(); vi.resetAllMocks() })
const user = () => ({ id: 7, username: 'labadmin', realName: '管理员', roleCode: 'labAdmin', department: '实验中心', status: 1 })
it.each([[[]],[{ records: [user()] }],[{ data: [user()] }],[{ data: { records: [user()], total: 10 } }]])('user pages unwrap response %j and apply local filters', async data => {
  http.get.mockResolvedValue({ data })
  const result = await api.getUserPage({ keyword: 'admin', role: '实验室管理员', department: '实验中心', status: '启用' })
  expect(Array.isArray(result.records)).toBe(true)
  if (result.records.length) expect(result.records[0]).toMatchObject({ roleName: '实验室管理员', employeeNo: 'labadmin' })
  await api.listUsers(); expect(http.get).toHaveBeenLastCalledWith('/admin/users', { params: { page: 1, pageSize: 1000 } })
  await api.listUserOptions(); expect(http.get).toHaveBeenLastCalledWith('/users/options')
})
it.each([
  ['createUser','post','/admin/users', [user()], user()],
  ['updateUser','put','/admin/users/7',[7,user()],user()],
  ['deleteUser','delete','/admin/users/7',[7],{id:7}],
  ['updateUserStatus','post','/admin/users/7/status',[7,'停用'],{id:7,status:0}],
  ['resetUserPassword','post','/admin/users/7/reset-password',[7],{id:7}],
])('%s uses its endpoint, supports absent record fallback and rejects business errors', async (name, verb, url, args, fallback) => {
  http[verb].mockResolvedValueOnce({ data: { data: { id: 7 } } }).mockResolvedValueOnce({ data: { code: 200 } }).mockResolvedValueOnce({ data: { code: 400, message: '无权操作' } })
  expect(await api[name](...args)).toEqual({ id: 7 }); expect(http[verb].mock.calls[0][0]).toBe(url)
  expect(await api[name](...args)).toEqual(fallback)
  await expect(api[name](...args)).rejects.toThrow('无权操作')
})
it.each([
  ['batchEnableUsers','enable',[[7]]],['batchDisableUsers','disable',[[7]]],['batchDeleteUsers','delete',[[7]]],
  ['batchUpdateRole','role',[[7],'实验室管理员']],['batchUpdateDepartment','department',[[7],'信息中心']],
])('%s only sends selected IDs and surfaces failed batches', async (name, path, args) => {
  http.post.mockResolvedValueOnce({ data: { data: [user()] } }).mockResolvedValueOnce({ data: { code: 500 } })
  expect(await api[name](...args)).toEqual([user()]); expect(http.post).toHaveBeenCalledWith(`/admin/users/batch/${path}`, expect.objectContaining({ ids: [7] }))
  await expect(api[name](...args)).rejects.toThrow()
})
it('list and option endpoints reject business failures', async () => {
  http.get.mockResolvedValue({ data: { code: 500, message: '查询失败' } })
  await expect(api.getUserPage()).rejects.toThrow('查询失败'); await expect(api.listUserOptions()).rejects.toThrow('查询失败')
})
it('import, avatar and export send files and return server payloads', async () => {
  const file = new File(['test'], 'users.xlsx'); http.post.mockResolvedValue({ data: { data: { count: 1, url: '/avatar' } } })
  expect(await api.importUsers(file)).toMatchObject({ count: 1 }); expect(http.post.mock.calls[0][1].get('file').name).toBe('users.xlsx')
  expect(await api.uploadUserAvatar(file)).toMatchObject({ url: '/avatar' }); expect(http.post.mock.calls[1][0]).toBe('/files/avatars')
  http.get.mockResolvedValue({ data: 'xlsx' }); expect(await api.exportUsers({ status: '启用' })).toBe('xlsx')
  http.post.mockResolvedValue({ data: { code: 400, message: '文件无效' } })
  await expect(api.importUsers(file)).rejects.toThrow('文件无效'); await expect(api.uploadUserAvatar(file)).rejects.toThrow('文件无效')
})
it('mock user CRUD persists normalized copies, status, role and department changes', async () => {
  mode.mock = true
  expect((await api.listUsers()).length).toBeGreaterThan(0)
  const saved = await api.createUser({ username: 'new', roleName: '实验室管理员', name: '新增账号' })
  expect(saved.realName).toBe('新增账号'); expect(saved.roleCode).toBe('labAdmin')
  expect(await api.updateUser(saved.id, { realName: '更新账号' })).toMatchObject({ realName: '更新账号' })
  expect(await api.updateUserStatus(saved.id, false)).toMatchObject({ status: 0 })
  expect((await api.getUserPage({ keyword: 'new', status: '停用' })).records).toHaveLength(1)
  expect((await api.batchEnableUsers([saved.id]))[0].status).toBe(1)
  expect((await api.batchDisableUsers([saved.id]))[0].status).toBe(0)
  expect((await api.batchUpdateRole([saved.id], '系统管理员'))[0].roleCode).toBe('systemAdmin')
  expect((await api.batchUpdateDepartment([saved.id], '信息中心'))[0].department).toBe('信息中心')
  expect((await api.resetUserPassword(saved.id)).logs[0].content).toContain('重置登录密码')
  expect((await api.listUserOptions()).some(item => item.id === saved.id)).toBe(true)
  expect((await api.batchDeleteUsers([saved.id]))[0].id).toBe(saved.id)
  expect(await api.deleteUser(999)).toEqual({ id: 999 })
  expect(await api.updateUser(999, { roleName: '维修人员' })).toMatchObject({ id: 999, permissions: '[]' })
  expect((await api.resetUserPassword(998)).id).toBe(998)
  expect(await api.updateUserStatus(998, true)).toEqual({ id: 998, status: 1 })
  expect(http.post).not.toHaveBeenCalled()
})
it('mock corrupt storage falls back and mock file operations return declared pending results', async () => {
  mode.mock = true; localStorage.setItem(ADMIN_USERS_STORAGE_KEY, 'broken')
  expect((await api.listUsers()).length).toBeGreaterThan(0)
  expect(await api.importUsers({ name: 'test.xlsx' })).toMatchObject({ pending: true, fileName: 'test.xlsx' })
  expect(await api.exportUsers({ role: '教师' })).toMatchObject({ pending: true, params: { role: '教师' } })
  expect(await api.uploadUserAvatar(new File(['image'], 'avatar.png', { type: 'image/png' }))).toMatchObject({ url: expect.stringMatching(/^data:image\/png;base64,/) })
})
it('notice read and write endpoints normalize list envelopes and record responses', async () => {
  const list = [{ id: 7 }]; http.get.mockResolvedValue({ data: { data: { items: list } } }); http.post.mockResolvedValue({ data: { data: { id: 7 } } }); http.put.mockResolvedValue({ data: { id: 7 } }); http.delete.mockResolvedValue({ data: { records: list } })
  for (const [method,args] of [['list',[]],['recipients',[7]],['userNotices',[]],['unreadNotices',[]]]) expect(await noticeService[method](...args)).toEqual(list)
  http.get.mockResolvedValue({ data: { data: { id: 7 } } })
  for (const [method,args] of [['create',[{}]],['update',[7,{}]],['publish',[7]],['withdraw',[7]],['archive',[7]],['markRead',[7]],['stats',[]],['syncBusinessReminders',[]]]) expect(await noticeService[method](...args)).toEqual({id:7})
  expect(await noticeService.remove(7)).toEqual(list)
  http.get.mockResolvedValue({data:null}); http.post.mockResolvedValue({data:null})
  expect(await noticeService.list()).toEqual([]); expect(await noticeService.stats()).toEqual({}); expect(await noticeService.syncBusinessReminders()).toEqual({})
})
