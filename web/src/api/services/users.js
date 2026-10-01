import http from '../http'
import { API_USE_MOCK } from '../config'
import { API_ENDPOINTS } from '../endpoints'
import { ADMIN_USERS_STORAGE_KEY } from '../../constants/auth'

const ROLE_CODE_MAP = {
  系统管理员: 'systemAdmin',
  实验室管理员: 'labAdmin',
  教师: 'teacher',
  维修人员: 'maintenance',
  教务人员: 'academic',
}

const ROLE_NAME_MAP = Object.fromEntries(Object.entries(ROLE_CODE_MAP).map(([name, code]) => [code, name]))

const PERMISSIONS_BY_ROLE = {
  系统管理员: [{ group: '平台治理', items: ['用户管理', '系统设置', '激活管理', '平台统计', '通知公告'] }],
  实验室管理员: [
    { group: '资源管理', items: ['实验室管理', '课表数据', '预约管理'] },
    { group: '资产管理', items: ['设备资产', '耗材库存', '设备状态'] },
    { group: '环境运维', items: ['环境管理', '故障报修', '物联网扩展'] },
    { group: '数据分析', items: ['使用记录', '统计分析', 'AI智能助手'] },
    { group: '系统通知', items: ['通知公告'] },
  ],
  教师: [{ group: '个人工作', items: ['教师课表', '课程环境'] }],
  维修人员: [{ group: '个人工作', items: ['维修工单'] }],
  教务人员: [{ group: '教务管理', items: ['课表数据', '通知公告'] }],
}

const MOCK_USERS = [
  {
    id: 1,
    username: 'sysadmin',
    employeeNo: 'SA-0001',
    realName: '陈老师',
    roleCode: 'systemAdmin',
    roleName: '系统管理员',
    department: '信息中心',
    phone: '13900000002',
    email: 'sysadmin@smartlab.edu',
    avatarUrl: '',
    status: 1,
    builtIn: true,
    remark: '系统内置管理员账号',
    lastLogin: '今天 09:12',
    createdAt: '2026-03-01 09:00',
    permissions: JSON.stringify(PERMISSIONS_BY_ROLE.系统管理员),
    logs: [
      { time: '今天 09:12', content: '登录系统并查看平台配置', type: 'primary' },
      { time: '昨天 16:00', content: '发布平台维护公告', type: 'success' },
    ],
  },
  {
    id: 2,
    username: 'labadmin',
    employeeNo: 'LA-0101',
    realName: '张明',
    roleCode: 'labAdmin',
    roleName: '实验室管理员',
    department: '实验中心',
    phone: '13800000001',
    email: 'labadmin@smartlab.edu',
    avatarUrl: '',
    status: 1,
    remark: '负责基础资源与实验室日常运维',
    lastLogin: '今天 08:40',
    createdAt: '2026-03-05 10:30',
    permissions: JSON.stringify(PERMISSIONS_BY_ROLE.实验室管理员),
    logs: [
      { time: '今天 08:40', content: '处理课程环境确认', type: 'primary' },
      { time: '昨天 14:20', content: '分派维修工单', type: 'warning' },
    ],
  },
  {
    id: 3,
    username: 'teacher01',
    employeeNo: 'T-2026001',
    realName: '李老师',
    roleCode: 'teacher',
    roleName: '教师',
    department: '软件学院',
    phone: '13800000003',
    email: 'teacher01@school.edu',
    avatarUrl: '',
    status: 1,
    remark: '承担软件工程课程教学',
    lastLogin: '今天 10:05',
    createdAt: '2026-03-12 14:12',
    permissions: JSON.stringify(PERMISSIONS_BY_ROLE.教师),
    logs: [{ time: '今天 10:05', content: '查看本周课表', type: 'success' }],
  },
  {
    id: 4,
    username: 'repair01',
    employeeNo: 'M-0042',
    realName: '周工',
    roleCode: 'maintenance',
    roleName: '维修人员',
    department: '运维组',
    phone: '13700000004',
    email: 'repair01@school.edu',
    avatarUrl: '',
    status: 1,
    remark: '负责设备维修工单处理',
    lastLogin: '今天 11:20',
    createdAt: '2026-03-18 08:30',
    permissions: JSON.stringify(PERMISSIONS_BY_ROLE.维修人员),
    logs: [{ time: '今天 11:20', content: '接收维修派单', type: 'warning' }],
  },
  {
    id: 5,
    username: 'academic01',
    employeeNo: 'JW-0066',
    realName: '杨君豪',
    roleCode: 'academic',
    roleName: '教务人员',
    department: '教务处',
    phone: '13600000005',
    email: 'academic01@school.edu',
    avatarUrl: '',
    status: 1,
    remark: '维护课表数据与教务通知',
    lastLogin: '今天 09:30',
    createdAt: '2026-03-20 13:20',
    permissions: JSON.stringify(PERMISSIONS_BY_ROLE.教务人员),
    logs: [{ time: '今天 09:30', content: '查看教务课表数据', type: 'primary' }],
  },
  {
    id: 6,
    username: 'teacher02',
    employeeNo: 'T-2026002',
    realName: '黄睿',
    roleCode: 'teacher',
    roleName: '教师',
    department: '软件学院',
    phone: '13800000005',
    email: 'teacher02@school.edu',
    avatarUrl: '',
    status: 0,
    remark: '学期结束后临时停用',
    lastLogin: '06-25 15:40',
    createdAt: '2026-04-02 09:45',
    permissions: JSON.stringify(PERMISSIONS_BY_ROLE.教师),
    logs: [{ time: '06-25 15:40', content: '账号由管理员停用', type: 'info' }],
  },
]

