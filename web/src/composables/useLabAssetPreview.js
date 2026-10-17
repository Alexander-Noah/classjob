/*
 * 实验室的 Vue 组合逻辑；视图通过返回的状态和方法调用本模块。
 * 主要导出：getAssetStatusType、getAssetHealthType、normalizeAssetRecord、loadLabAssetRecords、getLabAssetRecordsByLabName。
 */
const DEVICE_STORAGE_KEY = 'smart-lab:device-management:records'
const PREVIEW_STORAGE_KEY = 'smart-lab:lab-asset-preview:records'

const STATUS_TYPE_MAP = {
  正常: 'success',
  借用中: 'primary',
  维护中: 'warning',
  故障: 'danger',
  已盘点: 'success',
  停用: 'info',
}

const HEALTH_TYPE_MAP = {
  优: 'success',
  良: 'primary',
  关注: 'warning',
  风险: 'danger',
}

const INITIAL_ASSET_RECORDS = [
  {
    id: 'asset-a101-001',
    code: 'PC-A101-018',
    name: '图形工作站',
    category: '计算终端',
    lab: 'A101 云计算实验室',
    location: 'A101 第 2 排',
    owner: '张明',
    status: '正常',
    health: '优',
    online: true,
    specs: 'i7 / 32GB / RTX 4070',
    inventoryDate: '2026-04-20',
  },
  {
    id: 'asset-a101-002',
    code: 'PC-A101-032',
    name: '学生上机终端',
    category: '计算终端',
    lab: 'A101 云计算实验室',
    location: 'A101 第 4 排',
    owner: '张明',
    status: '正常',
    health: '良',
    online: true,
    specs: 'i5 / 16GB / 1TB SSD',
    inventoryDate: '2026-04-20',
  },
  {
    id: 'asset-a101-003',
    code: 'NET-A101-003',
    name: '汇聚交换机',
    category: '网络设备',
    lab: 'A101 云计算实验室',
    location: 'A101 弱电柜',
    owner: '张明',
    status: '维护中',
    health: '关注',
    online: true,
    specs: '24 口千兆 / PoE',
    inventoryDate: '2026-04-18',
  },
  {
    id: 'asset-a102-001',
    code: 'MON-A102-041',
    name: '教学显示器',
    category: '显示设备',
    lab: 'A102 软件工程实验室',
    location: 'A102 讲台',
    owner: '周敏',
    status: '正常',
    health: '良',
    online: true,
    specs: '75 寸 4K 触控屏',
    inventoryDate: '2026-04-18',
  },
  {
    id: 'asset-a102-002',
    code: 'PC-A102-027',
    name: '课程教学终端',
    category: '计算终端',
    lab: 'A102 软件工程实验室',
    location: 'A102 第 3 排',
    owner: '周敏',
    status: '借用中',
    health: '良',
    online: true,
    specs: 'i5 / 16GB / 双显示',
    inventoryDate: '2026-04-19',
  },
  {
    id: 'asset-a102-003',
    code: 'SAFE-A102-002',
    name: '门禁控制器',
    category: '安全设备',
    lab: 'A102 软件工程实验室',
    location: 'A102 入口',
    owner: '周敏',
    status: '正常',
    health: '优',
    online: true,
    specs: '二维码 / 人脸联动',
    inventoryDate: '2026-04-17',
  },
  {
    id: 'asset-b203-001',
    code: 'NET-B203-006',
    name: '核心交换机',
    category: '网络设备',
    lab: 'B203 网络安全实验室',
    location: 'B203 弱电柜',
    owner: '李娜',
    status: '维护中',
    health: '关注',
    online: true,
    specs: '48 口万兆交换机',
    inventoryDate: '2026-04-19',
  },
  {
    id: 'asset-b203-002',
    code: 'FW-B203-002',
    name: '安全防火墙',
    category: '安全设备',
    lab: 'B203 网络安全实验室',
    location: 'B203 机柜 1',
    owner: '李娜',
    status: '正常',
    health: '良',
    online: true,
    specs: '双机热备 / 日志审计',
    inventoryDate: '2026-04-19',
  },
  {
    id: 'asset-b203-003',
    code: 'PC-B203-021',
    name: '攻防演练终端',
    category: '计算终端',
    lab: 'B203 网络安全实验室',
    location: 'B203 第 2 排',
    owner: '李娜',
    status: '故障',
    health: '风险',
    online: false,
    specs: 'i7 / 32GB / 双网卡',
    inventoryDate: '2026-04-16',
  },
  {
    id: 'asset-c305-001',
    code: 'AI-C305-012',
    name: 'GPU 训练服务器',
    category: '服务器',
    lab: 'C305 人工智能实验室',
    location: 'C305 设备区',
    owner: '王磊',
    status: '借用中',
    health: '良',
    online: true,
    specs: '4 x RTX 4090 / 256GB',
    inventoryDate: '2026-04-21',
  },
  {
    id: 'asset-c305-002',
    code: 'SAFE-C305-003',
    name: '烟雾明火传感器',
    category: '安全设备',
    lab: 'C305 人工智能实验室',
    location: 'C305 顶部设备区',
    owner: '陈工',
    status: '故障',
    health: '风险',
    online: false,
    specs: '烟雾 / 明火 / 温湿度',
    inventoryDate: '2026-04-16',
  },
  {
    id: 'asset-c305-003',
    code: 'COOL-C305-004',
    name: '机柜制冷单元',
    category: '环境设备',
    lab: 'C305 人工智能实验室',
    location: 'C305 机柜区',
    owner: '王磊',
    status: '正常',
    health: '关注',
    online: true,
    specs: '温控联动 / 远程告警',
    inventoryDate: '2026-04-20',
  },
  {
    id: 'asset-d401-001',
    code: 'PC-D401-066',
    name: '基础教学终端',
    category: '计算终端',
    lab: 'D401 基础计算机实验室',
    location: 'D401 第 6 排',
    owner: '陈工',
    status: '正常',
    health: '良',
    online: true,
    specs: 'i5 / 8GB / 512GB SSD',
    inventoryDate: '2026-04-22',
  },
  {
    id: 'asset-d401-002',
    code: 'NET-D401-001',
    name: '接入交换机',
    category: '网络设备',
    lab: 'D401 基础计算机实验室',
    location: 'D401 弱电柜',
    owner: '陈工',
    status: '正常',
    health: '优',
    online: true,
    specs: '48 口千兆',
    inventoryDate: '2026-04-22',
  },
  {
    id: 'asset-d401-003',
    code: 'MON-D401-003',
    name: '投影控制终端',
    category: '显示设备',
    lab: 'D401 基础计算机实验室',
    location: 'D401 讲台',
    owner: '陈工',
    status: '停用',
    health: '关注',
    online: false,
    specs: '投影联动 / 中控面板',
    inventoryDate: '2026-04-11',
  },
]

