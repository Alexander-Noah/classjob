import http from '../http'
import { API_ENDPOINTS } from '../endpoints'
import { API_USE_MOCK } from '../config'
import {
  AUTH_ROLE_STORAGE_KEY,
  AUTH_REMEMBER_ENABLED_STORAGE_KEY,
  AUTH_REMEMBER_USERNAME_STORAGE_KEY,
  AUTH_STORAGE_KEY,
  AUTH_TOKEN_STORAGE_KEY,
  AUTH_USER_STORAGE_KEY,
  ADMIN_USERS_STORAGE_KEY,
  DEFAULT_AUTH_ROLE,
} from '../../constants/auth'

const PROFILE_REFRESH_INTERVAL = 8000
export const RETIRED_ROLE_MESSAGE = '角色已停用，请联系系统管理员重新分配角色'

let lastProfileRefreshAt = 0

const ROLE_CODE_MAP = {
  系统管理员: 'systemAdmin',
  实验室管理员: 'labAdmin',
  教师: 'teacher',
}

const ROLE_NAME_MAP = Object.fromEntries(Object.entries(ROLE_CODE_MAP).map(([name, code]) => [code, name]))

const DEFAULT_PERMISSION_GROUPS = {
  systemAdmin: [{ group: '平台治理', items: ['用户管理', '系统设置', '激活管理', '平台统计', '通知公告'] }],
  labAdmin: [
    { group: '资源管理', items: ['实验室管理', '课表数据', '预约管理'] },
    { group: '资产管理', items: ['设备资产', '耗材库存', '设备状态'] },
    { group: '环境运维', items: ['环境管理', '维护记录填写', '维护记录查看', '物联网扩展'] },
    { group: '数据分析', items: ['使用记录', '统计分析', 'AI智能助手'] },
    { group: '系统通知', items: ['通知公告'] },
  ],
  teacher: [{ group: '个人工作', items: ['教师课表', '课程环境'] }],
}

const MOCK_ROLE_USERS = {
  labAdmin: {
    id: 'mock-lab-admin',
    username: 'labadmin',
    name: '张明',
    realName: '张明',
    role: '实验室管理员',
    roleCode: 'labAdmin',
    roleName: '实验室管理员',
    permissions: JSON.stringify(DEFAULT_PERMISSION_GROUPS.labAdmin),
  },
  systemAdmin: {
    id: 'mock-system-admin',
    username: 'sysadmin',
    name: '陈老师',
    realName: '陈老师',
    role: '系统管理员',
    roleCode: 'systemAdmin',
    roleName: '系统管理员',
    permissions: JSON.stringify(DEFAULT_PERMISSION_GROUPS.systemAdmin),
  },
  teacher: {
    id: 'mock-teacher',
    username: 'teacher01',
    name: '李老师',
    realName: '李老师',
    role: '教师',
    roleCode: 'teacher',
    roleName: '教师',
    permissions: JSON.stringify(DEFAULT_PERMISSION_GROUPS.teacher),
  },
}

const MOCK_USERNAME_ROLE_MAP = {
  admin: 'systemAdmin',
  sysadmin: 'systemAdmin',
  system: 'systemAdmin',
  labadmin: 'labAdmin',
  lab: 'labAdmin',
  teacher: 'teacher',
  teacher01: 'teacher',
  academic: 'teacher',
  academic01: 'teacher',
  jw: 'teacher',
  jiaowu: 'teacher',
}

const RETIRED_MOCK_USERNAMES = new Set(['repair', 'repair01', 'maintenance'])

const isRetiredRoleValue = (value) => value === 'maintenance' || value === '维修人员'

export function assertActiveSessionRole(role, user = {}) {
  if ([role, user?.roleCode, user?.role, user?.roleName].some(isRetiredRoleValue)) {
    const error = new Error(RETIRED_ROLE_MESSAGE)
    error.code = 'ROLE_RETIRED'
    throw error
  }
}

