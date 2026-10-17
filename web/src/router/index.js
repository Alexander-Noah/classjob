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
import {
  APP_DOCUMENT_TITLE,
  DEFAULT_ROUTE_PATH,
  ROLE_DEFAULT_PATHS,
  ROUTE_MODULES,
  ROUTE_MODULE_MAP,
  getRoleHomeModule,
} from '../constants/routeModules'

const AdminLayout = () => import('../views/layout/AdminLayout.vue')
const Login = () => import('../views/login/Login.vue')
const Forbidden = () => import('../views/placeholder/Forbidden.vue')
const ScanReservation = () => import('../views/reservations/ScanReservation.vue')

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
  LabAdminPortal: () => import('../views/roles/LabAdminPortal.vue'),
  LabAdminLabManagement: () => import('../views/labs/LabManagement.vue'),
  LabAdminDeviceManagement: () => import('../views/devices/DeviceManagement.vue'),
  LabAdminEnvironmentManagement: () => import('../views/environments/EnvironmentManagement.vue'),
  LabAdminReservationManagement: () => import('../views/reservations/ReservationManagement.vue'),
  LabAdminMaintenanceRecordList: () => import('../views/maintenance-records/MaintenanceRecordList.vue'),
  LabAdminMaintenanceRecordEntry: () => import('../views/maintenance-records/MaintenanceRecordEntry.vue'),
  LabAdminConsumableInventory: () => import('../views/consumables/ConsumableInventory.vue'),
  LabAdminUsageRecords: () => import('../views/records/UsageRecords.vue'),
  LabAdminDataStatistics: () => import('../views/statistics/DataStatistics.vue'),
  LabAdminAiAssistant: () => import('../views/ai-assistant/AiAssistant.vue'),
  LabAdminNoticeCenter: () => import('../views/notices/NoticeCenter.vue'),
  LabAdminRoomControlCenter: () => import('../views/room-control/RoomControlCenter.vue'),
  LabAdminDeviceStatus: () => import('../views/device-status/DeviceStatusPage.vue'),
  LabAdminClassTimetable: () => import('../views/labs/LabAdminClassTimetable.vue'),
  LabAdminClassTimetableData: () => import('../views/labs/LabAdminClassTimetableData.vue'),

  SystemAdminPortal: () => import('../views/roles/SystemAdminPortal.vue'),
  SystemAdminUserPermissions: () => import('../views/users/UserPermissions.vue'),
  SystemAdminSettings: () => import('../views/system/SystemSettings.vue'),
  SystemAdminActivationCodes: () => import('../views/system/ActivationCodeManagement.vue'),
  SystemAdminNoticeCenter: () => import('../views/notices/NoticeCenter.vue'),
  SystemAdminStatistics: () => import('../views/statistics/DataStatistics.vue'),

  TeacherPortal: () => import('../views/roles/TeacherPortal.vue'),
  TeacherCourseEnvironments: () => import('../views/teacher/TeacherCourseEnvironments.vue'),
  TeacherSchedule: () => import('../views/teacher/TeacherSchedule.vue'),
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
      ...ROUTE_MODULES.map((routeModule) => ({
        path: routeModule.routePath,
        name: routeModule.name,
        component: routeComponentMap[routeModule.name],
        meta: buildRouteMeta(routeModule),
      })),
      {
        path: 'lab-admin/device-bindings',
        redirect: '/lab-admin/device-status',
      },
      {
        path: 'lab-admin/repairs',
        redirect: '/lab-admin/maintenance-records',
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
  scrollBehavior() {
    return { top: 0 }
  },
})

const getRedirectPath = () => {
  return AUTH_ROLE_DEFAULT_PATH[getStoredRole()] || AUTH_ROLE_DEFAULT_PATH[DEFAULT_AUTH_ROLE]
}

// 校验顺序：公开页面 → 登录状态/服务端资料 → 角色范围 → 自定义菜单权限。
// 路由守卫只控制页面访问体验，不能替代后端对接口和数据范围的授权。
router.beforeEach(async (to) => {
  const isPublicRoute = to.matched.some((matchedRoute) => matchedRoute.meta?.public)
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
    return true
  }

  if (hasAuth && to.meta?.roleScope && to.meta.roleScope !== storedRole) {
    return '/forbidden'
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
