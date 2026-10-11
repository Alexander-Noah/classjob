/**
 * 页面查看权限和路由路径的映射。
 *
 * 权限项名称需要和用户权限配置里的勾选项保持一致。
 */
export const PERMISSION_ROUTE_MAP = {
  用户管理: ['/system-admin/users'],
  用户权限: ['/system-admin/users'],
  平台统计: ['/system-admin/statistics'],
  通知公告: ['/lab-admin/notices', '/system-admin/notices'],

  实验室管理: ['/lab-admin/labs'],
  设备资产: ['/lab-admin/devices'],
  预约管理: ['/lab-admin/reservations'],
  维护记录填写: ['/lab-admin/maintenance-records/new'],
  维护记录查看: ['/lab-admin/maintenance-records'],
  使用记录: ['/lab-admin/records'],
  统计分析: ['/lab-admin/statistics'],
  课表抓取: ['/lab-admin/class-timetable', '/lab-admin/class-timetable-data'],
  课表数据: ['/lab-admin/class-timetable', '/lab-admin/class-timetable-data', '/teacher/class-timetable-data'],

  教师课表: ['/teacher/schedule'],
}

const CONTROLLED_PATHS = Object.values(PERMISSION_ROUTE_MAP).flat()
const EXACT_PERMISSION_PATHS = new Set([
  '/lab-admin/maintenance-records',
  '/lab-admin/maintenance-records/new',
])
const LEGACY_PERMISSION_ITEM_MAP = {
  故障报修: ['维护记录填写', '维护记录查看'],
}

const expandPermissionItem = (item) => LEGACY_PERMISSION_ITEM_MAP[item] || [item]

export function getAllowedPaths(permissionItems) {
  if (!Array.isArray(permissionItems) || !permissionItems.length) return []
  return permissionItems.flatMap((item) => PERMISSION_ROUTE_MAP[item] || [])
}

export function isControlledPath(path) {
  return CONTROLLED_PATHS.some((controlled) => path.startsWith(controlled))
}

export function isPathAllowed(path, permissionItems) {
  if (!permissionItems || !permissionItems.length) return true
  if (!isControlledPath(path)) return true

  const allowedPaths = getAllowedPaths(permissionItems)
  return allowedPaths.some((allowed) =>
    EXACT_PERMISSION_PATHS.has(allowed) ? path === allowed : path.startsWith(allowed),
  )
}

export function parseUserPermissions(user) {
  if (!user?.permissions) return []

  try {
    const parsed = typeof user.permissions === 'string' ? JSON.parse(user.permissions) : user.permissions

    if (Array.isArray(parsed)) {
      return [...new Set(parsed.flatMap((group) => {
        if (typeof group === 'string') return expandPermissionItem(group)
        if (Array.isArray(group?.items)) return group.items.flatMap(expandPermissionItem)
        return []
      }))]
    }
  } catch {
    // 本地缓存异常时按无自定义权限处理，避免阻断正常登录。
  }

  return []
}
