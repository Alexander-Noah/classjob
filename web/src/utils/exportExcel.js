function normalizeCellValue(value) {
  if (value === undefined || value === null) return ''
  if (Array.isArray(value)) return value.map(normalizeCellValue).filter(Boolean).join('；')
  if (value instanceof Date) return value.toLocaleString('zh-CN', { hour12: false })
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function sanitizeFileName(name = '数据导出') {
  return String(name || '数据导出')
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
}

function escapeCsvCell(value) {
  const text = normalizeCellValue(value)
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function getCellValue(row, column) {
  if (typeof column.formatter === 'function') {
    return column.formatter(row)
  }
  return row?.[column.prop]
}

function downloadBlob(blob, filename) {
  const link = document.createElement('a')
  const objectUrl = URL.createObjectURL(blob)

  link.href = objectUrl
  link.download = filename
  link.click()
  URL.revokeObjectURL(objectUrl)
}

export function exportTableToExcel({ filename, columns, rows }) {
  const exportRows = Array.isArray(rows) ? rows : []
  const exportColumns = (Array.isArray(columns) ? columns : []).map((column) =>
    typeof column === 'string' ? { label: column, prop: column } : column,
  )
  const header = exportColumns.map((column) => escapeCsvCell(column.label)).join(',')
  const body = exportRows
    .map((row) => exportColumns.map((column) => escapeCsvCell(getCellValue(row, column))).join(','))
    .join('\r\n')
  const csv = `\ufeff${header}${body ? `\r\n${body}` : ''}`
  const date = new Date().toISOString().slice(0, 10)
  const safeFileName = sanitizeFileName(filename || '数据导出')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })

  downloadBlob(blob, `${safeFileName}-${date}.csv`)
  return exportRows.length
}
