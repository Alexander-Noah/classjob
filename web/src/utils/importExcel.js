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

function parseCsvText(text) {
  const rows = []
  let row = []
  let value = ''
  let insideQuotes = false

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    const nextChar = text[index + 1]

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

export async function readSpreadsheetRows(file) {
  const fileName = getFileName(file)

  if (CSV_EXTENSION_PATTERN.test(fileName) || file?.type === 'text/csv') {
    return readCsvRows(file)
  }

  throw new Error('前端预览模式仅支持 .csv 文件；服务器模式请直接上传 .xlsx 或 .csv')
}