const isRetiredRoleError = (error) =>
  error?.code === 'ROLE_RETIRED' ||
  error?.message === RETIRED_ROLE_MESSAGE ||
  error?.userMessage === RETIRED_ROLE_MESSAGE ||
  error?.response?.data?.message === RETIRED_ROLE_MESSAGE

const MOCK_ACCOUNT_USERS = {
  academic01: {
    id: 'mock-academic01',
    username: 'academic01',
    name: '杨君豪',
    realName: '杨君豪',
    role: '教师',
    roleCode: 'teacher',
    roleName: '教师',
    department: '教务处',
    permissions: JSON.stringify([
      { group: '个人工作', items: ['教师课表', '课程环境'] },
      { group: '教务功能', items: ['课表数据'] },
    ]),
  },
}

const inferMockRoleByUsername = (username = '') => {
  const normalizedUsername = String(username || '').trim().toLowerCase()
  return MOCK_USERNAME_ROLE_MAP[normalizedUsername] || DEFAULT_AUTH_ROLE
}

const resolveRoleCode = (user = {}, fallbackRole = DEFAULT_AUTH_ROLE) => {
  const rawRole = user.roleCode || user.role || user.roleName || fallbackRole

  if (ROLE_NAME_MAP[rawRole]) return rawRole
  if (ROLE_CODE_MAP[rawRole]) return ROLE_CODE_MAP[rawRole]
  return ROLE_NAME_MAP[fallbackRole] ? fallbackRole : DEFAULT_AUTH_ROLE
}

const normalizeSessionUser = (user = {}, fallbackRole = DEFAULT_AUTH_ROLE) => {
  assertActiveSessionRole(fallbackRole, user)
  const roleCode = resolveRoleCode(user, fallbackRole)
  const roleName = user.roleName || ROLE_NAME_MAP[roleCode] || user.role || '实验室管理员'
  const realName = user.realName || user.name || user.username || roleName

  return {
    ...user,
    id: user.id || `mock-${roleCode}`,
    username: user.username || MOCK_ROLE_USERS[roleCode]?.username || '',
    name: realName,
    realName,
    role: roleName,
    roleCode,
    roleName,
    permissions: user.permissions || JSON.stringify(DEFAULT_PERMISSION_GROUPS[roleCode] || []),
  }
}

const readMockUsers = () => {
  const localStorage = getLocalStorage()
  if (!localStorage) return []

  try {
    const rawValue = localStorage.getItem(ADMIN_USERS_STORAGE_KEY)
    const parsedValue = rawValue ? JSON.parse(rawValue) : null
    return Array.isArray(parsedValue) ? parsedValue : []
  } catch {
    return []
  }
}

const findMockUser = ({ role, username, id } = {}) => {
  const normalizedUsername = String(username || '').trim().toLowerCase()
  const normalizedId = String(id || '').trim()
  const users = readMockUsers()
  const matchedUser = users.find((user) => {
    const sameUsername = normalizedUsername && String(user.username || '').trim().toLowerCase() === normalizedUsername
    const sameId = normalizedId && String(user.id || '').trim() === normalizedId
    return sameUsername || sameId
  })

  if (matchedUser) return matchedUser

  if (MOCK_ACCOUNT_USERS[normalizedUsername]) return MOCK_ACCOUNT_USERS[normalizedUsername]

  const roleCode = role || inferMockRoleByUsername(username)
  return users.find((user) => resolveRoleCode(user, DEFAULT_AUTH_ROLE) === roleCode) || MOCK_ROLE_USERS[roleCode] || MOCK_ROLE_USERS[DEFAULT_AUTH_ROLE]
}

const getLocalStorage = () => {
  if (typeof window === 'undefined') return null
  return window.localStorage
}

const getSessionStorage = () => {
  if (typeof window === 'undefined') return null
  return window.sessionStorage
}

const getAuthStorage = (remember = true) => (remember ? getLocalStorage() : getSessionStorage())

const getAuthRememberMode = () => {
  const localStorage = getLocalStorage()
  const sessionStorage = getSessionStorage()
  if (localStorage?.getItem(AUTH_STORAGE_KEY) === 'true') return true
  if (sessionStorage?.getItem(AUTH_STORAGE_KEY) === 'true') return false
  return true
}

