/*
 * 表格导入公共逻辑模块，供相关视图或服务复用。
 * 主要导出：readSpreadsheetRows。
 */
const CSV_EXTENSION_PATTERN = /\.csv$/i

function getFileName(file) {
  return String(file?.name || '').trim()
}

function rowsToObjects(rows) {
  const [headerRow = [], ...bodyRows] = rows
  const headers = headerRow.map((header) => String(header || '').trim())

  return bodyRows
    .map((row) =>
      headers.reduce((result, header, index) => {
        if (header) {
          result[header] = row[index] ?? ''
        }
        return result
      }, {}),
    )
    .filter((row) => Object.values(row).some((value) => String(value ?? '').trim()))
}

/** 按引用状态扫描 CSV，支持双引号转义和引号内换行；不做列数或未闭合引号校验。 */
function parseCsvText(text) {
  const rows = []
  let row = []
  let value = ''
  let insideQuotes = false

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    const nextChar = text[index + 1]

    // 两个连续双引号表示字段内容中的一个双引号，不能切换引用状态。
    if (char === '"' && insideQuotes && nextChar === '"') {
      value += '"'
      index += 1
      continue
    }

    if (char === '"') {
      insideQuotes = !insideQuotes
      continue
    }

    if (char === ',' && !insideQuotes) {
      row.push(value)
      value = ''
      continue
    }

    if ((char === '\n' || char === '\r') && !insideQuotes) {
      if (char === '\r' && nextChar === '\n') {
        index += 1
      }
      row.push(value)
      rows.push(row)
      row = []
      value = ''
      continue
    }

    value += char
  }

  row.push(value)
  rows.push(row)
  return rows
}

async function readCsvRows(file) {
  const text = await file.text()
  return rowsToObjects(parseCsvText(text.replace(/^\ufeff/, '')))
}

/**
 * 浏览器预览入口只解析 CSV；名称沿用表格导入用途，不代表能在此读取 XLSX。
 * @param {File} file 用户选择的文件，根据扩展名或 MIME 识别 CSV
 * @returns {Promise<Array<object>>} 以首行为列名的非空数据行；忽略空列名并移除 UTF-8 BOM
 * @throws {Error} 不是 CSV 或文件读取失败；服务端导入使用独立上传入口
 */
export async function readSpreadsheetRows(file) {
  const fileName = getFileName(file)

  if (CSV_EXTENSION_PATTERN.test(fileName) || file?.type === 'text/csv') {
    return readCsvRows(file)
  }

  throw new Error('前端预览模式仅支持 .csv 文件；服务器模式请直接上传 .xlsx 或 .csv')
}
