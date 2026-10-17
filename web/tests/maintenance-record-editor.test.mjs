/*
 * 维护记录的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { buildMaintenancePayload, canWriteMaintenanceRecords, createMaintenanceDraft, devicesForMaintenanceLab, filterMaintenanceRecords } from '../src/modules/maintenance-records/maintenanceRecordEditor.js'

const labs = [{id:1,name:'实验室甲'},{id:2,name:'实验室乙'}]
const devices = [{id:11,labId:1,deviceName:'设备甲'},{id:22,labId:2,deviceName:'设备乙'}]

test('maintenance records allow an empty or cleared device even when the laboratory has no devices', /** 验证当前测试用例的预期行为。 */ () => {
  for (const deviceId of ['', null, undefined]) {
    const payload = buildMaintenancePayload({labId:'1', deviceId}, labs, [])
    assert.equal(payload.deviceId, null)
    assert.equal(payload.device, '')
  }
})

test('maintenance device choices and payload cannot cross laboratory boundaries', /** 验证当前测试用例的预期行为。 */ () => {
  assert.deepEqual(devicesForMaintenanceLab(devices, '1').map(/** 将当前条目转换为目标结果。 入参：item。 */ item=>item.id), [11])
  assert.throws(/** 处理 throws 调用中的回调逻辑。 */ ()=>buildMaintenancePayload({labId:'1',deviceId:'22'},labs,devices), /属于该实验室/)
  const payload=buildMaintenancePayload({labId:'1',deviceId:'11',maintenanceType:' 巡检 ',remark:' 备注 '},labs,devices)
  assert.equal(payload.labId,1)
  assert.equal(payload.deviceId,11)
  assert.equal(payload.lab,'实验室甲')
  assert.equal(payload.device,'设备甲')
  assert.equal(payload.maintenanceType,'巡检')
  assert.equal(payload.remark,'备注')
})

test('new drafts do not reuse a previous record and editing preserves stable identifiers', /** 验证当前测试用例的预期行为。 */ () => {
  const edit=createMaintenanceDraft({id:7,labId:1,deviceId:11,recordNo:'MR-7',remark:'未填写',content:'原内容',maintenanceDate:'2026-10-15 12:00:00'})
  assert.equal(edit.id,7)
  assert.equal(edit.maintenanceDate,'2026-10-15')
  assert.equal(edit.remark,'')
  const empty=createMaintenanceDraft({}, {username:'labadmin'})
  assert.equal(empty.id,'')
  assert.equal(empty.content,'')
  assert.equal(empty.maintainer,'labadmin')
})

test('read-only and malformed permissions cannot expose mutation actions', /** 验证当前测试用例的预期行为。 */ () => {
  assert.equal(canWriteMaintenanceRecords({roleCode:'labAdmin',permissions:'[{"items":["维护记录查看"]}]'}),false)
  assert.equal(canWriteMaintenanceRecords({roleCode:'labAdmin',permissions:'[{"items":["维护记录填写"]}]'}),true)
  assert.equal(canWriteMaintenanceRecords({roleCode:'labAdmin',permissions:'broken'}),false)
  assert.equal(canWriteMaintenanceRecords({roleCode:'teacher',permissions:'["维护记录填写"]'}),false)
})

test('maintenance filters combine inclusive date range, references and free text', /** 验证当前测试用例的预期行为。 */ () => {
  const rows=[{lab:'甲',device:'设备',maintenanceDate:'2026-10-15 10:00:00',content:'例行巡检'},{lab:'乙',maintenanceDate:'2026-10-16',content:'例行巡检'}]
  assert.equal(filterMaintenanceRecords(rows,{keyword:'巡检',lab:'甲',dateRange:['2026-10-15','2026-10-15']}).length,1)
  assert.equal(filterMaintenanceRecords(rows,{dateRange:['2026-10-17','2026-10-18']}).length,0)
  assert.equal(rows.length,2)
})
