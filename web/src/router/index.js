/*
 * router index公共逻辑模块，供相关视图或服务复用。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  assertActiveSessionRole,
  clearAuthSession,
  getStoredRole,
  getStoredUser,
  isAuthenticated,
  refreshCurrentUserSession,
} from '../api/services/auth'
import {
  AUTH_ROLE_DEFAULT_PATH,
  DEFAULT_AUTH_ROLE,
} from '../constants/auth'
import { isPathAllowed, parseUserPermissions } from '../constants/permissions'
import { refreshPageAccess } from '../api/services/rolePageAccess'
import { canAccessRolePage, isRecoveryPage } from '../constants/rolePageAccess'
import {
  APP_DOCUMENT_TITLE,
  DEFAULT_ROUTE_PATH,
  ROLE_DEFAULT_PATHS,
  ACTIVE_ROUTE_MODULES,
  ROUTE_MODULE_MAP,
  getRoleHomeModule,
} from '../constants/routeModules'

/**
 * AdminLayout的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const AdminLayout = () => import('../views/layout/AdminLayout.vue')
/**
 * 登录的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const Login = () => import('../views/login/Login.vue')
/**
 * Forbidden的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const Forbidden = () => import('../views/placeholder/Forbidden.vue')
const PermissionsUnavailable = () => import('../views/placeholder/PermissionsUnavailable.vue')
/**
 * Scan预约的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const ScanReservation = () => import('../views/reservations/ScanReservation.vue')

/**
 * 构建Breadcrumb。
 * 入参：routeModule（routeModule）。
 * @returns 生成的数组。
 */
const buildBreadcrumb = (routeModule) => {
  const roleHomeModule = getRoleHomeModule(routeModule.roleScope)

  if (!roleHomeModule || routeModule.path === roleHomeModule.path) {
    return [{ title: routeModule.menuTitle, path: routeModule.path }]
  }

  return [
    { title: roleHomeModule.menuTitle, path: roleHomeModule.path },
    { title: routeModule.title, path: routeModule.path },
  ]
}

