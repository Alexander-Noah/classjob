/**
 * 页面查看权限和路由路径的映射。
 *
 * 权限项名称需要和用户权限配置里的勾选项保持一致。
 */
export const PERMISSION_ROUTE_MAP = {
  用户管理: ['/system-admin/users'],
  用户权限: ['/system-admin/users'],
  系统设置: ['/system-admin/system'],
  激活管理: ['/system-admin/activation-codes'],
  平台统计: ['/system-admin/statistics'],
  通知公告: ['/lab-admin/notices', '/system-admin/notices'],

  实验室管理: ['/lab-admin/labs'],
  设备资产: ['/lab-admin/devices'],
  环境管理: ['/lab-admin/environments'],
  预约管理: ['/lab-admin/reservations'],
  故障报修: ['/lab-admin/repairs'],
  耗材库存: ['/lab-admin/consumables'],
  使用记录: ['/lab-admin/records'],
  统计分析: ['/lab-admin/statistics'],
  AI智能助手: ['/lab-admin/ai-assistant'],
  物联网扩展: ['/lab-admin/room-control'],
  设备状态: ['/lab-admin/device-status'],
  接入授权: ['/lab-admin/device-status'],
  设备绑定: ['/lab-admin/device-status'],
  课表抓取: ['/lab-admin/class-timetable', '/lab-admin/class-timetable-data'],
  课表数据: ['/lab-admin/class-timetable', '/lab-admin/class-timetable-data', '/teacher/class-timetable-data'],

  教师课表: ['/teacher/schedule'],
  课程环境: ['/teacher/course-environments'],
  维修工单: ['/maintenance/work-orders'],
}

const CONTROLLED_PATHS = Object.values(PERMISSION_ROUTE_MAP).flat()

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
  return allowedPaths.some((allowed) => path.startsWith(allowed))
}

export function parseUserPermissions(user) {
  if (!user?.permissions) return []

  try {
    const parsed = typeof user.permissions === 'string' ? JSON.parse(user.permissions) : user.permissions

    if (Array.isArray(parsed)) {
      return parsed.flatMap((group) => {
        if (typeof group === 'string') return [group]
        if (Array.isArray(group?.items)) return group.items
        return []
      })
    }
  } catch {
    // 本地缓存异常时按无自定义权限处理，避免阻断正常登录。
  }

  return []
}
