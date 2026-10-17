/*
 * 维护记录公共逻辑模块，供相关视图或服务复用。
 * 主要导出：sanitizeMaintenanceRecordExportCell、toMaintenanceRecordExportRow。
 */
const FORMULA_PREFIX = /^\s*[=+\-@]/

export function sanitizeMaintenanceRecordExportCell(value) {
  if (value === undefined || value === null) return ''

  const text = String(value)
  return FORMULA_PREFIX.test(text) ? `'${text}` : text
}

export function toMaintenanceRecordExportRow(record = {}) {
  return Object.fromEntries(
    ['recordNo', 'lab', 'device', 'maintenanceType', 'maintenanceDate', 'maintainer', 'content', 'result', 'remark'].map((field) => [
      field,
      sanitizeMaintenanceRecordExportCell(record[field]),
    ]),
  )
}