const cloneRecords = (records) => JSON.parse(JSON.stringify(records || []))

const getRoleName = (record = {}) => record.roleName || ROLE_NAME_MAP[record.roleCode] || record.role || '教师'

const getRoleCode = (record = {}) => record.roleCode || ROLE_CODE_MAP[getRoleName(record)] || 'teacher'

const getDefaultPermissions = (roleName) => JSON.stringify(PERMISSIONS_BY_ROLE[roleName] || PERMISSIONS_BY_ROLE.教师)

const ensureUserShape = (record = {}) => {
  const roleName = getRoleName(record)

  return {
    ...record,
    username: record.username || `user${record.id || Date.now()}`,
    employeeNo: record.employeeNo || record.jobNo || record.username || '',
    realName: record.realName || record.name || record.username || '未命名账号',
    roleCode: getRoleCode(record),
    roleName,
    department: record.department || '未设置',
    phone: record.phone || '',
    email: record.email || '',
    status: Number(record.status ?? (record.enabled === false ? 0 : 1)),
    permissions: record.permissions || getDefaultPermissions(roleName),
    lastLogin: record.lastLogin || '暂无登录记录',
    createdAt: record.createdAt || '2026-06-01 09:00',
    logs: Array.isArray(record.logs)
      ? record.logs
      : [{ time: '刚刚', content: '账号信息已从用户中心同步', type: 'primary' }],
  }
}

const readMockUsers = () => {
  if (typeof window === 'undefined') return cloneRecords(MOCK_USERS).map(ensureUserShape)

  try {
    const rawValue = window.localStorage.getItem(ADMIN_USERS_STORAGE_KEY)
    const parsedValue = rawValue ? JSON.parse(rawValue) : null
    return (Array.isArray(parsedValue) ? parsedValue : cloneRecords(MOCK_USERS)).map(ensureUserShape)
  } catch {
    return cloneRecords(MOCK_USERS).map(ensureUserShape)
  }
}

const writeMockUsers = (users) => {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(ADMIN_USERS_STORAGE_KEY, JSON.stringify(users.map(ensureUserShape)))
}

const normalizeUsersResponse = (data) => {
  if (Array.isArray(data?.data?.records)) return data.data.records
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

const normalizeTotalResponse = (data, fallbackLength) =>
  Number(data?.data?.total ?? data?.total ?? fallbackLength ?? 0)

const normalizeUserResponse = (data, fallbackUser) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackUser
}

const filterMockUsers = (users, params = {}) => {
  const keyword = String(params.keyword || '').trim().toLowerCase()
  const role = params.role || params.roleName || ''
  const status = params.status ?? ''
  const department = params.department || ''

  return users.filter((user) => {
    const matchKeyword =
      !keyword ||
      [user.realName, user.username, user.employeeNo, user.phone, user.email, user.department]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(keyword))
    const matchRole = !role || role === '全部' || user.roleName === role
    const matchDepartment = !department || department === '全部' || user.department === department
    const matchStatus =
      status === '' ||
      status === undefined ||
      status === '全部' ||
      (status === '启用' && Number(user.status) === 1) ||
      (status === '停用' && Number(user.status) === 0)

    return matchKeyword && matchRole && matchDepartment && matchStatus
  })
}

const updateMockUsersByIds = (ids, mapper) => {
  const idSet = new Set(ids.map((id) => String(id)))
  const users = readMockUsers()
  const nextUsers = users.map((user) => (idSet.has(String(user.id)) ? mapper(user) : user))
  writeMockUsers(nextUsers)
  return nextUsers.filter((user) => idSet.has(String(user.id)))
}

