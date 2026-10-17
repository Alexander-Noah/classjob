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
  维护记录管理: ['/lab-admin/maintenance-records', '/lab-admin/maintenance-records/new'],
  维护记录填写: ['/lab-admin/maintenance-records', '/lab-admin/maintenance-records/new'],
  维护记录查看: ['/lab-admin/maintenance-records'],
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
}

const CONTROLLED_PATHS = Object.values(PERMISSION_ROUTE_MAP).flat()
const EXACT_PERMISSION_PATHS = new Set([
  '/lab-admin/maintenance-records',
  '/lab-admin/maintenance-records/new',
])
const LEGACY_PERMISSION_ITEM_MAP = {
  故障报修: ['维护记录填写', '维护记录查看'],
}

/**
 * expand权限条目的处理逻辑。
 * 入参：item（条目）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const expandPermissionItem = (item) => LEGACY_PERMISSION_ITEM_MAP[item] || [item]

/**
 * 获取Allowed路径。
 * 入参：permissionItems（权限条目集合）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function getAllowedPaths(permissionItems) {
  if (!Array.isArray(permissionItems) || !permissionItems.length) return []
  return permissionItems.flatMap(/** 转换并展开当前条目。 入参：item。 */ (item) => PERMISSION_ROUTE_MAP[item] || [])
}

/**
 * 判断是否为Controlled路径。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function isControlledPath(path) {
  return CONTROLLED_PATHS.some(/** 判断当前条目是否满足任一匹配条件。 入参：controlled。 */ (controlled) => path.startsWith(controlled))
}

/**
 * 空权限列表沿用角色默认菜单；受控路径按授权项匹配。
 * 维护记录“查看”和“填写”使用精确匹配，避免父路径授权连带开放填写入口。
 */
export function isPathAllowed(path, permissionItems) {
  if (!permissionItems || !permissionItems.length) return true
  if (!isControlledPath(path)) return true

  const allowedPaths = getAllowedPaths(permissionItems)
  return allowedPaths.some(/** 判断当前条目是否满足任一匹配条件。 入参：allowed。 */ (allowed) =>
    EXACT_PERMISSION_PATHS.has(allowed) ? path === allowed : path.startsWith(allowed),
  )
}

/** 兼容 JSON 字符串、分组数组及旧权限名称，输出去重后的权限项名称。 */
export function parseUserPermissions(user) {
  if (!user?.permissions) return []

  try {
    const parsed = typeof user.permissions === 'string' ? JSON.parse(user.permissions) : user.permissions

    if (Array.isArray(parsed)) {
      return [...new Set(parsed.flatMap(/** 转换并展开当前条目。 入参：group。 */ (group) => {
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