/**
 * 构建RouteMeta。
 * 入参：routeModule（routeModule）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const buildRouteMeta = (routeModule) => ({
  title: routeModule.title,
  menuTitle: routeModule.menuTitle,
  description: routeModule.description,
  activeMenu: routeModule.path,
  breadcrumb: buildBreadcrumb(routeModule),
  roleScope: routeModule.roleScope,
  module: routeModule,
})

// 路由元信息来自统一模块表，组件使用懒加载，避免首页下载所有业务页面。
const routeComponentMap = {
  SystemAdminRolePageAccess: () => import('../views/users/RolePageAccess.vue'),
  /**
   * 实验室AdminPortal的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminPortal: () => import('../views/roles/LabAdminPortal.vue'),
  /**
   * 实验室Admin实验室Management的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminLabManagement: () => import('../views/labs/LabManagement.vue'),
  /**
   * 实验室Admin设备Management的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminDeviceManagement: () => import('../views/devices/DeviceManagement.vue'),
  /**
   * 实验室Admin环境Management的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminEnvironmentManagement: () => import('../views/environments/EnvironmentManagement.vue'),
  /**
   * 实验室Admin预约Management的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminReservationManagement: () => import('../views/reservations/ReservationManagement.vue'),
  /**
   * 实验室Admin维护记录列表的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminMaintenanceRecordList: () => import('../views/maintenance-records/MaintenanceRecordList.vue'),
  /**
   * 实验室Admin维护记录条目的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminMaintenanceRecordEntry: () => import('../views/maintenance-records/MaintenanceRecordEntry.vue'),
  /**
   * 实验室AdminConsumable库存的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminConsumableInventory: () => import('../views/consumables/ConsumableInventory.vue'),
  /**
   * 实验室AdminUsage记录集合的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminUsageRecords: () => import('../views/records/UsageRecords.vue'),
  /**
   * 实验室Admin数据Statistics的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminDataStatistics: () => import('../views/statistics/DataStatistics.vue'),
  /**
   * 实验室AdminAIAssistant的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminAiAssistant: () => import('../views/ai-assistant/AiAssistant.vue'),
  /**
   * 实验室Admin通知Center的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminNoticeCenter: () => import('../views/notices/NoticeCenter.vue'),
  /**
   * 实验室AdminRoomControlCenter的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminRoomControlCenter: () => import('../views/room-control/RoomControlCenter.vue'),
  /**
   * 实验室Admin设备状态的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminDeviceStatus: () => import('../views/device-status/DeviceStatusPage.vue'),
  /**
   * 实验室AdminClass课表的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminClassTimetable: () => import('../views/labs/LabAdminClassTimetable.vue'),
  /**
   * 实验室AdminClass课表数据的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  LabAdminClassTimetableData: () => import('../views/labs/LabAdminClassTimetableData.vue'),

  /**
   * SystemAdminPortal的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  SystemAdminPortal: () => import('../views/roles/SystemAdminPortal.vue'),
  /**
   * SystemAdmin用户权限的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  SystemAdminUserPermissions: () => import('../views/users/UserPermissions.vue'),
  /**
   * SystemAdmin设置的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  SystemAdminSettings: () => import('../views/system/SystemSettings.vue'),
  /**
   * SystemAdminActivation编码集合的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  SystemAdminActivationCodes: () => import('../views/system/ActivationCodeManagement.vue'),
  /**
   * SystemAdmin通知Center的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  SystemAdminNoticeCenter: () => import('../views/notices/NoticeCenter.vue'),
  /**
   * SystemAdminStatistics的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  SystemAdminStatistics: () => import('../views/statistics/DataStatistics.vue'),

  /**
   * 教师Portal的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  TeacherPortal: () => import('../views/roles/TeacherPortal.vue'),
  /**
   * 教师课程环境的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  TeacherCourseEnvironments: () => import('../views/teacher/TeacherCourseEnvironments.vue'),
  /**
   * 教师课表的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  TeacherSchedule: () => import('../views/teacher/TeacherSchedule.vue'),
  /**
   * 教师Class课表数据的处理逻辑。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  TeacherClassTimetableData: () => import('../views/labs/LabAdminClassTimetableData.vue'),
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      title: '登录',
      public: true,
    },
  },
  {
    path: '/scan/reserve',
    name: 'ScanReservation',
    component: ScanReservation,
    meta: {
      title: '扫码预约',
      public: true,
    },
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: DEFAULT_ROUTE_PATH,
    meta: {
      requiresAuth: true,
    },
    children: [
      ...ACTIVE_ROUTE_MODULES.map(/** 将当前条目转换为目标结果。 入参：routeModule。 */ (routeModule) => ({
        path: routeModule.routePath,
        name: routeModule.name,
        component: routeComponentMap[routeModule.name],
        meta: buildRouteMeta(routeModule),
      })),
      { path: 'teacher/:pathMatch(.*)*', redirect: '/forbidden' },
      {
        path: 'lab-admin/device-bindings',
        redirect: '/lab-admin/device-status',
      },
      {
        path: 'lab-admin/repairs',
        redirect: '/lab-admin/maintenance-records',
      },
      {
          path: 'permissions-unavailable',
          name: 'PermissionsUnavailable',
          component: PermissionsUnavailable,
          meta: { title: '权限服务暂不可用' },
        },
        {
          path: 'forbidden',
        name: 'Forbidden',
        component: Forbidden,
        meta: {
          title: '无权访问',
        },
      },
    ],
  },
  {
    path: '/dashboard',
    redirect: ROLE_DEFAULT_PATHS.labAdmin,
  },
  {
    path: '/labs',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/labs`,
  },
  {
    path: '/devices',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/devices`,
  },
  {
    path: '/environments',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/environments`,
  },
  {
    path: '/reservations',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/reservations`,
  },
  {
    path: '/repairs',
    redirect: '/lab-admin/maintenance-records',
  },
  {
    path: '/records',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/records`,
  },
  {
    path: '/statistics',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/statistics`,
  },
  {
    path: '/ai-assistant',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/ai-assistant`,
  },
  {
    path: '/notices',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/notices`,
  },
  {
    path: '/room-control',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/room-control`,
  },
  {
    path: '/device-bindings',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/device-status`,
  },
  {
    path: '/device-status',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/device-status`,
  },
  {
    path: '/users',
    redirect: `${ROLE_DEFAULT_PATHS.systemAdmin}/users`,
  },
  {
    path: '/system',
    redirect: `${ROLE_DEFAULT_PATHS.systemAdmin}/system`,
  },
  {
    path: '/course-environments',
    redirect: `${ROLE_DEFAULT_PATHS.teacher}/course-environments`,
  },
  {
    path: '/schedule',
    redirect: `${ROLE_DEFAULT_PATHS.teacher}/schedule`,
  },
  {
    path: '/adjustments',
    redirect: `${ROLE_DEFAULT_PATHS.teacher}/schedule`,
  },
  {
    path: '/work-orders',
    redirect: '/forbidden',
  },
  {
    path: '/maintenance/:pathMatch(.*)*',
    redirect: '/forbidden',
  },
  {
    path: '/student',
    redirect: ROLE_DEFAULT_PATHS.teacher,
  },
  {
    path: '/leader',
    redirect: ROLE_DEFAULT_PATHS.systemAdmin,
  },
  {
    path: '/home',
    redirect: DEFAULT_ROUTE_PATH,
  },
  {
    path: '/software',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/environments?tab=software`,
  },
  {
    path: '/environment-templates',
    redirect: `${ROLE_DEFAULT_PATHS.labAdmin}/environments?tab=template`,
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: DEFAULT_ROUTE_PATH,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  /**
   * 滚动Behavior的处理逻辑。
   * @returns 组装的键值对象。
   */
  scrollBehavior() {
    return { top: 0 }
  },
})

