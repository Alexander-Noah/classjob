/** 精确匹配页面，避免父页面授权意外开放未授权的子页面。 */
const HOME_PATHS = { labAdmin: '/lab-admin', systemAdmin: '/system-admin', teacher: '/teacher' }

export function createRecoveryPolicy(role) {
  const paths = HOME_PATHS[role] ? [HOME_PATHS[role]] : []
  if (role === 'systemAdmin') paths.push('/system-admin/role-access')
  return { role, accessiblePaths: paths, visiblePaths: paths, unavailable: true }
}

export function isRecoveryPage(role, path) {
  return createRecoveryPolicy(role).accessiblePaths.includes(path)
}

export function canAccessRolePage(policy, path) {
  return Boolean(policy && Array.isArray(policy.accessiblePaths) && policy.accessiblePaths.includes(path))
}

export function isRoleMenuVisible(policy, path) {
  return canAccessRolePage(policy, path) && Boolean(policy.visiblePaths?.includes(path))
}

export function filterRoleMenus(menus, policy) {
  return menus.flatMap(menu => {
    if (menu.children) {
      const children = filterRoleMenus(menu.children, policy)
      return children.length ? [{ ...menu, children }] : []
    }
    return isRoleMenuVisible(policy, menu.index) ? [menu] : []
  })
}
