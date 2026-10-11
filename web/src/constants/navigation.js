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

export const getRoleLayout = (roleScope = DEFAULT_AUTH_ROLE) =>
  ROLE_LAYOUTS[roleScope] || ROLE_LAYOUTS[DEFAULT_AUTH_ROLE]

const ROUTE_MODULE_BY_PATH = Object.fromEntries(
  ROUTE_MODULES.map((routeModule) => [routeModule.path, routeModule]),
)

const LAB_ADMIN_MENU_GROUPS = [
  {
    index: 'lab-admin-basic-resources',
    title: '基础资源管理',
    iconPath: '/lab-admin/labs',
    children: ['/lab-admin/labs'],
  },
  {
    index: 'lab-admin-device-management',
    title: '设备管理',
    iconPath: '/lab-admin/devices',
    children: ['/lab-admin/devices'],
  },
  {
    index: 'lab-admin-teaching-reservations',
    title: '教学预约协同',
    iconPath: '/lab-admin/class-timetable',
    children: ['/lab-admin/class-timetable', '/lab-admin/class-timetable-data', '/lab-admin/reservations'],
  },
  {
    index: 'lab-admin-maintenance-management',
    title: '设备维护管理',
    iconPath: '/lab-admin/maintenance-records',
    children: ['/lab-admin/maintenance-records', '/lab-admin/maintenance-records/new', '/lab-admin/notices'],
  },
  {
    index: 'lab-admin-data-intelligence',
    title: '数据统计与智能辅助',
    iconPath: '/lab-admin/statistics',
    children: ['/lab-admin/records', '/lab-admin/statistics'],
  },
]

const canAccessMenu = (routeModule, roleScope, permissionItems) =>
  !permissionItems.length ||
  routeModule.path === ROLE_DEFAULT_PATHS[roleScope] ||
  isPathAllowed(routeModule.path, permissionItems)

const buildMenuItem = (routeModule) => ({
  index: routeModule.path,
  title: routeModule.menuTitle,
  icon: routeModule.icon,
})

const buildLabAdminMenus = (permissionItems) => {
  const homeModule = ROUTE_MODULE_BY_PATH[ROLE_DEFAULT_PATHS.labAdmin]
  const homeMenu = homeModule && canAccessMenu(homeModule, 'labAdmin', permissionItems) ? [buildMenuItem(homeModule)] : []

  const groupedMenus = LAB_ADMIN_MENU_GROUPS.map((group) => {
    const children = group.children
      .map((path) => ROUTE_MODULE_BY_PATH[path])
      .filter(Boolean)
      .filter((routeModule) => canAccessMenu(routeModule, 'labAdmin', permissionItems))
      .map(buildMenuItem)

    return {
      index: group.index,
      title: group.title,
      icon: ROUTE_MODULE_BY_PATH[group.iconPath]?.icon,
      children,
    }
  }).filter((group) => group.children.length)

  return [...homeMenu, ...groupedMenus]
}

export const getRoleMenus = (roleScope = DEFAULT_AUTH_ROLE, user) => {
  const permissionItems = parseUserPermissions(user)

  if (roleScope === 'labAdmin') {
    return buildLabAdminMenus(permissionItems)
  }

  return getRouteModulesByRole(roleScope)
    .filter((routeModule) => canAccessMenu(routeModule, roleScope, permissionItems))
    .map(buildMenuItem)
}
