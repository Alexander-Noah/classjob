// @vitest-environment happy-dom
import { beforeEach,it,expect,vi } from 'vitest'
import * as auth from '../../src/api/services/auth'
import * as keys from '../../src/constants/auth'
vi.mock('../../src/api/config',()=>({API_USE_MOCK:true}))
vi.mock('../../src/api/http',()=>({default:{}}))
beforeEach(()=>{localStorage.clear();sessionStorage.clear()})
it('mock profiles keep cached optional fields and can locate identities solely by ID', async () => {
  localStorage.setItem(keys.ADMIN_USERS_STORAGE_KEY, JSON.stringify([{ id: 71, role: '系统管理员', name: '缓存姓名' }, { username: 'other', roleCode: 'labAdmin' }]))
  auth.persistAuthSession({ role: 'systemAdmin', user: { id: 71 } })
  expect(await auth.getCurrentUser()).toMatchObject({ id: 71, roleCode: 'systemAdmin', name: '缓存姓名' })
  auth.persistAuthSession({ role: 'labAdmin', user: { username: 'entry', name: '姓名', realName: '真名', department: '部门', phone: '号码', email: '邮箱', avatarUrl: '/avatar' } })
  expect(await auth.updateCurrentUserProfile()).toMatchObject({ username: 'entry', name: '真名', department: '部门', phone: '号码', email: '邮箱', avatarUrl: '/avatar' })
  auth.clearAuthSession()
  expect((await auth.loginByPassword({ username: '', role: 'unknown' })).user.roleCode).toBe('labAdmin')
})
it('mock login and profile resolve defaults in non-browser rendering', async () => {
  vi.stubGlobal('window', undefined)
  try {
    expect((await auth.loginByPassword({})).user.roleCode).toBe('labAdmin')
    expect((await auth.getCurrentUser()).roleCode).toBe('labAdmin')
    expect((await auth.updateCurrentUserProfile()).roleCode).toBe('labAdmin')
  } finally { vi.unstubAllGlobals() }
})
it('mock login resolves explicit accounts from cached records and keeps temporary sessions isolated',async()=>{localStorage.setItem(keys.ADMIN_USERS_STORAGE_KEY,JSON.stringify([{id:9,username:'custom',realName:'管理员甲',role:'实验室管理员',department:'实验中心'}]));const session=await auth.loginByPassword({username:' CUSTOM ',remember:false});expect(session.user).toMatchObject({id:9,username:'custom',name:'管理员甲',roleCode:'labAdmin'});expect(sessionStorage.getItem(keys.AUTH_STORAGE_KEY)).toBe('true');expect(localStorage.getItem(keys.AUTH_TOKEN_STORAGE_KEY)).toBeNull();expect((await auth.getCurrentUser()).id).toBe(9);expect(await auth.updateCurrentUserProfile({name:'新姓名',phone:'123',email:'a@b.test',avatarUrl:'/avatar'})).toMatchObject({name:'新姓名',phone:'123',email:'a@b.test',department:'实验中心',avatarUrl:'/avatar'});await auth.logout();expect(auth.isAuthenticated()).toBe(false)})
it('mock login handles missing or malformed cache, default aliases and retired users',async()=>{for(const cache of ['bad','{}','[]']){localStorage.setItem(keys.ADMIN_USERS_STORAGE_KEY,cache);const session=await auth.loginByPassword({username:'labadmin'});expect(session.role).toBe('labAdmin');expect(session.user.username).toBe('labadmin');auth.clearAuthSession()}expect((await auth.loginByPassword({username:'sysadmin'})).role).toBe('systemAdmin');for(const username of ['repair','maintenance','teacher','academic01'])await expect(auth.loginByPassword({username})).rejects.toThrow('角色已停用')})
it('mock profile resolves cached account by role when no identity is stored',async()=>{localStorage.setItem(keys.ADMIN_USERS_STORAGE_KEY,JSON.stringify([{id:8,username:'fallback',roleCode:'labAdmin',name:'备选管理员'}]));expect(await auth.getCurrentUser()).toMatchObject({id:8,username:'fallback',name:'备选管理员'});localStorage.setItem(keys.ADMIN_USERS_STORAGE_KEY,'[]');expect(await auth.updateCurrentUserProfile({username:'new',realName:'新用户'})).toMatchObject({username:'new',name:'新用户',roleCode:'labAdmin'})})
it('cached records without one identity field cannot shadow a matching username or ID', async () => {
  localStorage.setItem(keys.ADMIN_USERS_STORAGE_KEY, JSON.stringify([
    { roleCode: 'systemAdmin' },
    { id: 44, roleCode: 'labAdmin', realName: '按编号查找' },
    { username: 'specific', roleCode: 'labAdmin', realName: '按用户名查找' },
  ]))
  expect((await auth.loginByPassword({ username: 'specific' })).user.name).toBe('按用户名查找')
  auth.persistAuthSession({ role: 'labAdmin', user: { id: 44, username: 'not-cached' } })
  expect((await auth.getCurrentUser()).name).toBe('按编号查找')
  localStorage.removeItem(keys.ADMIN_USERS_STORAGE_KEY)
  auth.clearAuthSession()
  expect((await auth.loginByPassword({ username: 'unconfigured' })).user.roleCode).toBe('labAdmin')
})