function readStorageRecords(storageKey) {
  if (typeof window === 'undefined') return null

  try {
    const rawValue = window.localStorage.getItem(storageKey)
    const parsedValue = rawValue ? JSON.parse(rawValue) : null
    return Array.isArray(parsedValue) ? parsedValue : null
  } catch {
    return null
  }
}

function persistPreviewRecords(records) {
  if (typeof window === 'undefined') return

  window.localStorage.setItem(PREVIEW_STORAGE_KEY, JSON.stringify(records))
}

function mergeRecords(records) {
  const recordMap = new Map()

  records.forEach((record) => {
    const key = record.id || record.code

    if (!key) return

    recordMap.set(key, normalizeAssetRecord(record))
  })

  return Array.from(recordMap.values())
}

export function getAssetStatusType(status) {
  return STATUS_TYPE_MAP[status] || 'info'
}

export function getAssetHealthType(health) {
  return HEALTH_TYPE_MAP[health] || 'info'
}

export function normalizeAssetRecord(record) {
  return {
    ...record,
    status: record.status || '正常',
    health: record.health || '良',
    tagType: getAssetStatusType(record.status),
    healthTagType: getAssetHealthType(record.health),
    online: Boolean(record.online),
  }
}

export function loadLabAssetRecords() {
  const seededRecords = INITIAL_ASSET_RECORDS.map(normalizeAssetRecord)
  const previewRecords = readStorageRecords(PREVIEW_STORAGE_KEY)
  const deviceRecords = readStorageRecords(DEVICE_STORAGE_KEY)

  if (!previewRecords?.length) {
    persistPreviewRecords(seededRecords)
  }

  return mergeRecords([
    ...seededRecords,
    ...(previewRecords || []),
    ...(deviceRecords || []),
  ])
}

export function getLabAssetRecordsByLabName(labName) {
  if (!labName) return []

  return loadLabAssetRecords().filter((record) => record.lab === labName)
}
