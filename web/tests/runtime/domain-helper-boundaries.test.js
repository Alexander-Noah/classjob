import { expect, it } from 'vitest'
import * as maintenance from '../../src/modules/maintenance-records/maintenanceRecordEditor'
import * as labs from '../../src/utils/labDisplay'
import * as providers from '../../src/modules/ai-assistant/providerPresets'
import { formatMessageContent } from '../../src/modules/ai-assistant/formatMessage'

it('maintenance permissions parse legacy groups and reject malformed or absent roles', () => {
  expect(maintenance.canWriteMaintenanceRecords({ roleCode: 'systemAdmin' })).toBe(true)
  expect(maintenance.canWriteMaintenanceRecords()).toBe(false)
  for (const permissions of [undefined, '[]', [], ['故障报修'], [{ items: ['维护记录管理'] }]]) expect(maintenance.canWriteMaintenanceRecords({ roleCode: 'labAdmin', permissions })).toBe(true)
  for (const permissions of ['{}', '{}bad', [null], ['其他权限'], [{ items: ['其他权限'] }]]) expect(maintenance.canWriteMaintenanceRecords({ roleCode: 'labAdmin', permissions })).toBe(false)
})

it('maintenance drafts preserve real IDs and choose the available author field', () => {
  for (const [user, expected] of [[{ realName: '真名' }, '真名'], [{ name: '姓名' }, '姓名'], [{ username: '账号' }, '账号'], [{}, '']]) expect(maintenance.createMaintenanceDraft({}, user).maintainer).toBe(expected)
  expect(maintenance.createMaintenanceDraft({ id: 1, labId: 2, deviceId: 3, maintenanceDate: '2026-01-02 10:00', maintainer: '填写人' })).toMatchObject({ id: 1, labId: '2', deviceId: '3', maintenanceDate: '2026-01-02', maintainer: '填写人' })
  const result = maintenance.buildMaintenancePayload({ labId: '2' }, [{ id: 2, labName: '旧实验室' }], [])
  expect(result).toMatchObject({ labId: 2, deviceId: null, lab: '旧实验室', device: '', content: '' })
  const rows = [{ maintenanceDate: '2026-01-02', content: '清洁', lab: 'A' }, { content: '重启', lab: 'B' }]
  expect(maintenance.filterMaintenanceRecords(rows, {})).toHaveLength(2)
  expect(maintenance.filterMaintenanceRecords(rows, { dateRange: ['2026-01-02'] })).toEqual([rows[0]])
  expect(maintenance.filterMaintenanceRecords(rows, { dateRange: [null, '2026-01-01'] })).toEqual([rows[1]])
  expect(maintenance.filterMaintenanceRecords(rows, { keyword: '不存在' })).toEqual([])
})

it('lab labels retain aliases, room prefixes, capacities and all supported identity fallbacks', () => {
  expect(labs.getLabRoomCode('A101 教室')).toBe('A101')
  expect(labs.getLabRoomCode('普通教室')).toBe('')
  expect(labs.formatLabOptionLabel('A101')).toBe('A101')
  expect(labs.formatLabOptionLabel({ name: '机房', capacity: 30 })).toBe('机房 · 30人')
  for (const input of [{ value: '1' }, { id: 2 }, { labId: 3 }, { name: 'name' }, { labName: 'labName' }, {}]) {
    const option = labs.createLabSelectOption(input)
    expect(option.value).toBe(input.value ?? input.id ?? input.labId ?? input.name ?? input.labName ?? '')
  }
  expect(labs.createLabSelectOption('A101').value).toBe('A101')
  expect(labs.createLabSelectOption({ value: 7 })).toMatchObject({ value: 7, label: '7', displayName: '7' })
})

it('provider endpoints reject malformed, credentialed and decorated URLs', () => {
  for (const value of [undefined, '', 'bad', 'ftp://api.test', 'https://user@api.test', 'https://user:pass@api.test', 'https://api.test?q=1', 'https://api.test/#hash']) expect(providers.normalizedApiEndpoint(value)).toBe('')
  expect(providers.inferAiProvider('invalid')).toBe('custom')
  expect(providers.normalizedApiEndpoint(' https://api.test/v1/chat/completions/ ')).toBe('https://api.test/v1')
  expect(providers.selectAiProvider({ baseUrl: 'https://api.deepseek.com', apiKey: 'keep' }, 'deepseek').apiKey).toBe('keep')
  expect(providers.aiConfigFingerprint()).toBe('[false,"","",""]')
  expect(providers.aiConfigFingerprint({ enabled: true, baseUrl: 'https://api.test/v1/', model: ' model ', systemPrompt: ' prompt ' })).toBe('[true,"https://api.test/v1","model","prompt"]')
})

it('safe compatibility serializer preserves ordered list starts and explicit line breaks', () => {
  const output = formatMessageContent('3. 第三项\n4. 第四项\n\n普通行\n下一行')
  expect(output).toContain('<ol start="3">')
  expect(output).toContain('<br>')
  expect(output).toContain('下一行')
})
import { normalizeMaintenanceRecord } from '../../src/modules/maintenance-records/maintenanceRecordModel'
import { getAcademicWeek, getTeachingWeekStartDate } from '../../src/modules/teaching-calendar/teachingCalendar'
import { credentialPresentation } from '../../src/modules/timetable/crawlerPresentation'
it('legacy maintenance identifiers preserve zero and fall back through snake-case identifiers',()=>{
 expect(normalizeMaintenanceRecord({id:0,recordId:9,labId:0,lab_id:8,deviceId:0,device_id:7})).toMatchObject({id:0,labId:0,deviceId:0})
 expect(normalizeMaintenanceRecord({recordId:9,lab_id:8,device_id:7})).toMatchObject({id:9,labId:8,deviceId:7})
 expect(normalizeMaintenanceRecord(null)).toMatchObject({id:'',labId:'',deviceId:'',recordNo:'未填写'})
})
it('calendar week boundaries clamp to semester limits and reject absent calendars',()=>{
 const calendar={semesterStartDate:'2026-09-01',maximumWeek:2}
 expect(getTeachingWeekStartDate(calendar,99).getDate()).toBe(8)
 expect(getTeachingWeekStartDate(calendar,-1).getDate()).toBe(1)
 expect(getTeachingWeekStartDate(calendar,'bad').getDate()).toBe(1)
 expect(getTeachingWeekStartDate({semesterStartDate:'2026-09-01'},2).getDate()).toBe(8)
 expect(getTeachingWeekStartDate(null,1)).toBeNull()
 expect(getAcademicWeek(undefined,new Date('2026-09-01'))).toBe(1)
 expect(credentialPresentation({configured:true})).toMatchObject({description:'已加密保存',canCollect:true})
})

