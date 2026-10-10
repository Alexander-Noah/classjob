import { existsSync, readFileSync } from 'node:fs'
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { sanitizeMaintenanceRecordExportCell } from '../src/views/maintenance-records/maintenanceRecordExport.js'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

const entryPath = new URL('../src/views/maintenance-records/MaintenanceRecordEntry.vue', import.meta.url)
const listPath = new URL('../src/views/maintenance-records/MaintenanceRecordList.vue', import.meta.url)

test('maintenance record entry and viewing pages are independent modules', () => {
  assert.equal(existsSync(entryPath), true)
  assert.equal(existsSync(listPath), true)

  const entry = readSource('../src/views/maintenance-records/MaintenanceRecordEntry.vue')
  const list = readSource('../src/views/maintenance-records/MaintenanceRecordList.vue')

  assert.doesNotMatch(entry, /MaintenanceRecordList/)
  assert.doesNotMatch(list, /MaintenanceRecordEntry/)
})

test('maintenance record entry contains every writable maintenance field', () => {
  const entry = readSource('../src/views/maintenance-records/MaintenanceRecordEntry.vue')
  const form = readSource('../src/views/maintenance-records/components/MaintenanceRecordForm.vue')

  assert.match(entry, /MaintenanceRecordForm/)
  for (const field of ['lab', 'device', 'maintenanceType', 'maintenanceDate', 'maintainer', 'content', 'result', 'remark']) {
    assert.match(form, new RegExp(field))
  }
  assert.match(entry, /@saved/)
  assert.match(form, /createMaintenanceRecord/)
  assert.match(form, /updateMaintenanceRecord/)
})

test('maintenance record viewing page provides filters detail and export only', () => {
  const list = readSource('../src/views/maintenance-records/MaintenanceRecordList.vue')

  for (const filter of ['keyword', 'lab', 'device', 'maintenanceType', 'maintainer', 'dateRange']) {
    assert.match(list, new RegExp(filter))
  }
  assert.match(list, /MaintenanceRecordFilters/)
  assert.match(list, /MaintenanceRecordDetail/)
  assert.match(list, /exportTableToExcel/)
  assert.match(list, /\/lab-admin\/maintenance-records\/new\?id=/)
})

test('maintenance record pages do not expose retired work-order operations', () => {
  const sources = [
    readSource('../src/views/maintenance-records/MaintenanceRecordEntry.vue'),
    readSource('../src/views/maintenance-records/MaintenanceRecordList.vue'),
  ].join('\n')

  assert.doesNotMatch(sources, /派单|待验收|完成报修|关闭工单|升级工单|维修工单/)
})

test('entry route loads ignore stale record responses after the requested ID changes or clears', () => {
  const entry = readSource('../src/views/maintenance-records/MaintenanceRecordEntry.vue')

  assert.match(entry, /let loadRequestToken = 0/)
  assert.match(entry, /const requestedId = recordId\.value/)
  assert.match(entry, /const requestToken = \+\+loadRequestToken/)
  assert.match(entry, /requestToken !== loadRequestToken \|\| requestedId !== recordId\.value/)
  assert.match(entry, /if \(!requestedId\) \{\s*record\.value = \{\}\s*loading\.value = false/)
})

test('maintenance export cells neutralize spreadsheet formulas with every dangerous prefix', () => {
  const values = ['=SUM(A1:A2)', '  +1+1', '-1+1', '\t@SUM(A1:A2)']

  for (const value of values) {
    assert.equal(sanitizeMaintenanceRecordExportCell(value), `'${value}`)
  }
  assert.equal(sanitizeMaintenanceRecordExportCell('普通维护内容'), '普通维护内容')

  const list = readSource('../src/views/maintenance-records/MaintenanceRecordList.vue')
  assert.match(list, /toMaintenanceRecordExportRow/)
})
