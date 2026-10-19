/*
 * 身份认证共享常量与映射，修改取值时同步检查使用方。
 * 主要导出：AUTH_STORAGE_KEY、AUTH_ROLE_STORAGE_KEY、AUTH_TOKEN_STORAGE_KEY、AUTH_USER_STORAGE_KEY、AUTH_REMEMBER_ENABLED_STORAGE_KEY 等。
 */
export const AUTH_STORAGE_KEY = 'smart-lab-authenticated'
export const AUTH_ROLE_STORAGE_KEY = 'smart-lab-role'
export const AUTH_TOKEN_STORAGE_KEY = 'smart-lab-token'
export const AUTH_USER_STORAGE_KEY = 'smart-lab-user'
export const AUTH_REMEMBER_ENABLED_STORAGE_KEY = 'smart-lab-remember-enabled'
export const AUTH_REMEMBER_USERNAME_STORAGE_KEY = 'smart-lab-remember-username'
export const ADMIN_USERS_STORAGE_KEY = 'smart-lab:admin-users:records'

export const DEFAULT_AUTH_ROLE = 'labAdmin'

// 暂停角色只关闭入口与会话，不删除其页面、接口、路由定义和账号数据。
const RETIRED_ROLE_VALUES = new Set(['teacher', '教师', 'maintenance', '维修人员'])

/** 角色编码和中文名称均可用于判断，避免旧缓存绕过停用状态。 */
export const isRetiredRole = (value) => RETIRED_ROLE_VALUES.has(String(value ?? '').trim().toLowerCase())

export const AUTH_ROLE_DEFAULT_PATH = {
  labAdmin: '/lab-admin',
  systemAdmin: '/system-admin',
  teacher: '/teacher',
}