const readStorage = (storageKey) => {
  const localStorage = getLocalStorage()
  const sessionStorage = getSessionStorage()
  return localStorage?.getItem(storageKey) || sessionStorage?.getItem(storageKey) || ''
}

const writeStorage = (storageKey, value, remember = true) => {
  const targetStorage = getAuthStorage(remember)
  const mirrorStorage = remember ? getSessionStorage() : getLocalStorage()

  mirrorStorage?.removeItem(storageKey)
  targetStorage?.setItem(storageKey, value)
}

const removeStorage = (storageKey) => {
  getLocalStorage()?.removeItem(storageKey)
  getSessionStorage()?.removeItem(storageKey)
}

export function getStoredRole() {
  return readStorage(AUTH_ROLE_STORAGE_KEY) || DEFAULT_AUTH_ROLE
}

export function getStoredUser() {
  const rawValue = readStorage(AUTH_USER_STORAGE_KEY)

  if (!rawValue) return null

  try {
    return JSON.parse(rawValue)
  } catch {
    return null
  }
}

export function getAuthToken() {
  return readStorage(AUTH_TOKEN_STORAGE_KEY)
}

export function isAuthenticated() {
  return readStorage(AUTH_STORAGE_KEY) === 'true'
}

export function getRememberedLogin() {
  const storage = getLocalStorage()
  const rememberedValue = storage?.getItem(AUTH_REMEMBER_ENABLED_STORAGE_KEY)
  const remember = rememberedValue === null ? true : rememberedValue === 'true'

  return {
    remember,
    username: remember ? storage?.getItem(AUTH_REMEMBER_USERNAME_STORAGE_KEY) || '' : '',
  }
}

export function persistRememberedLogin({ username, remember }) {
  const storage = getLocalStorage()
  if (!storage) return

  storage.setItem(AUTH_REMEMBER_ENABLED_STORAGE_KEY, String(Boolean(remember)))

  if (remember) {
    storage.setItem(AUTH_REMEMBER_USERNAME_STORAGE_KEY, String(username || '').trim())
    return
  }

  storage.removeItem(AUTH_REMEMBER_USERNAME_STORAGE_KEY)
}

export function persistAuthSession({ role, token, user, remember = true }) {
  assertActiveSessionRole(role, user)
  writeStorage(AUTH_STORAGE_KEY, 'true', remember)
  writeStorage(AUTH_ROLE_STORAGE_KEY, role || DEFAULT_AUTH_ROLE, remember)
  writeStorage(AUTH_TOKEN_STORAGE_KEY, token || '', remember)
  writeStorage(AUTH_USER_STORAGE_KEY, JSON.stringify(user || null), remember)
}

export function updateStoredUser(user, role) {
  const remember = getAuthRememberMode()
  const previousUser = getStoredUser()
  assertActiveSessionRole(role || getStoredRole(), { ...(previousUser || {}), ...(user || {}) })
  const nextUser = normalizeSessionUser({ ...(previousUser || {}), ...(user || {}) }, role || getStoredRole())
  const nextRole = role || nextUser.roleCode || getStoredRole()

  writeStorage(AUTH_ROLE_STORAGE_KEY, nextRole || DEFAULT_AUTH_ROLE, remember)
  writeStorage(AUTH_USER_STORAGE_KEY, JSON.stringify(nextUser), remember)

  return nextUser
}

export function clearAuthSession() {
  removeStorage(AUTH_STORAGE_KEY)
  removeStorage(AUTH_ROLE_STORAGE_KEY)
  removeStorage(AUTH_TOKEN_STORAGE_KEY)
  removeStorage(AUTH_USER_STORAGE_KEY)
}

