/*
 * 维护记录公共逻辑模块，供相关视图或服务复用。
 * 主要导出：canWriteMaintenanceRecords、createMaintenanceDraft、devicesForMaintenanceLab、buildMaintenancePayload、filterMaintenanceRecords。
 */
/** 前端写入入口展示规则；损坏的权限 JSON 返回 false，最终接口授权仍由后端执行。 */
export function canWriteMaintenanceRecords(user = {}) {
  if (user.roleCode === 'systemAdmin') return true
  if (user.roleCode !== 'labAdmin') return false
  if (!user.permissions) return true
  try {
    const groups = typeof user.permissions === 'string' ? JSON.parse(user.permissions) : user.permissions
    if (!Array.isArray(groups)) return false
    if (!groups.length) return true
    const items = groups.flatMap(/** 转换并展开当前条目。 入参：group。 */ group => typeof group === 'string' ? [group] : group?.items || [])
    return items.some(/** 判断当前条目是否满足任一匹配条件。 入参：item。 */ item => ['维护记录填写', '维护记录管理', '故障报修'].includes(item))
  } catch { return false }
}

/**
 * 文本的处理逻辑。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const text = value => ['未填写', '未关联设备'].includes(value) ? '' : String(value ?? '')
/** 创建独立表单草稿，清除占位文字；默认日期采用浏览器本地年月日，不使用 UTC 截断。 */
export function createMaintenanceDraft(record = {}, user = {}) {
  const now = new Date()
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  return {
    id: record.id || '', recordNo: text(record.recordNo),
    labId: String(record.labId || ''), deviceId: String(record.deviceId || ''),
    lab: text(record.lab), device: text(record.device),
    maintenanceType: text(record.maintenanceType),
    maintenanceDate: text(record.maintenanceDate).slice(0, 10) || today,
    maintainer: text(record.maintainer) || user.realName || user.name || user.username || '',
    content: text(record.content), result: text(record.result), remark: text(record.remark),
  }
}

/**
 * 设备For维护实验室的处理逻辑。
 * 入参：devices（设备）；labId（实验室标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function devicesForMaintenanceLab(devices, labId) {
  if (!labId) return []
  return devices.filter(/** 判断当前条目是否符合筛选条件。 入参：device。 */ device => String(device.labId) === String(labId))
}

/**
 * 将表单字符串转换为关联 ID，并校验可选设备确实属于当前实验室。
 * @param {object} draft 编辑草稿，未选择设备时允许空 deviceId
 * @param {Array<object>} labs 当前可选择的实验室元数据
 * @param {Array<object>} devices 当前设备资产元数据
 * @returns {object} 规范化提交对象；名称由元数据解析，文本字段去除前后空白
 * @throws {Error} 实验室 ID 非安全正整数、元数据不存在或设备归属错配
 */
export function buildMaintenancePayload(draft, labs, devices) {
  const labId = Number(draft.labId)
  const hasDevice = String(draft.deviceId ?? '').trim() !== ''
  const deviceId = hasDevice ? Number(draft.deviceId) : null
  const lab = labs.find(/** 判断当前条目是否为查找目标。 入参：row。 */ row => String(row.id) === String(draft.labId))
  const device = devicesForMaintenanceLab(devices, draft.labId).find(/** 判断当前条目是否为查找目标。 入参：row。 */ row => String(row.id) === String(draft.deviceId))
  if (!Number.isSafeInteger(labId) || labId <= 0 || !lab) throw new Error('请选择有效的实验室')
  if (hasDevice && (!Number.isSafeInteger(deviceId) || deviceId <= 0 || !device)) throw new Error('请选择属于该实验室的设备')
  return {
    labId, deviceId, lab: lab.name || lab.labName, device: device?.deviceName || '',
    ...Object.fromEntries(['maintenanceType', 'maintenanceDate', 'maintainer', 'content', 'result', 'remark'].map(/** 将当前条目转换为目标结果。 入参：field。 */ field => [field, String(draft[field] || '').trim()])),
  }
}

/**
 * 筛选维护记录集合。
 * 入参：records（记录集合）；filters（筛选条件）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function filterMaintenanceRecords(records, filters) {
  const keyword = String(filters.keyword || '').trim().toLowerCase()
  const [fromDate, toDate] = filters.dateRange || []
  return records.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ record => {
    const keywordMatched = !keyword || ['recordNo', 'lab', 'device', 'maintenanceType', 'maintainer', 'content', 'result', 'remark']
      .some(/** 判断当前条目是否满足任一匹配条件。 入参：field。 */ field => String(record[field] || '').toLowerCase().includes(keyword))
    const exactMatched = ['lab', 'device', 'maintenanceType', 'maintainer'].every(/** 判断当前条目是否满足全部匹配条件。 入参：field。 */ field => !filters[field] || record[field] === filters[field])
    const date = String(record.maintenanceDate || '').slice(0, 10)
    // ISO 日期字符串可直接按字典序比较；范围包含两端，前提是过滤器提供 YYYY-MM-DD。
    return keywordMatched && exactMatched && (!fromDate || date >= fromDate) && (!toDate || date <= toDate)
  })
}
