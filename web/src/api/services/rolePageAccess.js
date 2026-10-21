import { shallowRef } from 'vue'
import http from '../http'
import { createRecoveryPolicy } from '../../constants/rolePageAccess'

export const currentPageAccess = shallowRef(null)

export async function refreshPageAccess(role) {
  // 先清理上一角色缓存，失败时不沿用旧角色或旧授权。
  currentPageAccess.value = createRecoveryPolicy(role)
  const { data } = await http.get('/auth/page-access', { skipErrorMessage: true })
  const policy = data?.data
  if (policy?.role !== role || !Array.isArray(policy.accessiblePaths) || !Array.isArray(policy.visiblePaths)) {
    throw new Error('无法取得当前角色页面权限')
  }
  currentPageAccess.value = policy
  return policy
}

export async function listAccessRoles() {
  const { data } = await http.get('/role-page-access')
  return data.data
}
export async function getRolePageAccess(role) {
  const { data } = await http.get(`/role-page-access/${encodeURIComponent(role)}`)
  return data.data
}
export async function saveRolePageAccess(role, policy) {
  const { data } = await http.put(`/role-page-access/${encodeURIComponent(role)}`, policy)
  return data.data
}