const getMockUserPage = (params = {}) => {
  const users = filterMockUsers(readMockUsers(), params)
  const page = Number(params.page || params.currentPage || 1)
  const pageSize = Number(params.pageSize || params.size || users.length || 10)
  const start = (page - 1) * pageSize

  return {
    records: users.slice(start, start + pageSize),
    total: users.length,
  }
}

const createMockUser = (payload) => {
  const users = readMockUsers()
  const roleName = payload.roleName || ROLE_NAME_MAP[payload.roleCode] || payload.role || '教师'
  const savedUser = ensureUserShape({
    ...payload,
    id: Date.now(),
    username: payload.username || `user${Date.now()}`,
    roleName,
    roleCode: ROLE_CODE_MAP[roleName] || payload.roleCode || 'teacher',
    status: payload.status ?? 1,
    permissions: payload.permissions || getDefaultPermissions(roleName),
    createdAt: payload.createdAt || new Date().toLocaleString('zh-CN', { hour12: false }),
    lastLogin: '暂无登录记录',
    logs: [{ time: '刚刚', content: '系统管理员创建账号', type: 'primary' }],
  })
  writeMockUsers([savedUser, ...users])
  return savedUser
}

const updateMockUser = (id, payload) => {
  const users = readMockUsers()
  const nextUsers = users.map((user) =>
    String(user.id) === String(id)
      ? ensureUserShape({
          ...user,
          ...payload,
          id: user.id,
          logs: [{ time: '刚刚', content: '系统管理员更新账号资料', type: 'success' }, ...(user.logs || [])].slice(0, 6),
        })
      : user,
  )
  const savedUser = nextUsers.find((user) => String(user.id) === String(id)) || ensureUserShape({ ...payload, id })
  writeMockUsers(nextUsers.some((user) => String(user.id) === String(id)) ? nextUsers : [savedUser, ...users])
  return savedUser
}

const deleteMockUser = (id) => {
  const users = readMockUsers()
  const deletedUser = users.find((user) => String(user.id) === String(id))
  writeMockUsers(users.filter((user) => String(user.id) !== String(id)))
  return deletedUser || { id }
}

const updateMockUserStatus = (id, status) => {
  const nextStatus = status === '启用' || status === 1 || status === true ? 1 : 0
  const updatedUsers = updateMockUsersByIds([id], (user) =>
    ensureUserShape({
      ...user,
      status: nextStatus,
      logs: [
        { time: '刚刚', content: `系统管理员${nextStatus ? '启用' : '停用'}账号`, type: nextStatus ? 'success' : 'warning' },
        ...(user.logs || []),
      ].slice(0, 6),
    }),
  )
  return updatedUsers[0] || { id, status: nextStatus }
}

const resetMockUserPassword = (id) => {
  const updatedUsers = updateMockUsersByIds([id], (user) =>
    ensureUserShape({
      ...user,
      logs: [{ time: '刚刚', content: '系统管理员重置登录密码', type: 'warning' }, ...(user.logs || [])].slice(0, 6),
    }),
  )
  return updatedUsers[0] || { id }
}

const batchDeleteMockUsers = (ids = []) => {
  const idSet = new Set(ids.map((id) => String(id)))
  const users = readMockUsers()
  const deletedUsers = users.filter((user) => idSet.has(String(user.id)))
  writeMockUsers(users.filter((user) => !idSet.has(String(user.id))))
  return deletedUsers
}

export async function getUserPage(params = {}) {
  if (API_USE_MOCK) return getMockUserPage(params)

  const { data } = await http.get(API_ENDPOINTS.users.list, { params })
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '获取用户列表失败')
  }

  const users = filterMockUsers(normalizeUsersResponse(data).map(ensureUserShape), params)
  const page = Number(params.page || params.currentPage || 1)
  const pageSize = Number(params.pageSize || params.size || users.length || 10)
  const start = (page - 1) * pageSize

  return {
    records: users.slice(start, start + pageSize),
    total: normalizeTotalResponse(data, users.length),
  }
}

export async function listUsers(params = {}) {
  const pageData = await getUserPage({ ...params, page: 1, pageSize: 1000 })
  return pageData.records
}

export async function listUserOptions() {
  if (API_USE_MOCK) return readMockUsers().map(ensureUserShape)

  const { data } = await http.get(API_ENDPOINTS.users.options)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '获取用户选项失败')
  }

  return normalizeUsersResponse(data).map(ensureUserShape)
}

