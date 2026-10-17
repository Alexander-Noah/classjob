/*
 * 表格导出公共逻辑模块，供相关视图或服务复用。
 * 主要导出：exportTableToExcel。
 */
/**
 * 归一化Cell值。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function normalizeCellValue(value) {
  if (value === undefined || value === null) return ''
  if (Array.isArray(value)) return value.map(normalizeCellValue).filter(Boolean).join('；')
  if (value instanceof Date) return value.toLocaleString('zh-CN', { hour12: false })
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

/**
 * 清洗文件名称。
 * 入参：name（名称）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function sanitizeFileName(name = '数据导出') {
  return String(name || '数据导出')
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * 转义CSVCell。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function escapeCsvCell(value) {
  const text = normalizeCellValue(value)
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

/**
 * 获取Cell值。
 * 入参：row（行）；column（column）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function getCellValue(row, column) {
  if (typeof column.formatter === 'function') {
    return column.formatter(row)
  }
  return row?.[column.prop]
}

/**
 * 下载Blob。
 * 涉及：分配对象 URL，须配套释放。
 * 入参：blob（blob）；filename（filename）。
 */
function downloadBlob(blob, filename) {
  const link = document.createElement('a')
  const objectUrl = URL.createObjectURL(blob)

  link.href = objectUrl
  link.download = filename
  link.click()
  URL.revokeObjectURL(objectUrl)
}

/**
 * 导出Table目标Excel。
 * @returns exportRows.length 的当前值。
 */
export function exportTableToExcel({ filename, columns, rows }) {
  const exportRows = Array.isArray(rows) ? rows : []
  const exportColumns = (Array.isArray(columns) ? columns : []).map(/** 将当前条目转换为目标结果。 入参：column。 */ (column) =>
    typeof column === 'string' ? { label: column, prop: column } : column,
  )
  const header = exportColumns.map(/** 将当前条目转换为目标结果。 入参：column。 */ (column) => escapeCsvCell(column.label)).join(',')
  const body = exportRows
    .map(/** 将当前条目转换为目标结果。 入参：row。 */ (row) => exportColumns.map(/** 将当前条目转换为目标结果。 入参：column。 */ (column) => escapeCsvCell(getCellValue(row, column))).join(','))
    .join('\r\n')
  const csv = `\ufeff${header}${body ? `\r\n${body}` : ''}`
  const date = new Date().toISOString().slice(0, 10)
  const safeFileName = sanitizeFileName(filename || '数据导出')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })

  downloadBlob(blob, `${safeFileName}-${date}.csv`)
  return exportRows.length
}
