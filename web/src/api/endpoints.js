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
    status: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/status`,
    remark: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/remark`,
    bindings: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/bindings`,
    remoteBind: (id) => `/admin/activation-codes/${encodeURIComponent(id)}/bindings/remote-bind`,
    remoteUnbind: (id, bindingId) =>
      `/admin/activation-codes/${encodeURIComponent(id)}/bindings/${encodeURIComponent(bindingId)}/remote-unbind`,
    batch: '/admin/activation-codes/batch',
    auditLogs: '/admin/activation-codes/audit-logs',
    bindLab: '/activation-codes/bind-lab',
    unbindLab: (code) => `/activation-codes/${encodeURIComponent(code)}/unbind-lab`,
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
    devices: (id) => `/labs/${id}/devices`,
  },
  devices: {
    list: '/devices',
    page: '/devices/page',
    stats: '/devices/stats',
    import: '/devices/import',
    export: '/devices/export',
    detail: (id) => `/devices/${id}`,
    batch: '/devices/batch',
    reset: '/devices/reset',
    inventory: (id) => `/devices/${id}/inventory`,
    inventoryRecords: (id) => `/devices/${id}/inventory-records`,
    transfer: (id) => `/devices/${id}/transfer`,
    transferRecords: (id) => `/devices/${id}/transfer-records`,
  },
  deviceStatus: {
    lab: (labId) => `/device-status/labs/${encodeURIComponent(labId)}`,
    device: (deviceId) => `/device-status/devices/${encodeURIComponent(deviceId)}`,
    events: '/device-status/events',
    report: '/device-status/report',
    summary: '/device-status/summary',
  },
  iot: {
    devices: '/iot/devices',
    labDevices: (labId) => `/iot/labs/${encodeURIComponent(labId)}/devices`,
    labOverview: (labId) => `/iot/labs/${encodeURIComponent(labId)}/overview`,
    control: '/iot/control',
    commandLogs: '/iot/command-logs',
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
    labProfile: (id) => `/public/labs/${id}/reservation-profile`,
    submit: '/public/reservations/scan',
    status: (id) => `/public/reservations/${id}/status`,
  },
  repairs: {
    list: '/repairs',
    batch: '/repairs/batch',
    reset: '/repairs/reset',
  },
  maintenanceRecords: {
    list: '/maintenance-records',
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
    publish: (id) => `/notices/${id}/publish`,
    withdraw: (id) => `/notices/${id}/withdraw`,
    archive: (id) => `/notices/${id}/archive`,
    read: (id) => `/notices/${id}/read`,
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