export async function createUser(payload) {
  if (API_USE_MOCK) return createMockUser(payload)

  const { data } = await http.post(API_ENDPOINTS.users.list, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '创建用户失败')
  }

  return normalizeUserResponse(data, payload)
}

export async function updateUser(id, payload) {
  if (API_USE_MOCK) return updateMockUser(id, payload)

  const { data } = await http.put(`${API_ENDPOINTS.users.detail}/${id}`, payload)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '更新用户失败')
  }

  return normalizeUserResponse(data, payload)
}

export async function deleteUser(id) {
  if (API_USE_MOCK) return deleteMockUser(id)

  const { data } = await http.delete(`${API_ENDPOINTS.users.detail}/${id}`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '删除用户失败')
  }

  return normalizeUserResponse(data, { id })
}

export async function updateUserStatus(id, status) {
  const nextStatus = status === '启用' || status === 1 || status === true ? 1 : 0
  if (API_USE_MOCK) return updateMockUserStatus(id, nextStatus)

  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/${id}/status`, {
    status: nextStatus,
  })
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '更新用户状态失败')
  }

  return normalizeUserResponse(data, { id, status: nextStatus })
}

export async function resetUserPassword(id) {
  if (API_USE_MOCK) return resetMockUserPassword(id)

  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/${id}/reset-password`)
  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '重置密码失败')
  }

  return normalizeUserResponse(data, { id })
}

export async function batchEnableUsers(ids = []) {
  if (API_USE_MOCK) return updateMockUsersByIds(ids, (user) => ensureUserShape({ ...user, status: 1 }))

  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/batch/enable`, { ids })
  if (data?.code && data.code !== 200) throw new Error(data.message || '批量启用失败')
  return normalizeUsersResponse(data)
}

export async function batchDisableUsers(ids = []) {
  if (API_USE_MOCK) return updateMockUsersByIds(ids, (user) => ensureUserShape({ ...user, status: 0 }))

  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/batch/disable`, { ids })
  if (data?.code && data.code !== 200) throw new Error(data.message || '批量停用失败')
  return normalizeUsersResponse(data)
}

export async function batchDeleteUsers(ids = []) {
  if (API_USE_MOCK) return batchDeleteMockUsers(ids)

  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/batch/delete`, { ids })
  if (data?.code && data.code !== 200) throw new Error(data.message || '批量删除失败')
  return normalizeUsersResponse(data)
}

export async function batchUpdateRole(ids = [], role) {
  const roleCode = ROLE_CODE_MAP[role] || 'teacher'
  if (API_USE_MOCK) {
    return updateMockUsersByIds(ids, (user) =>
      ensureUserShape({
        ...user,
        roleName: role,
        roleCode,
        permissions: getDefaultPermissions(role),
      }),
    )
  }

  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/batch/role`, { ids, role, roleCode })
  if (data?.code && data.code !== 200) throw new Error(data.message || '批量分配角色失败')
  return normalizeUsersResponse(data)
}

export async function batchUpdateDepartment(ids = [], department) {
  if (API_USE_MOCK) {
    return updateMockUsersByIds(ids, (user) => ensureUserShape({ ...user, department }))
  }

  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/batch/department`, { ids, department })
  if (data?.code && data.code !== 200) throw new Error(data.message || '批量调整部门失败')
  return normalizeUsersResponse(data)
}

export async function importUsers(file) {
  if (API_USE_MOCK) {
    return {
      pending: true,
      fileName: file?.name || '',
      message: '导入功能待后端接口接入',
    }
  }

  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post(`${API_ENDPOINTS.users.detail}/import`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  if (data?.code && data.code !== 200) throw new Error(data.message || '批量导入失败')
  return data?.data || data
}

export async function exportUsers(params = {}) {
  if (API_USE_MOCK) {
    return {
      pending: true,
      params,
      message: '导出功能待后端接口接入',
    }
  }

  const { data } = await http.get(`${API_ENDPOINTS.users.detail}/export`, { params, responseType: 'blob' })
  return data
}

export async function uploadUserAvatar(file) {
  if (API_USE_MOCK) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve({ url: reader.result, objectKey: `mock/avatar-${Date.now()}` })
      reader.onerror = () => reject(new Error('头像读取失败'))
      reader.readAsDataURL(file)
    })
  }

  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post(API_ENDPOINTS.users.avatar, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  if (data?.code && data.code !== 200) {
    throw new Error(data.message || '头像上传失败')
  }

  return data?.data || data
}