/**
 * 获取Redirect路径。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getRedirectPath = () => {
  return AUTH_ROLE_DEFAULT_PATH[getStoredRole()] || AUTH_ROLE_DEFAULT_PATH[DEFAULT_AUTH_ROLE]
}

// 校验顺序：公开页面 → 登录状态/服务端资料 → 角色范围 → 自定义菜单权限。
// 路由守卫只控制页面访问体验，不能替代后端对接口和数据范围的授权。
router.beforeEach(/** 处理 beforeEach 调用中的回调逻辑。 入参：to。 */ async (to) => {
  const isPublicRoute = to.matched.some(/** 判断当前条目是否满足任一匹配条件。 入参：matchedRoute。 */ (matchedRoute) => matchedRoute.meta?.public)
  const requiresAuth = !isPublicRoute
  const hasAuth = isAuthenticated()
  const isForbiddenRoute = to.name === 'Forbidden'

  document.title = `${to.meta?.title || '系统'} - ${APP_DOCUMENT_TITLE}`

  if (requiresAuth && !hasAuth) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (hasAuth) {
    try {
      assertActiveSessionRole(getStoredRole(), getStoredUser())
      if (!isPublicRoute) {
        await refreshCurrentUserSession({ force: true })
      }
    } catch (error) {
      clearAuthSession()
      const message = error?.userMessage || error?.message || '登录状态无效'
      if (!error?.__handled) ElMessage.error(message)
      return { path: '/login', query: { error: message } }
    }
  }

  if (isPublicRoute && hasAuth && to.path === '/login') {
    return getRedirectPath()
  }

  const storedRole = getStoredRole()

  if (isForbiddenRoute) {
    // 403也需要有效导航，不能因上一次权限请求失败变成无法离开的空壳。
    try { await refreshPageAccess(storedRole) } catch { /* 保留必要恢复入口。 */ }
    return true
  }
  if (to.name === 'PermissionsUnavailable') {
    return true
  }

  if (hasAuth && to.meta?.roleScope && to.meta.roleScope !== storedRole) {
    return '/forbidden'
  }

  if (hasAuth && !isPublicRoute && to.meta?.roleScope) {
    try {
      const policy = await refreshPageAccess(storedRole)
      if (!canAccessRolePage(policy, to.path)) return '/forbidden'
    } catch {
      // 必要入口由服务端固定保留；服务异常时不能把管理员锁在403。
      if (isRecoveryPage(storedRole, to.path)) return true
      return { path: '/permissions-unavailable', query: { redirect: to.fullPath } }
    }
  }

  if (!isPublicRoute && to.name && !ROUTE_MODULE_MAP[to.name] && to.path !== '/login' && !isForbiddenRoute) {
    return AUTH_ROLE_DEFAULT_PATH[storedRole] || DEFAULT_ROUTE_PATH
  }

  // 权限拦截：工作台（portal）页面始终放行，子页面需要权限校验
  if (hasAuth && to.meta?.roleScope) {
    const roleHomePath = AUTH_ROLE_DEFAULT_PATH[storedRole]
    const isPortalPage = to.path === roleHomePath
    if (!isPortalPage) {
      const user = getStoredUser()
      const userPermissions = parseUserPermissions(user)
      if (userPermissions.length && !isPathAllowed(to.path, userPermissions)) {
        return '/forbidden'
      }
    }
  }

  return true
})

export default router
