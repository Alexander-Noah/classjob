/*
 * 维护记录的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { existsSync, readFileSync } from 'node:fs'
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { sanitizeMaintenanceRecordExportCell } from '../src/views/maintenance-records/maintenanceRecordExport.js'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

const entryPath = new URL('../src/views/maintenance-records/MaintenanceRecordEntry.vue', import.meta.url)
const listPath = new URL('../src/views/maintenance-records/MaintenanceRecordList.vue', import.meta.url)

test('legacy maintenance entry redirects to the list dialog', /** 验证当前测试用例的预期行为。 */ () => {
  assert.equal(existsSync(entryPath), true)
  assert.equal(existsSync(listPath), true)

  const entry = readSource('../src/views/maintenance-records/MaintenanceRecordEntry.vue')
  const list = readSource('../src/views/maintenance-records/MaintenanceRecordList.vue')

  assert.doesNotMatch(entry, /MaintenanceRecordList/)
  assert.doesNotMatch(list, /MaintenanceRecordEntry/)
})

test('maintenance record dialog contains every writable maintenance field', /** 验证当前测试用例的预期行为。 */ () => {
  const entry = readSource('../src/views/maintenance-records/MaintenanceRecordList.vue')
  const form = readSource('../src/views/maintenance-records/components/MaintenanceRecordForm.vue')

  assert.match(entry, /MaintenanceRecordForm/)
  for (const field of ['lab', 'device', 'maintenanceType', 'maintenanceDate', 'maintainer', 'content', 'result', 'remark']) {
    assert.match(form, new RegExp(field))
  }
  assert.match(entry, /@saved/)
  assert.match(form, /createMaintenanceRecord/)
  assert.match(form, /updateMaintenanceRecord/)
})

test('maintenance record management provides filters detail export and CRUD', /** 验证当前测试用例的预期行为。 */ () => {
  const list = readSource('../src/views/maintenance-records/MaintenanceRecordList.vue')

  for (const filter of ['keyword', 'lab', 'device', 'maintenanceType', 'maintainer', 'dateRange']) {
    assert.match(list, new RegExp(filter))
  }
  assert.match(list, /MaintenanceRecordFilters/)
  assert.match(list, /MaintenanceRecordDetail/)
  assert.match(list, /exportTableToExcel/)
  assert.match(list, /deleteMaintenanceRecord/)
  assert.match(list, /<el-dialog/)
  assert.match(list, /ElMessageBox.confirm/)
})

test('maintenance record pages do not expose retired work-order operations', /** 验证当前测试用例的预期行为。 */ () => {
  const sources = [
    readSource('../src/views/maintenance-records/MaintenanceRecordEntry.vue'),
    readSource('../src/views/maintenance-records/MaintenanceRecordList.vue'),
  ].join('\n')

  assert.doesNotMatch(sources, /派单|待验收|完成报修|关闭工单|升级工单|维修工单/)
})

test('legacy entry opens create or edit in the merged page', /** 验证当前测试用例的预期行为。 */ () => {
  const entry = readSource('../src/views/maintenance-records/MaintenanceRecordEntry.vue')
  assert.match(entry, /router.replace/)
  assert.match(entry, /route.query.id/)
  assert.match(entry, /create: '1'/)
  assert.doesNotMatch(entry, /<MaintenanceRecordForm/)
})

test('maintenance export cells neutralize spreadsheet formulas with every dangerous prefix', /** 验证当前测试用例的预期行为。 */ () => {
  const values = ['=SUM(A1:A2)', '  +1+1', '-1+1', '\t@SUM(A1:A2)']

  for (const value of values) {
    assert.equal(sanitizeMaintenanceRecordExportCell(value), `'${value}`)
  }
  assert.equal(sanitizeMaintenanceRecordExportCell('普通维护内容'), '普通维护内容')

  const list = readSource('../src/views/maintenance-records/MaintenanceRecordList.vue')
  assert.match(list, /toMaintenanceRecordExportRow/)
})
