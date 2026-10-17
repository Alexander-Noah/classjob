/*
 * navigation共享常量与映射，修改取值时同步检查使用方。
 * 主要导出：ROLE_LAYOUTS、getRoleLayout、getRoleMenus。
 */
import { DEFAULT_AUTH_ROLE } from './auth'
import { getRouteModulesByRole, ROLE_DEFAULT_PATHS, ROUTE_MODULES } from './routeModules'
import { isPathAllowed, parseUserPermissions } from './permissions'

export const ROLE_LAYOUTS = {
  labAdmin: {
    user: {
      name: '张明',
      role: '实验室管理员',
    },
    subtitle: '实验室运维工作台',
  },
  systemAdmin: {
    user: {
      name: '陈倩',
      role: '系统管理员',
    },
    subtitle: '平台配置与权限中心',
  },
  teacher: {
    user: {
      name: '李娜',
      role: '教师',
    },
    subtitle: '教师教学支持中心',
  },
}

/**
 * 获取角色Layout。
 * 入参：roleScope（角色Scope）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getRoleLayout = (roleScope = DEFAULT_AUTH_ROLE) =>
  ROLE_LAYOUTS[roleScope] || ROLE_LAYOUTS[DEFAULT_AUTH_ROLE]

const ROUTE_MODULE_BY_PATH = Object.fromEntries(
  ROUTE_MODULES.map(/** 将当前条目转换为目标结果。 入参：routeModule。 */ (routeModule) => [routeModule.path, routeModule]),
)

const LAB_ADMIN_MENU_GROUPS = [
  {
    index: 'lab-admin-basic-resources',
    title: '基础资源管理',
    iconPath: '/lab-admin/labs',
    children: ['/lab-admin/labs', '/lab-admin/consumables'],
  },
  {
    index: 'lab-admin-device-management',
    title: '设备管理',
    iconPath: '/lab-admin/devices',
    children: ['/lab-admin/devices', '/lab-admin/device-status', '/lab-admin/room-control'],
  },
  {
    index: 'lab-admin-teaching-reservations',
    title: '教学预约协同',
    iconPath: '/lab-admin/class-timetable',
    children: ['/lab-admin/class-timetable', '/lab-admin/class-timetable-data', '/lab-admin/reservations'],
  },
  {
    index: 'lab-admin-environment-operations',
    title: '实验环境运维',
    iconPath: '/lab-admin/environments',
    children: ['/lab-admin/environments'],
  },
  {
    index: 'lab-admin-maintenance-management',
    title: '实验室维护管理',
    iconPath: '/lab-admin/maintenance-records',
    children: ['/lab-admin/maintenance-records', '/lab-admin/notices'],
  },
  {
    index: 'lab-admin-data-intelligence',
    title: '数据统计与智能辅助',
    iconPath: '/lab-admin/statistics',
    children: ['/lab-admin/records', '/lab-admin/statistics', '/lab-admin/ai-assistant'],
  },
]

/**
 * 判断是否允许AccessMenu。
 * 入参：routeModule（routeModule）；roleScope（角色Scope）；permissionItems（权限条目集合）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const canAccessMenu = (routeModule, roleScope, permissionItems) =>
  !permissionItems.length ||
  routeModule.path === ROLE_DEFAULT_PATHS[roleScope] ||
  isPathAllowed(routeModule.path, permissionItems)

/**
 * 构建Menu条目。
 * 入参：routeModule（routeModule）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const buildMenuItem = (routeModule) => ({
  index: routeModule.path,
  title: routeModule.menuTitle,
  icon: routeModule.icon,
})

/**
 * 构建实验室AdminMenus。
 * 入参：permissionItems（权限条目集合）。
 * @returns 生成的数组。
 */
const buildLabAdminMenus = (permissionItems) => {
  const homeModule = ROUTE_MODULE_BY_PATH[ROLE_DEFAULT_PATHS.labAdmin]
  const homeMenu = homeModule && canAccessMenu(homeModule, 'labAdmin', permissionItems) ? [buildMenuItem(homeModule)] : []

  const groupedMenus = LAB_ADMIN_MENU_GROUPS.map(/** 将当前条目转换为目标结果。 入参：group。 */ (group) => {
    const children = group.children
      .map(/** 将当前条目转换为目标结果。 入参：path。 */ (path) => ROUTE_MODULE_BY_PATH[path])
      .filter(Boolean)
      .filter(/** 判断当前条目是否符合筛选条件。 入参：routeModule。 */ (routeModule) => canAccessMenu(routeModule, 'labAdmin', permissionItems))
      .map(buildMenuItem)

    return {
      index: group.index,
      title: group.title,
      icon: ROUTE_MODULE_BY_PATH[group.iconPath]?.icon,
      children,
    }
  }).filter(/** 判断当前条目是否符合筛选条件。 入参：group。 */ (group) => group.children.length)

  return [...homeMenu, ...groupedMenus]
}

/**
 * 获取角色Menus。
 * 入参：roleScope（角色Scope）；user（用户）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getRoleMenus = (roleScope = DEFAULT_AUTH_ROLE, user) => {
  const permissionItems = parseUserPermissions(user)

  if (roleScope === 'labAdmin') {
    return buildLabAdminMenus(permissionItems)
  }

  return getRouteModulesByRole(roleScope)
    .filter(/** 判断当前条目是否符合筛选条件。 入参：routeModule。 */ (routeModule) => canAccessMenu(routeModule, roleScope, permissionItems))
    .map(buildMenuItem)
}