export async function getCurrentUser(username) {
  if (API_USE_MOCK) {
    const storedUser = getStoredUser()
    return normalizeSessionUser(
      findMockUser({
        id: storedUser?.id,
        role: getStoredRole(),
        username: username || storedUser?.username,
      }),
      getStoredRole(),
    )
  }

  const { data } = await http.get(API_ENDPOINTS.auth.profile, {
    params: { username },
  })

  if (data?.code && data.code !== 200) {
    throw new Error(data?.message || '获取用户信息失败')
  }

  return data?.data || data || null
}

export async function updateCurrentUserProfile(payload = {}) {
  if (API_USE_MOCK) {
    const storedUser = getStoredUser()
    const role = getStoredRole()
    return normalizeSessionUser(
      {
        ...(storedUser || {}),
        realName: payload.realName || payload.name || storedUser?.realName,
        name: payload.realName || payload.name || storedUser?.name,
        department: payload.department ?? storedUser?.department,
        phone: payload.phone ?? storedUser?.phone,
        email: payload.email ?? storedUser?.email,
        avatarUrl: payload.avatarUrl ?? storedUser?.avatarUrl,
        username: storedUser?.username || payload.username,
      },
      role,
    )
  }

  const { data } = await http.put(API_ENDPOINTS.auth.profile, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data?.message || '更新个人资料失败')
  }

  return data?.data || data || null
}

export async function refreshCurrentUserSession({ force = false } = {}) {
  if (!isAuthenticated()) return null

  assertActiveSessionRole(getStoredRole(), getStoredUser())

  const now = Date.now()
  if (!force && now - lastProfileRefreshAt < PROFILE_REFRESH_INTERVAL) {
    return getStoredUser()
  }

  lastProfileRefreshAt = now

  const storedUser = getStoredUser()
  try {
    const profileUser = await getCurrentUser(storedUser?.username)
    if (!profileUser) return storedUser

    const nextUser = normalizeSessionUser(profileUser, getStoredRole())
    return updateStoredUser(nextUser, nextUser.roleCode)
  } catch (error) {
    if (isRetiredRoleError(error)) throw error
    return storedUser
  }
}

export async function loginByPassword(payload) {
  const shouldRemember = payload.remember !== false

  if (API_USE_MOCK) {
    if (RETIRED_MOCK_USERNAMES.has(String(payload.username || '').trim().toLowerCase())) {
      throw new Error(RETIRED_ROLE_MESSAGE)
    }
    const role = payload.role || inferMockRoleByUsername(payload.username)
    const user = normalizeSessionUser(findMockUser({ role, username: payload.username }), role)
    const session = {
      role,
      token: `mock-token-${role}-${Date.now()}`,
      user,
    }

    persistAuthSession({ ...session, remember: shouldRemember })
    return session
  }

  const loginPayload = { ...payload }
  delete loginPayload.remember
  const { data } = await http.post(API_ENDPOINTS.auth.login, loginPayload)
  if (data?.code && data.code !== 200) {
    throw new Error(data?.message || '登录失败')
  }

  const payloadData = data?.data || data
  const loginUser = payloadData?.user || payloadData || null
  const initialRole = loginUser?.roleCode || payloadData?.role || loginUser?.role || payload.role || DEFAULT_AUTH_ROLE
  const initialUser = loginUser ? normalizeSessionUser(loginUser, initialRole) : null
  const initialSession = {
    role: initialUser?.roleCode || initialRole,
    token: payloadData?.token || '',
    user: initialUser,
  }

  persistAuthSession({ ...initialSession, remember: shouldRemember })

  let profileUser = null
  try {
    profileUser = loginUser?.username ? await getCurrentUser(loginUser.username) : null
  } catch {
    profileUser = null
  }

  const user = profileUser ? normalizeSessionUser(profileUser, initialSession.role) : initialUser
  const session = {
    role: user?.roleCode || initialSession.role,
    token: initialSession.token,
    user,
  }
  persistAuthSession({ ...session, remember: shouldRemember })
  return session
}

export async function logout() {
  if (!API_USE_MOCK) {
    try {
      await http.post(API_ENDPOINTS.auth.logout)
    } catch {
      // ignore logout request errors and clear local session anyway
    }
  }

  clearAuthSession()
}
