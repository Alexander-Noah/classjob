/*
 * endpoints公共逻辑模块，供相关视图或服务复用。
 * 主要导出：API_ENDPOINTS、getBusinessModuleEndpoints。
 */
export const API_ENDPOINTS = {
  auth: {
    login: '/auth/login',
    logout: '/auth/logout',
    profile: '/auth/profile',
  },
  users: {
    list: '/admin/users',
    detail: '/admin/users',
    options: '/users/options',
    avatar: '/files/avatars',
  },
  systemSettings: {
    detail: '/system-settings',
    reset: '/system-settings/reset',
  },
  activationCodes: {
    list: '/admin/activation-codes',
    labBindings: '/activation-codes/lab-bindings',
    create: '/admin/activation-codes',
    generate: '/admin/activation-codes/generate',
    /**
     * 状态的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    status: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/status`,
    /**
     * remark的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    remark: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/remark`,
    /**
     * bindings的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    bindings: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/bindings`,
    /**
     * 远程Bind的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    remoteBind: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/bindings/remote-bind`,
    /**
     * 远程Unbind的处理逻辑。
     * 入参：id（标识）；bindingId（binding标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    remoteUnbind: (id, bindingId) =>
      `/admin/activation-codes/${encodeURIComponent(id)}/bindings/${encodeURIComponent(bindingId)}/remote-unbind`,
    batch: '/admin/activation-codes/batch',
    auditLogs: '/admin/activation-codes/audit-logs',
    bindLab: '/activation-codes/bind-lab',
    /**
     * unbind实验室的处理逻辑。
     * 入参：code（编码）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    unbindLab: (code) => `/activation-codes/${encodeURIComponent(code)}/unbind-lab`,
    /**
     * 禁用当前操作。
     * 入参：code（编码）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    disable: (code) => `/admin/activation-codes/${encodeURIComponent(code)}/disable`,
    verify: '/admin/activation-codes/verify',
  },
  scheduleAdjustments: {
    list: '/schedule-adjustments',
    detail: '/schedule-adjustments',
  },
  dashboard: {
    overview: '/dashboard/overview',
  },
  aiAssistant: {
    conversations: '/ai-assistant/conversations',
    userConfig: '/ai-assistant/user-config',
    testUserConfig: '/ai-assistant/user-config/test',
    chat: '/ai-assistant/chat',
  },
  labs: {
    list: '/labs',
    batch: '/labs/batch',
    reset: '/labs/reset',
    /**
     * 设备的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    devices: (id) => `/labs/${id}/devices`,
  },
  devices: {
    list: '/devices',
    page: '/devices/page',
    stats: '/devices/stats',
    import: '/devices/import',
    export: '/devices/export',
    /**
     * 详情的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    detail: (id) => `/devices/${id}`,
    batch: '/devices/batch',
    reset: '/devices/reset',
    /**
     * 库存的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    inventory: (id) => `/devices/${id}/inventory`,
    /**
     * 库存记录集合的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    inventoryRecords: (id) => `/devices/${id}/inventory-records`,
    /**
     * 调拨当前操作。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    transfer: (id) => `/devices/${id}/transfer`,
    /**
     * 调拨记录集合。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    transferRecords: (id) => `/devices/${id}/transfer-records`,
  },
  deviceStatus: {
    /**
     * 实验室的处理逻辑。
     * 入参：labId（实验室标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    lab: (labId) => `/device-status/labs/${encodeURIComponent(labId)}`,
    /**
     * 设备的处理逻辑。
     * 入参：deviceId（设备标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    device: (deviceId) => `/device-status/devices/${encodeURIComponent(deviceId)}`,
    events: '/device-status/events',
    report: '/device-status/report',
    summary: '/device-status/summary',
  },
  iot: {
    devices: '/iot/devices',
    /**
     * 实验室设备的处理逻辑。
     * 入参：labId（实验室标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    labDevices: (labId) => `/iot/labs/${encodeURIComponent(labId)}/devices`,
    /**
     * 实验室概况的处理逻辑。
     * 入参：labId（实验室标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    labOverview: (labId) => `/iot/labs/${encodeURIComponent(labId)}/overview`,
    control: '/iot/control',
    commandLogs: '/iot/command-logs',
    /**
     * 环境的处理逻辑。
     * 入参：labId（实验室标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    environment: (labId) => `/iot/environment/${encodeURIComponent(labId)}`,
  },
  deviceInventoryRecords: {
    list: '/device-inventory-records',
  },
  deviceTransferRecords: {
    list: '/device-transfer-records',
  },
  reservations: {
    list: '/reservations',
    batch: '/reservations/batch',
    reset: '/reservations/reset',
  },
  scanReservations: {
    /**
     * 实验室Profile的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    labProfile: (id) => `/public/labs/${id}/reservation-profile`,
    submit: '/public/reservations/scan',
    /**
     * 状态的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    status: (id) => `/public/reservations/${id}/status`,
  },
  repairs: {
    list: '/repairs',
    batch: '/repairs/batch',
    reset: '/repairs/reset',
  },
  maintenanceRecords: {
    list: '/maintenance-records',
    /**
     * 详情的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    detail: (id) => `/maintenance-records/${encodeURIComponent(id)}`,
  },
  usageRecords: {
    list: '/usage-records',
    batch: '/usage-records/batch',
    reset: '/usage-records/reset',
  },
  consumables: {
    list: '/consumables',
    batch: '/consumables/batch',
    reset: '/consumables/reset',
    records: '/consumables/stock-records',
    /**
     * movement的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    movement: (id) => `/consumables/${id}/movement`,
  },
  notices: {
    list: '/notices',
    batch: '/notices/batch',
    reset: '/notices/reset',
    stats: '/notices/stats',
    user: '/notices/user',
    unread: '/notices/user/unread',
    syncBusinessReminders: '/notices/business-reminders/sync',
    /**
     * publish的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    publish: (id) => `/notices/${id}/publish`,
    /**
     * withdraw的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    withdraw: (id) => `/notices/${id}/withdraw`,
    /**
     * 归档的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    archive: (id) => `/notices/${id}/archive`,
    /**
     * 读取当前操作。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    read: (id) => `/notices/${id}/read`,
    /**
     * recipients的处理逻辑。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    recipients: (id) => `/notices/${id}/recipients`,
  },
  courseEnvironmentRequests: {
    list: '/course-environment-requests',
    detail: '/course-environment-requests',
  },
  academicSchedules: {
    parse: '/academic-schedules/parse',
    fetch: '/academic-schedules/fetch',
    import: '/academic-schedules/import',
    credentials: '/academic-schedules/credentials',
  },
  classTimetables: {
    list: '/class-timetables',
    summary: '/class-timetables/summary',
    semesters: '/class-timetables/semesters',
    crawl: '/class-timetables/crawl',
    crawlerConfig: '/class-timetables/crawler-config',
    crawlerTasks: '/class-timetables/crawler-tasks',
    credential: '/class-timetables/credential',
  },
  environmentTemplates: {
    list: '/environment-templates',
    batch: '/environment-templates/batch',
    reset: '/environment-templates/reset',
  },
}

/**
 * 获取BusinessModule端点。
 * 入参：moduleName（module名称）。
 * @returns 组装的键值对象。
 */
export function getBusinessModuleEndpoints(moduleName = 'module-records') {
  const normalizedModuleName = String(moduleName || 'module-records')
    .replace(/[^a-zA-Z0-9-]/g, '-')
    .toLowerCase()

  return {
    list: `/modules/${normalizedModuleName}`,
    batch: `/modules/${normalizedModuleName}/batch`,
    reset: `/modules/${normalizedModuleName}/reset`,
  }
}
