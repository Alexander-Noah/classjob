export function canWriteMaintenanceRecords(user = {}) {
  if (user.roleCode === 'systemAdmin') return true
  if (user.roleCode !== 'labAdmin') return false
  if (!user.permissions) return true
  try {
    const groups = typeof user.permissions === 'string' ? JSON.parse(user.permissions) : user.permissions
    if (!Array.isArray(groups)) return false
    if (!groups.length) return true
    const items = groups.flatMap(group => typeof group === 'string' ? [group] : group?.items || [])
    return items.some(item => ['维护记录填写', '维护记录管理', '故障报修'].includes(item))
  } catch { return false }
}

const text = value => ['未填写', '未关联设备'].includes(value) ? '' : String(value ?? '')
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

export function devicesForMaintenanceLab(devices, labId) {
  if (!labId) return []
  return devices.filter(device => String(device.labId) === String(labId))
}

export function buildMaintenancePayload(draft, labs, devices) {
  const labId = Number(draft.labId)
  const hasDevice = String(draft.deviceId ?? '').trim() !== ''
  const deviceId = hasDevice ? Number(draft.deviceId) : null
  const lab = labs.find(row => String(row.id) === String(draft.labId))
  const device = devicesForMaintenanceLab(devices, draft.labId).find(row => String(row.id) === String(draft.deviceId))
  if (!Number.isSafeInteger(labId) || labId <= 0 || !lab) throw new Error('请选择有效的实验室')
  if (hasDevice && (!Number.isSafeInteger(deviceId) || deviceId <= 0 || !device)) throw new Error('请选择属于该实验室的设备')
  return {
    labId, deviceId, lab: lab.name || lab.labName, device: device?.deviceName || '',
    ...Object.fromEntries(['maintenanceType', 'maintenanceDate', 'maintainer', 'content', 'result', 'remark'].map(field => [field, String(draft[field] || '').trim()])),
  }
}

export function filterMaintenanceRecords(records, filters) {
  const keyword = String(filters.keyword || '').trim().toLowerCase()
  const [fromDate, toDate] = filters.dateRange || []
  return records.filter(record => {
    const keywordMatched = !keyword || ['recordNo', 'lab', 'device', 'maintenanceType', 'maintainer', 'content', 'result', 'remark']
      .some(field => String(record[field] || '').toLowerCase().includes(keyword))
    const exactMatched = ['lab', 'device', 'maintenanceType', 'maintainer'].every(field => !filters[field] || record[field] === filters[field])
    const date = String(record.maintenanceDate || '').slice(0, 10)
    return keywordMatched && exactMatched && (!fromDate || date >= fromDate) && (!toDate || date <= toDate)
  })
}
