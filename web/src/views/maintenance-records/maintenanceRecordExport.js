/*
 * 维护记录公共逻辑模块，供相关视图或服务复用。
 * 主要导出：sanitizeMaintenanceRecordExportCell、toMaintenanceRecordExportRow。
 */
const FORMULA_PREFIX = /^\s*[=+\-@]/

/**
 * 清洗维护记录ExportCell。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function sanitizeMaintenanceRecordExportCell(value) {
  if (value === undefined || value === null) return ''

  const text = String(value)
  return FORMULA_PREFIX.test(text) ? `'${text}` : text
}

/**
 * 转换为维护记录Export行。
 * 入参：record（记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function toMaintenanceRecordExportRow(record = {}) {
  return Object.fromEntries(
    ['recordNo', 'lab', 'device', 'maintenanceType', 'maintenanceDate', 'maintainer', 'content', 'result', 'remark'].map(/** 将当前条目转换为目标结果。 入参：field。 */ (field) => [
      field,
      sanitizeMaintenanceRecordExportCell(record[field]),
    ]),
  )
}
